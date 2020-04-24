
---
layout: post
title: "Java基础 系列 ArrayList"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - java
---
> 线程不安全的数组容器。

## ArrayList插入删除
性能取决于删除的元素离数组末端有多远，ArrayList 拿来作为堆栈来用还是挺合适的，push 和 pop 操作完全不涉及数据移动操作。
## ArrayList 遍历和 LinkedList 遍历性能比较
遍历 ArrayList 要比 LinkedList 快得多，ArrayList 遍历最大的优势在于内存的连续性，CPU 的内部缓存结构会缓存连续的内存片段，可以大幅降低读取内存的性能开销。
## ArrayList 扩容
ArrayList 扩容后的大小等于扩容前大小的 1.5 倍，当 ArrayList 很大的时候，这样扩容还是挺浪费空间的，甚至会导致内存不足抛出 OutOfMemoryError 。扩容时还需要对数组进行拷贝，这个也挺费时的。所以使用时要竭力避免扩容，提供一个初始估计容量参数，以免扩容对性能带来较大影响。

## ArrayList 默认数组大小 10
据说是因为 sun 的程序员对一系列广泛使用的程序代码进行了调研，结果就是10这个长度的数组是最常用的最有效率的。
## ArrayList 线程安全
不是。线程安全的数组容器是 Vector。Vector 实现很简单，是把所有的方法统统加上 synchronized 就完事了。也可以不使用 Vector，用 Collections.synchronizedList 把一个普通 ArrayList 包装成一个线程安全版本的数组容器也可以，原理同 Vector 是一样的，就是给所有的方法套上一层 synchronized。

## 数组用来做队列合适么？
队列一般是 FIFO 的，如果用 ArrayList 做队列，就需要在数组尾部追加数据，数组头部删除数组，反过来也可以。但是无论如何总会涉及到数组的数据搬迁，这是比较耗费性能的。

ArrayList 不适合做队列，但是数组是非常合适的。比如 ArrayBlockingQueue 内部实现就是一个环形队列，它是一个定长队列，内部是用一个定长数组来实现的。简单点说就是使用两个偏移量来标记数组的读位置和写位置，如果超过长度就折回到数组开头，前提是它们是定长数组。

