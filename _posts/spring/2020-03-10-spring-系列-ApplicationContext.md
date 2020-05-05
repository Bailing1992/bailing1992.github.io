---
layout: post
title: "Spring 系列 ApplicationContext"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - spring
---

常见的创建 Application 的方式有三种:
* FileSystemXmlApplicationContext
* ClassPathXmlApplicationContext
* AnnotationConfigApplicationContext

内部会调用refresh方法
## refresh方法

#### bean 加载
![dubbo](/img/spring/4.png)