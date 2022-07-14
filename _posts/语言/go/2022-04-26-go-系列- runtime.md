---
layout: post
title: "Go 系列 性能调优"
author: "lichao"
header-img: "img/post/bg/post-bg-ngon-ngu-golang.jpg"
catalog: true
tags:
  - go
---

Go runtime 是 go 语言的基础设施，完成任务：
- 特化语法处理
- map、channel、slice、string 内置类型的实现
- 协程调度、内存分配、GC
- OS 和 CPU 相关的封装
- pprof，trace，race检测的支持

> 特化语法处理
每一种语言都有一些特化语言，开发者无法自己在程序中实现，比如Java的String，不需要引入java.lang, 而自己实现的String类必须要引入自己的包。go中最重要的特化语法就是go关键字可以新启动一个协程。编译时，go中的特化语言会替换成runtime包中的私有函数。
![协程切换](/img/post/lang/go/特化语法.png)

> 协程的管理
runtime包负责管理和切换协程，具体会在下面详细描述。

> 内存分配
go的内存分配早期是基于 tcmalloc，后面改动较大。golang完整的实现了内存分配机制。
这里简单的介绍几个相关的Go内存分配规则
