---
layout: post
title: "RocketMQ 系列 nameServer"
subtitle: '刨析rocketMQ的底层实现'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - rocketMQ
---

> 主要介绍RocketMQ 路由管理、服务注册及服务发现的机制

## 概念
##### 服务发现
分布式服务SOA 架构体系中会有服务注册中心，分布式服务SOA 的注册中心主要提供服务调用的解析服务，指引服务调用方（消费者）找到“远方”的服务提供者，完成网络通信.

#### NameServer 整体架构设计
* Broker消息服务器在启动时向所有NameServer注册
* 消息生产者（Producer）在发送消息之前先从Name Server 获取Broker 服务器地址列表，然后根据负载算法从列表中选择一台消息服务器进行消息发送。


#### NameServer 动态路由发现与剔除机制


## 问题

#### nameServer的职责？
为了避免消息服务器broker的单点故障导致的整个系统瘫痪，通常会部署多台消息服务器共同承担消息的存储。那消息生产者如何知道消息要发往哪台消息服
务器呢？如果某一台消息服务器宕机了，那么生产者如何在不重启服务的情况下感知呢？ NameServer就是为了解决上述问题而设计的
#### 如何避免nameServer 的单点故障，提供高可用？
