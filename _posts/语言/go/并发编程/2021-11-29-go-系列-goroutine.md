---
layout: post
title: "Go 系列 协程"
author: "lichao"
header-img: "img/post/bg/post-bg-ngon-ngu-golang.jpg"
catalog: true
tags:
  - go
---

goroutine 特点：
1. go 语言层面实现了协程，语言层面上 hook 了阻塞的操作，阻塞操作中实现了协程切换；
2. runtime 实现了 work-stealing 算法，实现协程在不同的线程上切换，M 个协程可以运行在 N 个线程上；
3. 实现了协程的抢占式调度（go1.14之后）；

goroutine 优点：
1. goroutine 可以在用户空间调度，避免了内核态和用户态的切换导致的成本。
2. goroutine 语言原生支持，提供了非常简洁的语法，屏蔽了大部分复杂的底层实现。
3. goroutine 更小的栈空间允许用户创建成千上万的实例。

## GMP 调度模型
![GMP模型](/img/post/lang/go/GMP模型.png)

**G(goroutine) **       
G 是 Go 运行时对 goroutine 的抽象描述，G 中存放并发执行的代码入口地址、上下文、运行环境（关联的 P 和 M）、运行栈等执行相关的元信息。

G 的新建、休眠、恢复、停止都受到 Go 运行时的管理。Go 运行时的监控线程会监控 G 的调度，G 不会长久地阻塞系统线程，运行时的调度器会自动切换到其他 G 上继续运行。G 新建或恢复时会添加到运行队列，等到 M 取出并运行。

> G 并不是执行体，而是用于存放并发执行体的元信息，包括并发执行的代码入口地址、上下文、堆栈等信息。为了减少对象的分配与回收，G 对象是可以复用的，只需将相关元信息初始化为新值即可。

**M(Machine)**      
M 代表 OS 内核线程，是操作系统层面调度和执行的实体。M 仅负责执行，M 不停地被唤醒或创建，然后执行。M 启动时进入的是运行时的管理代码，由这段代码 获取 G 和 P 资源，然后执行调度。另外，Go 语言运行时会单独创建一个监控线程，负责对程序的内存、调度等信息进行监控与控制。

> 每个 M 都有自己的管理堆栈 g0, g0不指向任何可执行的函数，g0 仅在 M 执行管理和调度逻辑时使用。在调度或系统调用时会切换到g0的栈空间，

**P(Processor)**      
P 代表 M 运行 G 所需要的资源，是对资源的一种抽象与管理，P 不是一段代码实体，而是一个管理的数据结构，P 主要是降低 M 管理调度 G 的复杂度，增加一个间接的控制层数据结构。
把 P 看作资源，而不是处理器，P 控制 Go 代码的并行度，它不是运行实体。P 持有 G 的队列，P 可以隔离调度，解除 P 和 M 的绑定就解除了 M 对一串 G 的调用。

M 和 P 一起构建一个运行时环境，每个 P 有一个本地的可调度 G 队列，队列里面的G会被M依次调度执行，如果本地队列空了，则会去全局队列偷取一部分 G，如果全部队列也是空的，则去其他的 P 中投去一部分 G，只就是 Work Stealing 算法的基本原理。

> P 的数量默认是 CPU 核心的数量，可以通过 ```sync.GOMAXPROCS``` 函数设置或查询，M 和 P 的数量差不多，但运行时会根据当前的状态动态的创建 M，M 有个最大值上限，当前是 10000。 G 与 P 是一种 N:M 的关系，M 可以成千上万，远远大于 N。


> 还有特殊的 G 和 M，它们是 m0 和 g0。m0 是启动程序的主线程，这个 m 对应的信息会存放在全局变量 m0 中，m0 负责初始化操作和启动第一个 g，之后 m0 和其他的 M 都一样了。全局变量的 g0 时 m0 的 g0。

