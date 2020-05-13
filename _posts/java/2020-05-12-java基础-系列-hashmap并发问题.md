---
layout: post
title: "Java 基础 HashMap"
subtitle: '深究Java基础'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - Java 
---

HashMap 的设计目标是简洁高效，没有采取任何措施保证 put、remove 操作的多线程安全。下面这张图是 JDK 1.8 的 HashMap 的 put 方法的操作逻辑，可以发现 put 方面的操作对象要么是整个散列表，要么是某个哈希桶里的链表或红黑树，而这些过程都没有采取措施保证多线程安全。在这个复杂的逻辑过程中，任何一个线程在这个过程中改动了散列表的结构，都有可能造成另一个线程的操作失败。

![java](/img/java/1.jpg)
