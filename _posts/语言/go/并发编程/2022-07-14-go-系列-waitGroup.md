---
layout: post
title: "Go 系列 WaitGroup深入解析"
author: "lichao"
header-img: "img/post/bg/post-bg-ngon-ngu-golang.jpg"
catalog: true
tags:
  - go
---

## 数据结构

源码包中 src/sync/waitgroup.go:WaitGroup 定义了其数据结构： 
```go
type WaitGroup struct { 
	state1 [3]uint32 
} 
```
state1 是个长度为 3 的数组，其中包含了 state 和一个信号量，而 state 实际上是两个计数器： 
- counter： 当前还未执行结束的goroutine计数器 
- waiter count: 等待goroutine-group结束的goroutine数量，即有多少个等候者 
- semaphore: 信号量 
  
考虑到字节是否对齐，三者出现的位置不同，为简单起见，依照字节已对齐情况下，三者在内存中的位置如下所示：
![wait group](/img/post/lang/go/WaitGroup.png)

#### Add(delta int)
Add() 做了两件事，一是把 delta 值累加到 counter 中，因为 delta 可以为负值，也就是说 counter 有可能变成0或 负值，所以第二件事就是当counter 值变为 0 时，跟据 waiter 数值释放等量的信号量，把等待的 goroutine 全部唤醒，如果 counter 变为负值，则 panic。

Add() 伪代码如下：
```go
func (wg *WaitGroup) Add(delta int) { 
	statep, semap := wg.state() // 获取 state 和 semaphore 地址指针 
	
	state := atomic.AddUint64(statep, uint64(delta)<<32) //把 delta 左移 32 位累加到 state，即累加到 counter 中 
	v := int32(state >> 32) //获取counter值 
	w := uint32(state) //获取waiter值 
	if v < 0 { //经过累加后counter值变为负值，panic 
		panic("sync: negative WaitGroup counter") 
	} 
	// 经过累加后，此时，counter >= 0 
	// 如果 counter 为正，说明不需要释放信号量，直接退出 
	// 如果 waiter 为零，说明没有等待者，也不需要释放信号量，直接退出 
	if v > 0 || w == 0 { 
		return 
	} 
	// 此时，counter 一定等于 0，而 waiter 一定大于 0（内部维护 waiter，不会出现小于 0 的情况），
	// 先把 counter 置为 0，再释放 waiter 个数的信号量 
	*statep = 0 
	for ; w != 0; w-- { 
		runtime_Semrelease(semap, false) //释放信号量，执行一次释放一个，唤醒一个等待者 
	} 
}
```

#### Wait()
Wait() 方法也做了两件事，一是累加 waiter, 二是阻塞等待信号量
```go
func (wg *WaitGroup) Wait() { 
	statep, semap := wg.state() //获取state和semaphore地址指针 
	for { 
		state := atomic.LoadUint64(statep) //获取state值
		v := int32(state >> 32) // 获取counter值 
		w := uint32(state) //获取waiter值 
		if v == 0 { //如果counter值为0，说明所有goroutine都退出了，不需要待待，直接返回 
			return  
		} 
		// 使用CAS（比较交换算法）累加waiter，累加可能会失败，失败后通过for loop下次重试 
		if atomic.CompareAndSwapUint64(statep, state, state+1) { 
			runtime_Semacquire(semap) //累加成功后，等待信号量唤醒自己 
			return 
		} 
	} 
}
```
这里用到了 CAS 算法保证有多个 goroutine 同时执行 Wait() 时也能正确累加 waiter。
####  Done()
Done()只做一件事，即把counter减1，我们知道Add()可以接受负值，所以Done实际上只是调用了Add(-1)。 

源码如下：
```go
func (wg *WaitGroup) Done() {
	wg.Add(-1)
}
```
Done()的执行逻辑就转到了Add()，实际上也正是最后一个完成的goroutine把等待者唤醒的。


## 编程Tips
- Add()操作必须早于Wait(), 否则会panic 
- Add()设置的值必须与实际等待的goroutine个数一致，否则会panic