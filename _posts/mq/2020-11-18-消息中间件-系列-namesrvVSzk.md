---
layout: post
title: "MQ 系列 namesrv VS zookeeper"
subtitle: '消息中间件基本特性'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - mq
---

## namesrv VS zk

Kafka 通过 zookeeper 来进行协调，而rocketMq通过自身的namesrv进行协调。

RocketMQ 在协调节点的设计上显得更加轻量，用了另外一种方式解决高可用的问题，思路也是可以借鉴的。

> Kafka 具备选举功能，在Kafka里面，Master/Slave的选举，有2步：第1步，先通过ZK在所有机器中，选举出一个KafkaController；第2步，再由这个Controller，决定每个partition的Master是谁，Slave是谁。因为有了选举功能，所以kafka某个partition的master挂了，该partition对应的某个slave会升级为主对外提供服务。
>
> RocketMQ 不具备选举，Master/Slave 的角色也是固定的。当一个 Master 挂了之后，你可以写到其他 Master 上，但不能让一个 Slave 切换成 Master。那么 RocketMQ 是如何实现高可用的呢，其实很简单，RocketMQ 的所有broker节点的角色都是一样，上面分配的 topic 和对应的 queue 的数量也是一样的，Mq只能保证当一个broker挂了，把原本写到这个broker的请求迁移到其他 broker 上面，而并不是这个 broker 对应的 slave 升级为主。
