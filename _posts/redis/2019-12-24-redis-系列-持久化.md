---
layout: post
title: "redis 系列 持久化"
subtitle: '开启 redis 探索新篇章'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - redis 
---

> Redis 的数据全部在内存里，需要持久化机制保证 Redis 的数据不会因为故障而丢失。


Redis 持久化机制有两种：
* 第一种是快照
* 第二种是 AOF 日志
快照是一次全量备份，AOF 日志是连续的增量备份。快照是内存数据的二进制序列化形式，在存储上非常紧凑，而 AOF 日志记录的是内存数据修改的指令记录文本。AOF 日志在长期的运行过程中会变的无比庞大，数据库重启时需要加载 AOF 日志进行指令重放，这个时间就会无比漫长。所以需要定期进行 AOF 重写，给 AOF 日志进行瘦身。