---
layout: post
title: "Java 基础 HashTable"
subtitle: '深究Java基础'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - java 
---

HashTable 类是线程安全的，它使用 synchronized 来做线程安全，全局只有一把锁，在线程竞争比较激烈的情况下hashtable的效率是比较低下的
