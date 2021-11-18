---
layout: post
title: "JVM 系列 内存管理之内存区域"
subtitle: '开启JVM探索新篇章'
author: "lichao"
header-img: "img/post-bg-digital-native.jpg"
catalog: true
tags:
  - jvm
---

Java 内存模型 要求 lock、unlock、read、load、assign、use、store、write 这 8 个操作都具有原子性，但是对于 64 位的数据类型（long 和 double），在模型中特别定义了一条相对宽松的规定：允许虚拟机将没有被volatile修饰的 64 位数据的读写操作划分为 2 次 32 位的操作来执行，即允许虚拟机实现选择可以不保证 64 位数据类型的 read、store、load 和 write 这 4 个操作的原子性。这就是所谓的 long 和 double 的非原子性协定（Nonatomic Treatment of double and long Variables）。不过目前各种平台下的商用虚拟机几乎都选择把64位数据的读写操作作为原子操作来对待，因此编码时，一般不需要把用到的 long 和 double 变量专门声明为 volatile。

JVM 规范在涉及线程、同步等的方面的规定是委托给Java语言规范定义的，而Java语言规范里的相关定义：
* 实现对普通long与double的读写不要求是原子的（但如果实现为原子操作也OK）
* 实现对volatile long与volatile double的读写必须是原子的（没有选择余地）