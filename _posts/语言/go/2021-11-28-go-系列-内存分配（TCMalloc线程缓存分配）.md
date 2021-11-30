---
layout: post
title: "Go 系列 内存分配（TCMalloc线程缓存分配）"
author: "lichao"
header-img: "img/post/bg/post-bg-ngon-ngu-golang.jpg"
catalog: true
tags:
  - go
---


线程缓存分配（Thread-Caching Malloc，TCMalloc）是用于分配内存的机制，它比 glibc 中的 ```malloc``` 还要快很多。[“TCMalloc : Thread-Caching Malloc”](https://gperftools.github.io/gperftools/tcmalloc.html)。Go 语言的内存分配器就借鉴了 TCMalloc 的设计实现高速的内存分配，它的核心理念是使用多级缓存将对象根据大小分类，并按照类别实施不同的分配策略。

Go 语言的内存分配器会根据申请分配的内存大小选择不同的处理逻辑，运行时根据对象的大小将对象分成微对象、小对象和大对象三种：

|类别	| 大小| 
|  ----  | ----  |
|微对象 |	(0, 16B)|
|小对象	 | [16B, 32KB]|
|大对象	| (32KB, +∞)|

因为程序中的绝大多数对象的大小都在 32KB 以下，而申请的内存大小影响 Go 语言运行时分配内存的过程和开销，所以分别处理大对象和小对象有利于提高内存分配器的性能。

内存分配器不仅会区别对待大小不同的对象，还会将内存分成不同的级别分别管理，TCMalloc 和 Go 运行时分配器都会引入线程缓存（Thread Cache）、中心缓存（Central Cache）和页堆（Page Heap）三个组件分级管理内存：

![多级缓存](/img/post/lang/go/多级缓存.png)

线程缓存属于每一个独立的线程，它能够满足线程上绝大多数的内存分配需求，因为不涉及多线程，所以也不需要使用互斥锁来保护内存，这能够减少锁竞争带来的性能损耗。当线程缓存不能满足需求时，运行时会使用中心缓存作为补充解决小对象的内存分配，在遇到 32KB 以上的对象时，内存分配器会选择页堆直接分配大内存。


这种多层级的内存分配设计与计算机操作系统中的多级缓存有些类似，因为多数的对象都是小对象，我们可以通过线程缓存和中心缓存提供足够的内存空间，发现资源不足时从上一级组件中获取更多的内存资源。

## 内存块
Go 语言的内存块分为多级：
- arena：堆从操作系统申请到大块的内存，64位系统大小是64MB其他的大部分是4MB。
- span：由多个地址连续的内存page组成的大块内存，是内存管理的基本单位。
- object：将span按照特定大小切分成多个小块，每个小块可存储一个对象。
#### object
Go语言将分配的object对象分为两类。一种是大于32KB的大对象，会直接从heap申请。对于其他小于等于32KB的object，Go语言会尝试从叫做mcache的本地缓存里获取内存。mcache保存着叫做
mspan的内存块列表。
#### mspan
object面向的是对象分配，而span面向的是内部的管理。
- Go语言中实现span概念的就是mspan，结构定义在runtime/heap.go中。mspan是一个双向链表的节点，便于管理。
- mspan有66种class，分别用来保存不同大小的object。
每种class定义了内存块的大小，和保存object的大小。这种方式虽然会造成一些内存浪费，但是面对有限几种规格的内存块，便于优化内存管理策略。而且内存分配时会尝试将多个微小的对象组合到一个object块内，以节约内存。
runtime/sizeclass.go中提供了mspan的66个类的表。

```
// class  bytes/obj  bytes/span  objects  tail waste  max waste
//     1          8        8192     1024           0     87.50%
//     2         16        8192      512           0     43.75%
//     3         32        8192      256           0     46.88%
//     4         48        8192      170          32     31.52%
//     5         64        8192      128           0     23.44%
//     6         80        8192      102          32     19.07%
。
。
。
//    66      32768       32768        1           0     12.50%
```

## 管理组件
Go语言内存分配器由mcache，mcentral，mheap三种组件构成。
* cache：每个P都会绑定一个cache，用于无锁的object分配。mcache中有sizeClass个双向链表来分别管理各种mspan。
* central：为所有的cache提供切分好的后备span资源。每个mcentral只管理一种sizeClass
* heap：管理闲置的span，需要时向操作系统申请arena。