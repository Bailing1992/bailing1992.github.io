---
layout: post
title: "并发编程 系列 Current"
subtitle: '开启并发编程探索新篇章'
author: "lichao"
header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
  - concurrency
---

> ArrayList 不是线程安全的，在读线程读取 ArrayList 的时候如果有写线程写数据，基于 fast-fail 机制，会抛出 ConcurrentModificationException 异常，也就是说 ArrayList 并不是一个线程安全的容器，当然可以用 Vector，或者使用 Collections 的静态方法将 ArrayList 包装成一个线程安全的类，但是这些方式都是采用 Java 关键字 synchronzied 对方法进行修饰，利用独占式锁来保证线程安全的。


