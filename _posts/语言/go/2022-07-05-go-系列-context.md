---
layout: post
title: "Go 系列 context标准库"
author: "lichao"
header-img: "img/post/bg/post-bg-ngon-ngu-golang.jpg"
catalog: true
tags:
  - go
---


> context 设计目的是跟踪 goroutine 调用树，并在这些 goroutine 调用树中传递通知与元数据。context 包提供的核心功能是多个 goroutine 之间的退出通知机制，传递数据只是一个辅助功能，应谨慎使用 context 传递数据。


#### Context 源码
**Context 接口**

```go
type Context interface {
    Deadline() (deadline time.Time, ok bool)  // 完成工作的截止日期
    Done() <-chan struct{}                    // 当前工作完成或者上下文被取消后关闭
    Err() error                               // 返回 context 取消原因
    Value(key interface{}) interface{}        // 获取之前设置的 key 对应的 value
}
```

Done() 返回一个空只读 channel，可以表示 context 被取消的信号：当这个 channel 被关闭时，说明 context 被取消了。这是一个只读的channel， 读一个关闭的 channel 会读出相应类型的零值。并且源码里没有地方会向这个 channel 里面塞入值（只会close）。因此在子协程里读这个 channel，除非被关闭，否则读不出来任何东西。也正是利用了这一点，子协程从 channel 里读出了值后，就可以做一些收尾工作，尽快退出。同样在父协程中也可以读这个channel，监听子协程的cancel。

**Canceler 接口**

```go
type canceler interface {  
    cancel(removeFromParent bool, err error)  
    Done() <-chan struct{}  
}
```

实现了上面定义的两个方法的 Context，就表明该 Context 是可取消的。源码中有两个类型实现了 canceler 接口：*cancelCtx 和 *timerCtx。

**emptyCtx**

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

**cancelContext**

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

**timerCtx**

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
**Context 的作用**

在 goroutine 之间传递上下文信息，包括：取消信号、超时时间、截止时间、k-v 等。

**Context 的原理**
1. 取消信号：通过关闭 done channel 通知监听者
2. 超时时间：通过Timer自动触发 cancel 关闭 channel
3. 存值：通过链表生成一个新的节点存储 k-v，查询时递归查找

**Context 实现超时控制**

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

**Context 的使用**
1. 应该使用 RPCContext 供其它组件使用 Ginex 传递 context 给 kitc/kitex/log等 
2. 不要异步使用 ctx 中的某些值 勿异步使用 RPCInfo 
3. Log 尽量带上 ctx 可以根据 logid 追踪日志

## 拓展阅读 
https://draveness.me/golang/docs/part3-runtime/ch06-concurrency/golang-context/

https://qcrao.com/2019/06/12/dive-into-go-context/