- 一个正在执行的协程 G 一定会绑定一个 P；
![Processor和线程](/img/post/lang/go/Processor和线程1.png)
- Processor 绑定一个正在运行的线程 M，当一个 P 上的线程被系统调用阻塞后，runtime 会新生成一个线程重新绑定 P。
![Processor和线程](/img/post/lang/go/Processor和线程2.png)
- P 实现了调度的局部性，一个 Processor 可以持有 257 个等待运行的协程(一个容量为 256 的队列和一个 runnext 变量，runnext 会首先被执行)；P 执行完协程后，会继续运行自己持有的协程；
- 当协程执行结束后，它绑定的 P 会执行 findrunner，优先从自己持有的协程中挑选一个新的 G 继续执行；

#### 启动分析
Go 启动初始化过程：
- 分配和检查栈空间
- 初始化参数和环境变量
- 当前运行线程标记为 m0， m0 是程序启动的主线程。
- 调用运行时初始化函数 runtime.schedinit 进行初始化。
- 在 m0上调度第一个 G，这个 G运行 runtime.main 函数。
  -  runtime.main 会拉起运行时的监控线程，然后调用 main 包的init 初始化方法，最后执行 mian 函数。


**什么时候创建M、P、G**    
在程序启动过程中会初始化空闲 P 列表，P 是在这个时候被创建的，同时第一个 G 也是在初始化过程中被创建的。后续在有 go 并发调用的地方都有可能创建 G，由于 G 只是一个数据结构，并不是执行实体，所以 G 是可以被复用的。在需要 G 结构时，首先要去 P 的空闲 G 列表里面寻找已经运行结束的 goroutine，其 G 会被缓存起来。每个并发调用都会初始化一个新的 G 任务，然后唤醒 M 执行任务。这个唤醒不是特定唤醒某个线程去工作，而是先尝试获取当前线程 M，如果无法获取，则从全局调度的空闲M列表中获取可用的M，如果没有可用的，则新建M，然后绑定P和G进行运行。所以M和P不是一一对应的，M是按需分配的，但是运行时会设置一个上限值(默认是10000)，超出最大值将导致程序崩溃。

M 线程里有管理调度和切换堆栈的逻辑，但是 M 必须拿到 P 后才能运行，可以看到 M 是自驱动的，但是需要 P 的配合。这是一个巧妙的设计。

**抢占调度**        
**抢占调度的原因**       
1. 不让某个 G 长久地被系统调用阻塞，阻碍其他 G 运行。
2. 不让某个 G 一直占用某个 M 不释放。
3. 避免全局队列里面的 G 得不到执行。

**抢占调度的策略**          
1. 在进入系统调用(syscall)前后，各封装一层代码检测 G 的状态，当检测到当前 G 已经被监控线程抢占调度，则 M 停止执行当前 G，进行调度切换。
2. 监控线程经过一段时间检测感知到 P 运行超过一定时间，取消 P 和 M 的关联，这也是一种更高层次的调度。
3. 监控线程经过一段时间检测感知到 G 一直运行，超过了一定的时间，设置 G 标记 G 执行栈扩展逻辑检测到抢占标记，根据相关条件决定是否抢占调度。

> Go程序运行时是比较复杂的，涉及内存分配、垃圾回收、goroutine调度和通信管理等诸多方面。

#### 示例分析
```go
func GOMAXPROCS(n int) int
```
当 P 的数目缩减的时候，runtime 会选择需要销毁的P，将 P 中的协程放入 GRQ 的队列首部。

**举例-Case 1**
```go
import (
   "fmt"
   "runtime"
   "sync"
)

func main() {
   runtime.GOMAXPROCS(1)
   var x = 1
   var wg sync.WaitGroup ;
   for {
      wg.Add(1)
      go func(i int) {
         fmt.Println(i)
         wg.Add(-1)
      }(x)
      x++
      if (x > 300) {
         break
      }
   }
   wg.Wait()
}
```

这个程序可以让我们更好的理解 GMP 模型，程序一共 1 个 Processor。程序共产生了 ```1-300``` 号的协程。当产生完成 257 个线程后，Local Queue 和 Global Queue 的状态为：
- runnext_ : 257 号协程
- Local Queue: 1-256 号协程
- Global Queue: 为空

