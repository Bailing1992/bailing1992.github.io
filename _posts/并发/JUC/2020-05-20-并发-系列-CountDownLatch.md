---
layout: post
title: "并发编程 系列 CopeOnWriteArrayList"
subtitle: '开启并发编程探索新篇章'
author: "lichao"
header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
  - concurrency
---

执行 await 方法的线程阻塞，直到 countDown 方法执行 N 次使得计数器为 1 时执行。  

CountDownLatch 主要有两个方法：countDown() 和 await()。countDown() 方法用于使计数器减一，其一般是执行任务的线程调用，await()方法则使调用该方法的线程处于等待状态，其一般是主线程调用。
