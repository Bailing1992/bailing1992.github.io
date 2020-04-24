---
layout: post
title: "IO 系列 Selector"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - io
---

## 多路复用（事件轮询）
![存储概览](/img/io/10.png)
最简单的事件轮询 API 是 select 函数，它是操作系统提供给用户程序的 API。输入是读写描述符列表 read_fds & write_fds，输出是与之对应的可读可写事件。同时还提供了一个 timeout 参数，如果没有任何事件到来，那么就最多等待 timeout 时间，线程处于阻塞状态。一旦期间有任何事件到来，就可以立即返回。时间过了之后还是没有任何事件到来，也会立即返回。拿到事件后，线程就可以继续挨个处理相应的事件。处理完了继续过来轮询。于是线程就进入了一个死循环，这个死循环称为事件循环，一个循环为一个周期。


```
read_events, write_events = select(read_fds, write_fds, timeout)
```

> 每个客户端套接字 socket 都有对应的读写文件描述符。

通过 select 系统调用同时处理多个通道描述符的读写事件，因此将这类系统调用称为多路复用 API。现代操作系统的多路复用 API 已经不再使用 select 系统调用，而改用 epoll(linux) 和 kqueue(freebsd & macosx)，因为 select 系统调用的性能在描述符特别多时性能会非常差。

## Selector
![存储概览](/img/io/9.png)
Open JDK 中 Selector 的实现是 SelectorImpl，然后 SelectorImpl 又将职责委托给了具体的平台，比如图中框出的 Linux 2.6 以后才有的 EpollSelectorImpl， Windows 平台的 WindowsSelectorImpl， MacOSX 平台的 KQueueSelectorImpl。

## 源码追踪
#### 获取 Selector
Selector.open() 可以得到一个 Selector 实例:

###