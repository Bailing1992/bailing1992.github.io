---
layout: post
title: "Go 系列 Context 标准库"
subtitle: "Context 顾名思义是协程的上下文，主要用于跟踪协程的状态，可以做一些简单的协程控制，也能记录一些协程信息"
author: "lichao"
header-img: "img/post/bg/post-bg-ngon-ngu-golang.jpg"
catalog: true
tags:
  - go
---


> Context 设计目的是跟踪 goroutine 调用树，并在这些 goroutine 调用树中传递通知与元数据。
> Context 提供的核心功能是多个 goroutine 之间的退出通知机制，传递数据只是一个辅助功能，应谨慎使用 context 传递数据。

**Context们是一棵树**
![wait group](/img/post/lang/go/ctx树.jpg)

context 整体是一个树形结构，不同的 ctx 间可能是兄弟节点或者是父子节点的关系。

同时由于 Context 接口有多种不同的实现，所以树的节点可能也是多种不同的 ctx 实现。总的来说我觉得 Context 的特点是：

- 树形结构，每次调用WithCancel, WithValue, WithTimeout, WithDeadline实际是为当前节点在追加子节点。

继承性，某个节点被取消，其对应的子树也会全部被取消。

多样性，节点存在不同的实现，故每个节点会附带不同的功能。

## 基础用法

接下来介绍 Context 的基础用法，最为重要的就是 3 个基础能力，**取消、超时、附加值**。

**（一）新建一个Context：**

```go
ctx := context.TODO()
ctx := context.Background()
```

这两个方法返回的内容是一样的，都是返回一个空的 context，这个 context 一般用来做父 context。

**（二）WithCancel：**

```go
// 函数声明
func WithCancel(parent Context) (ctx Context, cancel CancelFunc)
// 用法:返回一个子Context和主动取消函数
ctx, cancel := context.WithCancel(parentCtx)
```

这个函数相当重要，会根据传入的context生成一个子context和一个取消函数。当父context有相关取消操作，或者直接调用cancel函数的话，子context就会被取消。

举个日常业务中常用的例子：

```go
// 一般操作比较耗时或者涉及远程调用等，都会在输入参数里带上一个ctx，这也是公司代码规范里提倡的
func Do(ctx context.Context, ...) {
  ctx, cancel := context.WithCancel(parentCtx)
  
  // 实现某些业务逻辑
  
  // 当遇到某种条件，比如程序出错，就取消掉子Context，这样子Context绑定的协程也可以跟着退出
  if err != nil {
    cancel()
  }
}
```

**（三）WithTimeout：**

```go
// 函数声明
func WithTimeout(parent Context, timeout time.Duration) (Context, CancelFunc)
// 用法：返回一个子Context（会在一段时间后自动取消），主动取消函数
ctx := context.WithTimeout(parentCtx, 5*time.Second)
```

这个函数在日常工作中使用得非常多，简单来说就是给 Context 附加一个超时控制，当超时 ```ctx.Done()``` 返回的 channel 就能读取到值，协程可以通过这个方式来判断执行时间是否满足要求。

举个日常业务中常用的例子：

```go
// 一般操作比较耗时或者涉及远程调用等，都会在输入参数里带上一个ctx，这也是公司代码规范里提倡的
func Do(ctx context.Context, ...) {
  ctx := context.WithTimeout(parentCtx, 5*time.Second)
  
  // 实现某些业务逻辑

  for {
    select {
     // 轮询检测是否已经超时
      case <-ctx.Done():
        return
      // 有时也会附加一些错误判断
      case <-errCh:
        cancel()
      default:
    }
  }

}
```

现在大部分 go 库都实现了超时判断逻辑，只需要传入 ctx 就好。

**（四）WithDeadline：**

```go
// 函数声明
func WithDeadline(parent Context, d time.Time) (Context, CancelFunc)
// 用法：返回一个子Context（会在指定的时间自动取消），主动取消函数
ctx, cancel := context.WithDeadline(parentCtx, time.Now().Add(5*time.Second))
```

这个函数感觉用得比较少，和WithTimeout相比的话就是使用的是截止时间。

**（五）WithValue：**

```go
// 函数声明
func WithValue(parent Context, key, val interface{}) Context
// 用法: 传入父Context和(key, value)，相当于存一个kv
ctx := context.WithValue(parentCtx, "name", 123)
// 用法：将key对应的值取出
v := ctx.Value("name")
```

这个函数常用来保存一些链路追踪信息，比如 API 服务里会有来保存一些来源 ip、请求参数等。

因为这个方法实在是太常用了，比如```grpc-go```里的 metadata 就使用这个方法将结构体存储在 ctx 里。

```go
func NewOutgoingContext(ctx context.Context, md MD) context.Context {
    return context.WithValue(ctx, mdOutgoingKey{}, rawMD{md: md})
}
```

## 源码实现

context.Context是一个接口，源码里是有多种不同的实现的，借此实现不同的功能。

```go
type Context interface {
    Deadline() (deadline time.Time, ok bool)  // 完成工作的截止日期
    Done() <-chan struct{}                    // 当前工作完成或者上下文被取消后关闭
    Err() error                               // 返回 context 取消原因
    Value(key interface{}) interface{}        // 获取之前设置的 key 对应的 value
}
```