当产生第 258 号协程的时候，Local Queue 的容量已经满了，runtime 会从 Local Queue 获取 128 协程放入 Global Queue，并将 257 号协程插入Global Queue。
- runnext_ : 258 号协程
- Local Queue：129-256号协程
- Global Queue：1-128 257
插入 300 的时候：
- runnext_ : 300号协程
- Local Queue：129-256  258-299号协程
- Global Queue：1-128 257
调用 ```wg.Wait()```，开始执行 Queue 中的协程。
1. 首先执行 300 号协程，
2. Local Queue 129-188号协程
3. Global Queue 1号协程 （调度了61次）
4. Local Queue 189-248
5. Global Queue 2号协程 调度（调度了61次）
6. 249-256 258-299
7. 从Global Queue获取128个，Local Queue 3-128 257
8. 3-128 257


#### 协程切换
Go1.14前，如果一个 G 会一直在执行计算任务没有遇到阻塞，那么这个 P 上的其余协程都不会被执行。GO 不适合执行复杂的计算逻辑。
Go1.14中，每 10ms，runtime会发送os信号给线程，信号处理函数中执行切换。

**协程切换时机:**      
协程会在四种情况下切换，执行 ```findrunnable``` 选择下一个协程：
![协程切换](/img/post/lang/go/协程切换2.png)

当遇到协程切换的场景的时候，runtime 会调用```mcall```函数完成协程栈的切换。

###### chan切换
chan 的实际定义如下：
```go
type hchan struct {
   qcount   uint           // 队列中的元素数目
   dataqsiz uint           // 队列中最大的元素树
   buf      unsafe.Pointer // points to an array of dataqsiz elements
   elemsize uint16
   closed   uint32
   elemtype *_type // 单个元素的大小
   sendx    uint   // 下一个发送元素写入位置
   recvx    uint   // 下一个元素读出位置
   recvq    waitq  // 表示阻塞在此chan上的读队列
   sendq    waitq  // 表示阻塞在此chan上的写队列
   lock mutex
}
```
chan 含有两个阻塞协程队列，recvq 和 sendq，当读一个 chan 遇到 chan 为空时，此时协程需要被阻塞；runtime 会将当前协程变为 _Gowaiting 状态，放入 recvq 中；当向 chan 写数据的时候，首先会检查是 recvq 否有阻塞的协程，如果 recvq 不为空，则取第一个将数据返回给这个协程，同时调用 ```runqput```，将协程加入 GMP 模型中。
![chan切换](/img/post/lang/go/chan切换.png)
对于 sendq 的操作类似。

###### 对 chan 调用 select
对于以下语法，go 会进行特化处理：
```go
select{
    case x<-chan1:
            XXXX
    case y<-chan2:
            XXXX
    ·····
}
```
Go 不会将对 chan 的 select 处理成其他语言中 switch 形式判断语句，相反 go 的编译器会处理为 ```selectgo``` 函数。
```go
func selectgo(cas0 *scase, order0 *uint16, ncases int) (int, bool)
```
在 ```selectgo``` 函数中，会对目标的 chan 按照随机顺序进行轮询，当发现一个 chan 满足条件后返回，否则让出协程等待下次调度再次轮询。

> 对于 chan 的 select 不是基于事件触发而是基于轮询;

