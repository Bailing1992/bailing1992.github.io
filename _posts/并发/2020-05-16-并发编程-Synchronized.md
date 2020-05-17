---
layout: post
title: "并发编程 系列 Synchronized"
subtitle: '开启 并发编程 新篇章'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - Concurrent 
---


## Synchronized 用法
在 Java 中，最简单粗暴的同步手段就是 synchronized 关键字，其同步的三种用法:
1. 同步实例方法，锁是当前实例对象
2. 同步类方法，锁是当前类对象
3. 同步代码块，锁是括号里面的对象

```

public class SynchronizedTest {

    /**
     * 同步实例方法，锁实例对象
     */
    public synchronized void test() {
    }

    /**
     * 同步类方法，锁类对象
     */
    public synchronized static void test1() {
    }

    /**
     * 同步代码块
     */
    public void test2() {
        // 锁类对象
        synchronized (SynchronizedTest.class) {
            // 锁实例对象
            synchronized (this) {

            }
        }
    }
}

```
## Synchronized 实现
javap -verbose查看上述示例:
![jvm](/img/concurrent/27.png)

从图中我们可以看出:
* 同步方法：方法级同步没有通过字节码指令来控制，它实现在方法调用和返回操作之中。当方法调用时，调用指令会检查方法ACC_SYNCHRONIZED访问标志是否被设置，若设置了则执行线程需要持有管程(Monitor)才能运行方法，当方法完成(无论是否出现异常)时释放管程。

* 同步代码块：synchronized 关键字经过编译后，会在同步块的前后分别形成 monitorenter 和 monitorexit 两个字节码指令，每条 monitorenter 指令都必须执行其对应的 monitorexit 指令，为了保证方法异常完成时这两条指令依然能正确执行，编译器会自动产生一个异常处理器，其目的就是用来执行monitorexit指令。

####  锁优化

jdk1.6 中 synchronized 的实现进行了各种优化，如适应性自旋、锁消除、锁粗化、轻量级锁和偏向锁，主要解决三种场景:
1. 只有一个线程进入临界区，偏向锁
2. 多线程未竞争，轻量级锁
3. 多线程竞争，重量级锁
偏向锁→轻量级锁→重量级锁过程，锁可以升级但不能降级，这种策略是为了提高获得锁和释放锁的效率

> synchronized 使用的锁都放在对象头里，JVM 中用 2 个字宽来储存对象头(如果对象是数组则分配 3 个字宽，多的一个字宽用于存储数组的长度)。而对象头包含两部分信息，分别为 Mark Word 和类型指针。Mark Word 主要用于储存对象自身的运行时数据，例如对象的 hashCode、GC 分代年龄、锁状态标志、线程持有的锁、偏向线程的 ID、偏向时间戳等。而类型指针用于标识 JVM 通过这个指针来确定这个对象是哪个类的实例。   
由于对象需要储存的运行时数据过多，Mark Word 被设计成一个非固定的数据结构以便在极小的空间内存储更多的信息。对象在不同的状态下，Mark Word 会存储不同的内容(32位虚拟机)
![jvm](/img/concurrent/31.png)

##### 偏向锁
引入偏向锁的目的是：在没有多线程竞争的情况下，尽量减少不必要的轻量级锁执行路径。相对于轻量级锁，偏向锁只依赖一次CAS原子指令置换ThreadID，不过一旦出现多个线程竞争时必须撤销偏向锁，主要校验是否为偏向锁、锁标识位以及ThreadID。

* 加锁
1. 获取对象的对象头里的 Mark Word
2. 检测 Mark Word 是否为可偏向状态，即 mark 的偏向锁标志位为 1，锁标识位为 01
3. 若为可偏向状态，判断 Mark Word 中的线程 ID 是否为当前线程 ID，如果指向当前线程执行⑥，否则执行④
4. 通过 CAS 操作竞争锁，竞争成功，则将 Mark Word 的线程 ID 替换为当前线程 ID，否则执行⑤
5. 通过 CAS 竞争锁失败，证明当前存在多线程竞争，当到达 safepoint全局安全点(这个时间点是上没有正在执行的代码)，获得偏向锁的线程被挂起，撤销偏向锁，并升级为轻量级，升级完成后被阻塞在安全点的线程继续执行同步代码块
6. 执行同步代码块

* 解锁
线程是不会主动去释放偏向锁，只有当其它线程尝试竞争偏向锁时，持有偏向锁的线程才会释放锁，释放锁需要等待全局安全点。步骤如下:
1. 暂停拥有偏向锁的线程，判断锁对象石是否处于被锁定状态
2. 撤销偏向锁，恢复到无锁状态(01)或者轻量级锁(00)的状态
![jvm](/img/concurrent/29.png)

#### 轻量级锁 
引入轻量级锁的主要目的是在多线程没有竞争的前提下，减少传统的重量级锁使用操作系统互斥量产生的性能消耗。如果多个线程在同一时刻进入临界区，会导致轻量级锁膨胀升级重量级锁，所以轻量级锁的出现并非是要替代重量级锁，在有多线程竞争的情况下，轻量级锁比重量级锁更慢。