Done() 返回一个空只读 channel，可以表示 context 被取消的信号：当这个 channel 被关闭时，说明 context 被取消了。这是一个只读的channel， 读一个关闭的 channel 会读出相应类型的零值。并且源码里没有地方会向这个 channel 里面塞入值（只会close）。因此在子协程里读这个 channel，除非被关闭，否则读不出来任何东西。也正是利用了这一点，子协程从 channel 里读出了值后，就可以做一些收尾工作，尽快退出。同样在父协程中也可以读这个channel，监听子协程的cancel。

**Canceler 接口：**

```go
type canceler interface {  
    cancel(removeFromParent bool, err error)  
    Done() <-chan struct{}  
}
```

实现了上面定义的两个方法的 Context，就表明该 Context 是可取消的。源码中有两个类型实现了 canceler 接口：*cancelCtx 和*timerCtx。

**emptyCtx：**

```go
type emptyCtx int

func (*emptyCtx) Deadline() (deadline time.Time, ok bool) {
    return
}

func (*emptyCtx) Done() <-chan struct{} {
    return nil
}

func (*emptyCtx) Err() error {
    return nil
}

func (*emptyCtx) Value(key interface{}) interface{} {
    return nil
}
```

background 和 todo 是一个空的 context，永远不会被 cancel，没有存储值，也没有 deadline。
background 通常用在 main 函数中，作为所有 context 的根节点。
todo 通常用在并不知道传递什么 context 的情形。

**cancelContext：**

```go
type cancelCtx struct {  
        Context  

        // 保护之后的字段  
        mu       sync.Mutex  
        done     chan struct{}  
        children map[canceler]struct{}  
        err      error  
}
```

这是一个可以取消的 Context，实现了 canceler 接口。它直接将接口 Context 作为它的一个匿名字段，这样，它就可以被看成一个 Context。

```go
func (c *cancelCtx) Done() <-chan struct{} {  
         c.mu.Lock()  
         if c.done == nil {  
             c.done = make(chan struct{})  
         }  
         d := c.done  
         c.mu.Unlock()  
         return d  
}
```

c.done 是“懒汉式”创建，只有调用了 Done() 方法的时候才会被创建。直接调用读这个 channel，协程会被 block 住。一般通过搭配 select 来使用。一旦关闭，就会立即读出零值。

```go
func (c *cancelCtx) cancel(removeFromParent bool, err error) {  
   c.mu.Lock()  
   if c.err != nil {  
      c.mu.Unlock()  
      return // already canceled  
 }  
   c.err = err  
   if c.done == nil {  
      c.done = closedchan  
   } else {  
      close(c.done)  
   }  
   for child := range c.children {  
      // NOTE: acquiring the child's lock while holding parent's lock.  
      child.cancel(false, err)  
   }  
   c.children = nil  
   c.mu.Unlock()  

   if removeFromParent {  
      removeChild(c.Context, c)  
   }  
}
```

总体来看，cancel() 方法的功能就是关闭 channel；递归地取消它的所有子节点；从父节点从删除自己。达到的效果是通过关闭 channel，将取消信号传递给了它的所有子节点。goroutine 接收到取消信号的方式就是 select 语句中的读 c.done 被选中。

**timerCtx：**

```go
type timerCtx struct {  
    cancelCtx  
    timer *time.Timer // Under cancelCtx.mu.  
    
    deadline time.Time  
}

timerCtx 基于 cancelCtx，只是多了一个 time.Timer 和一个 deadline。Timer 会在 deadline 到来时，自动取消 context。
valueCtx
type valueCtx struct {  
    Context  
    key, val interface{}  
}

func WithValue(parent Context, key, val interface{}) Context {  
   return &valueCtx{parent, key, val}  
}
```

WithValue 能从父上下文中创建一个子上下文，传值的子上下文使用 context.valueCtx 类型

```go
func (c *valueCtx) Value(key interface{}) interface{} {  
   if c.key == key {  
      return c.val  
   }  
   return c.Context.Value(key)  
}
```

## 总结

**Context 的作用：**

在 goroutine 之间传递上下文信息，包括：取消信号、超时时间、截止时间、k-v 等。

**Context 的原理：**

1. 取消信号：通过关闭 done channel 通知监听者
2. 超时时间：通过Timer自动触发 cancel 关闭 channel
3. 存值：通过链表生成一个新的节点存储 k-v，查询时递归查找

**Context 实现超时控制：**

```go
func doLongJob(ctx context.Context)  {
   time.Sleep(10*time.Second)
   print("done long job")
}

func TestJob(t *testing.T) {
   ctx := context.Background()
   ctx, cancel := context.WithTimeout(ctx, time.Second * 5)
   defer cancel()
   start := time.Now()
   go doLongJob(ctx)
   select {
   case <-ctx.Done():
      t.Log(ctx.Err())
   }
   elapsed := time.Since(start)
}
```

**Context 的使用：**

1. 应该使用 RPCContext 供其它组件使用 Ginex 传递 context 给 kitc/kitex/log等
2. 不要异步使用 ctx 中的某些值 勿异步使用 RPCInfo
3. Log 尽量带上 ctx 可以根据 logid 追踪日志

## 拓展阅读

[golang context](https://draveness.me/golang/docs/part3-runtime/ch06-concurrency/golang-context/)

[dive-into-go-context](https://qcrao.com/2019/06/12/dive-into-go-context/)