###### 网络IO
Go 的 runtime 会为每个套接字创建一个 ```pollDesc```，
```go
type pollDesc struct {
   link *pollDesc // in pollcache, protected by pollcache.lock

   // The lock protects pollOpen, pollSetDeadline, pollUnblock and deadlineimpl operations.
   // This fully covers seq, rt and wt variables. fd is constant throughout the PollDesc lifetime.
   // pollReset, pollWait, pollWaitCanceled and runtime·netpollready (IO readiness notification)
   // proceed w/o taking the lock. So closing, everr, rg, rd, wg and wd are manipulated
   // in a lock-free way by all operations.
   // NOTE(dvyukov): the following code uses uintptr to store *g (rg/wg),
   // that will blow up when GC starts moving objects.
   lock    mutex // protects the following fields
   fd      uintptr
   closing bool
   everr   bool      // marks event scanning error happened
   user    uint32    // user settable cookie
   rseq    uintptr   // protects from stale read timers
   rg      uintptr   // pdReady, pdWait, G waiting for read or nil
   rt      timer     // read deadline timer (set if rt.f != nil)
   rd      int64     // read deadline
   wseq    uintptr   // protects from stale write timers
   wg      uintptr   // pdReady, pdWait, G waiting for write or nil
   wt      timer     // write deadline timer
   wd      int64     // write deadline
   self    *pollDesc // storage for indirect interface. See (*pollDesc).makeArg.
}
```

当一个协程发生网络 IO 的时候，调用非阻塞 IO，如果网络 IO 未准备好(即内核缓冲区没有足够的数据提供给进程读写)，runtime 会将该协程状态变为 ```_Gowaiting```，移除 GMP 模型，同时在 ```epoll/kqueue/iocp``` 注册该协程监听的套接字。

当一个 P 上的没有可执行的协程，并且 GRQ 为空的时候，runtime 会调用 netpoll(底层封装了 ```epoll/kqueue/iocp```)获得准备好的套接字以及对应阻塞的协程状态转化为 ```_Grunning```，重新加入 GRQ 中，runtime 会为 P 重新获取协程。

###### Time 处理
第一次调用 time 相关的函数，time 协程持有一个四叉堆(优先队列，四叉堆可以充分发挥 1 级 cache 的性能)，用于处于等待状态的 Goroutine 排序，当调 ```time.Sleep```等函数，runtime 会将当前的协程放入对应的 time 协程的四叉堆中；runtime 切换到 time 协程时，time 协程会检查优先队列的头部判断是否存在 time 到期的协程，如果存在会将协程放回 GRQ 或者 LRQ 中，遍历完毕后会再次让出 P。



#### goroutine状态      
一个 goroutine 在生命周期中有以下几种状态: 
![goroutine生命周期](/img/post/lang/go/goroutine生命周期.png)

**_Grunning、_Grunnable:**     
- _Grunning：程序正在被运行。
- _Grunnable：程序可以被执行。

Go 采用了一种被称为 GMP 的模型调度、运行协程。
- 当协程状态由其他状态变为 ```_Grunning``` 的时候，runtime 将协程交给 GMP 来管理；
- 当 GMP 执行某个协程，协程状态变为 ```_Grunning```；
- 如果执行中的协程的时间片到了或者调用了 ```runtime.Goshed```，协程会变为 ```_Grunnable``` 等待重新调度；

**_Gsyscall:**     
部分系统调用会导致线程阻塞。进入阻塞的系统调用的时候，go 的 runtime 会将协程状态设置为 ```_Gsyscall```，移除 GMP 调度模型。线程从阻塞的系统调用返回的时候，runtime 重新将协程加入 GMP 调度模型中。为了保证程序的并发数目，当一个线程进入阻塞的时候，runtime 会新建一个线程绑定 P。

**_Gowaiting:**     
当正在执行的协程由于网络 IO、chan、time、sync 阻塞的时候，runtime 将协程移除 GMP 模型的同时会将协程的状态设置为 ```_Gowaiting```。当条件满足后，runtime 会将协程状态改为```_Grunable```，继续由 GMP 管理。

**_GocopyStack:**     
Golang 的协程的调用栈初始化为 2KB，与 C++ 不同，golang 在编译的时候会进行逃逸分析，当一个变量可能被取指针的时候，那么这个变量会在堆上分配，这样做的原因是 golang 中的堆栈会发生拷贝，这就会导致 golang 栈上地址运行时发生变化。

当运行时 runtime 发现协程的栈不够时，就会重新分配栈，此时协程的状态为 ```_GocopyStack```，分配完后，重新放入Global Queue。