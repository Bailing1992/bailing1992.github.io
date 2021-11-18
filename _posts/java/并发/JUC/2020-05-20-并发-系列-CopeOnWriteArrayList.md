---
layout: post
title: "并发编程 系列 CopeOnWriteArrayList"
subtitle: '开启并发编程探索新篇章'
author: "lichao"
header-img: "img/post/bg/post-bg-2015.jpg"
catalog: true
tags:
  - concurrency
---

> Doug Lea 大师 提供 CopyOnWriteArrayList 容器可以保证线程安全，保证读读之间在任何时候都不会被阻塞。

如果简单的使用读写锁的话，在写锁被获取之后，读写线程被阻塞，只有当写锁被释放后读线程才有机会获取到锁从而读到最新的数据，站在读线程的角度来看，即读线程任何时候都是获取到最新的数据，满足数据实时性。既然说到要进行优化，必然有 trade-off，就可以牺牲数据实时性满足数据的最终一致性即可。而 CopyOnWriteArrayList 就是通过 Copy-On-Write(COW)，即写时复制的思想来通过延时更新的策略来实现数据的最终一致性，并且能够保证读线程间不阻塞。

COW 通俗的理解是当往一个容器添加元素的时候，不直接往当前容器添加，而是先将当前容器进行 Copy，复制出一个新的容器，然后新的容器里添加元素，添加完元素之后，再将原容器的引用指向新的容器。对 CopyOnWrite 容器进行并发的读的时候，不需要加锁，因为当前容器不会添加任何元素。所以 CopyOnWrite 容器也是一种读写分离的思想，延时更新的策略是通过在写的时候针对的是不同的数据容器来实现的，放弃数据实时性达到数据的最终一致性。