* 加锁
1. 获取对象的对象头里的Mark Word
2. 判断当前对象是否处于无锁状态，即mark的偏向锁标志位为0，锁标志位为 01
3. 若是，JVM首先将在当前线程的栈帧中建立一个名为锁记录(Lock Record)的空间，用于存储锁对象目前的Mark Word的拷贝(官方把这份拷贝加了一个Displaced前缀，即Displaced Mark Word)，然后执行④；若不是执行⑤
4. JVM利用CAS操作尝试将对象的Mark Word更新为指向Lock Record的指针，如果成功表示竞争到锁，则将锁标志位变成00(表示此对象处于轻量级锁状态)，执行同步操作；如果失败则执行⑤
5. 判断当前对象的Mark Word是否指向当前线程的栈帧，如果是说明当前线程已经持有这个对象的锁，则直接执行同步代码块；否则说明该锁对象已经被其他线程抢占了，如果有两条以上的线程争用同一个锁，那轻量级锁就不再有效，要膨胀为重量级锁，锁标志位变成10，后面等待的线程将会进入阻塞状态

* 解锁，轻量级锁的释放也是通过CAS操作来进行的，主要步骤如下:
1. 如果对象的Mark Word仍然指向着线程的锁记录，执行②
3. 用CAS操作把对象当前的Mark Word和线程中复制的Displaced Mark Word替换回来，如果成功，则说明释放锁成功，否则执行③
3. 如果CAS操作替换失败，说明有其他线程尝试获取该锁，则需要在释放锁的同时需要唤醒被挂起的线程
![jvm](/img/concurrent/30.png)

#### 重量级锁
重量级锁 通过 对象内部的监视器(monitor)实现，其中 monitor 的本质是依赖于底层操作系统的 Mutex Lock 实现，操作系统实现线程之间的切换需要从用户态到内核态的切换，切换成本非常高。

* 锁膨胀过程
1. 整个膨胀过程在自旋下完成
2. 判断当前是否为重量级锁，即 Mark Word 的锁标识位为 10，如果当前状态为重量级锁，执行步骤（3），否则执行步骤（4）
3. 获取指向 ObjectMonitor 的指针并返回，说明膨胀过程已经完成
4. 如果当前锁处于膨胀中，说明该锁正在被其它线程执行膨胀操作，则当前线程就进行自旋等待锁膨胀完成，这里需要注意一点，虽然是自旋操作，但不会一直占用cpu资源，每隔一段时间会通过 os::NakedYield 方法放弃 cpu 资源，或通过 park 方法挂起；如果其他线程完成锁的膨胀操作，则退出自旋并返回
5. 如果当前是轻量级锁状态，即锁标识位为 00，膨胀过程如下：
  1. 获取一个可用的ObjectMonitor monitor，并重置monitor数据
  2. 通过 CAS 尝试将 Mark Word 设置为 markOopDesc:INFLATING，标识当前锁正在膨胀中，如果 CAS 失败，说明同一时刻其它线程已经将 Mark Word 设置为 markOopDesc:INFLATING，当前线程进行自旋等待膨胀完成
  3. 如果 CAS 成功，设置 monitor 的各个字段：_header、_owner和_object等，并返回


###### 监视器 Monitor
每个对象都拥有自己的监视器，当这个对象由同步块或者这个对象的同步方法调用时，执行方法的线程必须先获取该对象的监视器才能进入同步块和同步方法，如果没有获取到监视器的线程将会被阻塞在同步块和同步方法的入口处，进入到BLOCKED状态，

* monitor竞争         
当锁膨胀完成并返回对应的monitor时，并不表示该线程竞争到了锁
1. 通过 CAS 尝试把 monitor 的 _owner 字段设置为当前线程；
2. 如果设置之前的 _owner 指向当前线程，说明当前线程再次进入 monitor，即重入锁，执行 _recursions ++ ，记录重入的次数；
3. 如果之前的 _owner 指向的地址在当前线程中，即之前 _owner 指向的 BasicLock 在当前线程栈上，说明当前线程是第一次进入该 monitor，设置 _recursions 为 1， _owner 为当前线程，该线程成功获得锁并返回；
4、如果获取锁失败，则等待锁的释放；

* monitor等待       
monitor竞争失败的线程，通过自旋执行ObjectMonitor::EnterI方法等待锁的释放
1. 当前线程被封装成ObjectWaiter对象node，状态设置成ObjectWaiter::TS_CXQ；
2. 在for循环中，通过CAS把node节点push到_cxq列表中，同一时刻可能有多个线程把自己的node节点push到_cxq列表中；
3. node节点push到_cxq列表之后，通过自旋尝试获取锁，如果还是没有获取到锁，则通过park将当前线程挂起，等待被唤醒
4. 当该线程被唤醒时，会从挂起的点继续执行，通过ObjectMonitor::TryLock尝试获取锁，TryLock方法实现如下：

其本质就是通过CAS设置monitor的_owner字段为当前线程，如果CAS成功，则表示该线程获取了锁，跳出自旋操作，执行同步代码，否则继续被挂起；


* monitor释放     
当某个持有锁的线程执行完同步代码块时，会进行锁的释放，给其它线程机会执行同步代码，在HotSpot中，通过退出monitor的方式实现锁的释放，并通知被阻塞的线程
1. 如果是重量级锁的释放，monitor 中的 _owner 指向当前线程，即 THREAD == _owner；
2. 根据不同的策略（由 QMode 指定），从cxq或EntryList中获取头节点，通过 ObjectMonitor::ExitEpilog 方法唤醒该节点封装的线程，唤醒操作最终由 unpark 完成
3. 被唤醒的线程，继续执行 monitor 的竞争；



## 参考文献

https://www.jianshu.com/p/c5058b6fe8e5

https://juejin.im/post/5b42c2546fb9a04f8751eabc