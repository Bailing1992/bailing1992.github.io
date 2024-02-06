---
layout: post
title: "RocksDB"
subtitle: '分析 高可用的分布式 KV 存储'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - nosql 
---

RocksDB 是 facebook 基于 LevelDB 开发的单机存储引擎，主要开发语言为 C++。

## LSM 