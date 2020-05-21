---
layout: post
title: "Zookeeper 系列 综述"
subtitle: '开启 Zookeeper 探索新篇章'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - zookeeper 
---


分布式协调技术主要用来解决分布式环境中多个进程之间的同步控制，让进程间有序的去访问某种临界资源，防止造成"脏数据"。

ZooKeeper 是一种为分布式应用所设计的高可用、高性能且一致的开源协调服务，它提供了一项基本服务：分布式锁服务。由于 ZooKeeper 的开源特性，后来开发者在分布式锁的基础上，摸索了出了其他的使用方法：配置维护、组服务、分布式消息队列、分布式通知/协调等。

注意：ZooKeeper 性能上的特点决定了它能够用在大型的、分布式的系统当中。从可靠性方面来说，它并不会因为一个节点的错误而崩溃。除此之外，它严格的序列访问控制意味着复杂的控制原语可以应用在客户端上。ZooKeeper 在一致性、可用性、容错性的保证，也是ZooKeeper的成功之处，它获得的一切成功都与它采用的协议——Zab协议是密不可分的。

## 简介
https://www.cnblogs.com/wuxl360/p/5817471.html
