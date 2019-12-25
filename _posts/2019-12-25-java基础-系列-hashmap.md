---
layout: post
title: "java 基础 HashMap"
subtitle: '深究Java基础'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - java 
---

## HashMap线程安全性问题
#### 多线程put可能导致元素的丢失
#### put和get并发时，可能导致get为null
#### JDK7中 HashMap 并发 put 会造成循环链表，导致 get 时出现死循环
**此问题在JDK8中已经解决**