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

ConcurrentHashMap没有对整个hash表进行锁定，而是采用了分离锁（segment）的方式进行局部锁定。具体体现在，它在代码中维护着一个segment数组。

ConcurrentHashMap 使用了分段锁技术来提高了并发度，不在同一段的数据互相不影响，多个线程对多个不同的段的操作是不会相互影响的。每个段使用一把锁