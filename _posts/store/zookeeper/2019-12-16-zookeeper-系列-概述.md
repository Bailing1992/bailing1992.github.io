---
layout: post
title: "Zookeeper 系列 概述"
subtitle: '一个开源的分布式协同服务'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - zookeeper 
---


Zookeeper 是一个开源的分布式协同服务系统，Zookeeper 的设计目标是将那些复杂且容易出错的分布式协同服务封装起来，抽象出一个高效可靠的原语集，并以一系列简单的接口提供给用户使用。

#### 应用场景

Zookeeper 提供了 Master 选举、分布式锁、配置管理（数据发布与订阅）等诸多功能。

> 关于 ZooKeeper 这样的系统功能的讨论都围绕着一条主线：它可以在分布式系统中协作多个任务。一个协作任务是指一个包含多个进程的任务。这个任务可以是为了协作或者是为了管理竞争。协作意味着多个进程需要一同处理某些事情，一些进程采取某些行动使得其他进程可以继续工作。比如，在典型的主-从（master-worker）工作模式中，从节点处于空闲状态时会通知主节点可以接受工作，于是主节点就会分配任务给从节点。竞争则不同。它指的是两个进程不能同时处理工作的情况，一个进程必须等待另一个进程。同样在主-从工作模式的例子中，我们想有一个主节点，但是很多进程也许都想成为主节点，因此我们需要实现互斥排他锁（mutualexclusion）。实际上，我们可以认为获取主节点身份的过程其实就是获取锁的过程，获得主节点控制权锁的进程即主节点进程。

在一台计算机上运行的多个进程和跨计算机运行的多个进程从概念上区别并不大。在多线程情况下有用的同步原语在分布式系统中也同样有效。一个重要的区别在于，在典型的不共享环境下不同的计算机之间不共享除了网络之外的其他任何信息。虽然许多消息传递算法可以实现同步原语，但是使用一个提供某种有序共享存储的组件往往更加简便，这正是ZooKeeper所采用的方式。

协同并不总是采取像群首选举或者加锁等同步原语的形式。配置元数据也是一个进程通知其他进程需要做什么的一种常用方式。比如，在一个主-从系统中，从节点需要知道任务已经分配到它们。即使在主节点发生崩溃的情况下，这些信息也需要有效。

#### 高可用架构(ZAB)

![架构](/img/post/store/zk/架构.png){:height="80%" width="80%"}

Zookeeper 中的 Leader 会定期向 Follower 发送心跳，如果 Follower 超出一定时间（```syncLimit * tickTime```）没有响应，Leader 会将该 Follower 移除下线；同样地，Follower 长期没有收到 Leader 的心跳，会认为 Leader 下线，从而触发选举。
选举时，当有过半 Follower 投票给某一个 Follower 时，该 Follower 即晋升为新 Leader；一些细节：

- 每个 Follower 都会有当前复制 Leader 的进度偏移值（zxid），偏移值越大代表数据越新，Follower 不会投票给比自己偏移值小的 Follower，最终选出的新Leader 会包含已经 Commited 的所有数据。
- Follower可以更改自己的投票，比如Follower4一开始投给自己，后来收到Follower1的消息后，重新投给1，而且进行广播，告诉别人自己投了1。

> 可以简单参考：[Zookeeper Leader 选举原理](https://www.runoob.com/w3cnote/zookeeper-leader.html)

![重新选举](/img/post/store/zk/重新选举.png){:height="80%" width="80%"}

> 类似的架构还有基于Raft的ETCD，其他组件可以在 这类分布式协调组件 之上 构建高可用方案，比如注册(创建)一个Master(节点/Key-Val)，然后只会有一个客户端注册成功；当Master宕机后，其他客户端可以感知到并重新抢占Master。
