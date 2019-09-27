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

NameServer 主要作用是为消息生产者和消息消费者提供关于主题Topic 的路由信息，那么NameServer 需要存储路由的基础信息，还要能够管理Broker 节点，包括路由注册、路由删除等功能。

**NameServer本身的高可用可通过部署多台NameServer服务器来实现，但彼此之间互不通信，也就是NameServer服务器之间在某一时刻的数据并不会完全相同，但这对消息发送不会造成任何影响，这也是RocketMQ NameServer 设计的一个亮点， RocketMQNameServer 设计追求简单高效。**

## 概念
##### 服务发现
分布式服务SOA 架构体系中会有服务注册中心，分布式服务SOA 的注册中心主要提供服务调用的解析服务，指引服务调用方（消费者）找到“远方”的服务提供者，完成网络通信.

#### NameServer 整体架构设计
* Broker 消息服务器 在启动时向所有NameServer注册
* 消息生产者（Producer）在发送消息之前先从NameServer获取Broker服务器地址列表，然后根据负载算法从列表中选择一台消息服务器进行消息发送。
* NameServer与每台Broker服务器保持长连接，并间隔30s检测Broker是否存活，如果检测到Broker宕机， 则从路由注册表中将其移除。
#### NameServer 动态路由发现与剔除机制
RocketMQ 路由注册是通过Broker 与Name Server 的心跳功能实现的。Broker启动时向集群中所有的NameServer发送心跳语句，每隔30s向集群中所有NameServer发送心跳包，NameServer收到Broker心跳包时会更新brokerLiveTable 缓存中 BrokerLivelnfo.lastUpdateTimestamp，然后 NameServer每隔10s扫描brokerLiveTable ，如果连续120s 没有收到心跳包，NameServer 将移除该Broker的路由信息同时关闭Socket连接。

#### 路由注册

Broker 每隔30s向NameServer 发送一个心跳包，心跳包中包含BrokerId 、Broker地址、Broker名称、Broker所属集群名称、Broker关联的FilterServer 列表。
* 路由注册需要加写锁，防止并发修改RoutelnfoManager 中的路由表。首先判断Broker 所属集群是否存在， 如果不存在，则创建，然后将broker加入到集群Broker集合中 clusterAddrTable
* 维护BrokerData信息，首先从brokerAddrTable 根据BrokerName 尝试获取Broker信息，如果不存在， 则新建BrokerData 并放入到brokerAddrTable , registerFirst设置为true ；如果存在， 直接替换原先的， registerFirst 设置为false，表示非第一次注册。
* 如果Broker 为Master ，并且Broker Topic 配置信息发生变化或者是初次注册，则需要创建或更新Topic 路由元数据，填充topicQueueTable ， 其实就是为默认主题自动注册路由信息，其中包含Mi xA II.DEFAULT TOPIC 的路由信息。当消息生产者发送主题时，如果该主题未创建并且BrokerConfig 的autoCreateTopicEnable 为true 时， 将返回MixAII.DEFAULT TOPIC 的路由信息。
* 更新BrokerLivelnfo ，存活Broker 信息表， BrokeLivelnfo 是执行路由删除的重要依据
* 注册Broker的过滤器Server 地址列表，一个Broker 上会关联多个FilterServer消息过滤服务器，此部分内容将在第6 章详细介绍；如果此Broker 为从节点，则需要查找该Broker 的Master 的节点信息，并更新对应的masterAddr 属性。

> Name Serve 与Broker 保持长连接， Broker 状态存储在brokerLiveTable 中，
NameServer 每收到一个心跳包，将更新brokerLiveTable 中关于Broker 的状态信息以及路
由表（ topicQueueTable 、brokerAddrTable 、brokerLiveTable 、filterServerTable ） 。更新上述
路由表（ HashTable ）使用了锁粒度较少的读写锁，允许多个消息发送者（Producer ）并发读，
保证消息发送时的高并发。但同一时刻NameServer 只处理一个Broker 心跳包，多个心跳
包请求串行执行。这也是读写锁经典使用场景，更多关于读写锁的信息

#### 路由删除
Name Server 会每隔I Os 扫描brokerLiveTable 状态表，如果BrokerLive 的lastUpdateTimestamp 的时间戳距当前时间超过120s ，则认为Broker失效，移除该Broker,关闭与Broker 连接，并同时更新topicQueueTable 、brokerAddrTable 、brokerLive Table 、
filterServerTable 。RocktMQ 有两个触发点来触发路由删除：
1. NameServer 定时扫描brokerLiveTable 检测上次心跳包与当前系统时间的时间差，
如果时间戳大于120s ，则需要移除该Broker 信息。
2. Broker 在正常被关闭的情况下，会执行unr巳gisterBroker 指令。
由于不管是何种方式触发的路由删除，路由删除的方法都是一样的，就是从topicQueueTable 、brokerAddrTable 、brokerLiveTable 、filterServerTable 删除与该Broker 相关的信息

#### 路由发现
RocketMQ 路由发现是非实时的，当Topic路由出现变化后，NameServer不主动推送给客户端， 而是由客户端定时拉取主题最新的路由

#### KV 配置

#### 开启定时任务
#### 路由元信息



## 问题

#### nameServer的职责？
为了避免消息服务器broker的单点故障导致的整个系统瘫痪，通常会部署多台消息服务器共同承担消息的存储。那消息生产者如何知道消息要发往哪台消息服
务器呢？如果某一台消息服务器宕机了，那么生产者如何在不重启服务的情况下感知呢？ NameServer就是为了解决上述问题而设计的
#### 如何避免 nameServer 的单点故障，提供高可用？
NameServer 本身的高可用可通过部署多台NameServer 服务器来实现，但彼此之间互不通信，也就是NameServer 服务器之间在某一时刻的数据并不会完全相同，但这对消 息发送不会造成任何影响，这也是RocketMQ NameServer 设计的一个亮点， RocketMQ NameServer 设计追求简单高效。
#### nameServer的路由变化不会马上通知消息生产者，为什么要这样设计呢？
这是为了降低NameServer实现的复杂性，在消息发送端提供容错机制来保证消息发送的高可用性

#### 路由信息使用读写锁的原因？
路由表（ HashTable ）使用了锁粒度较少的读写锁，允许多个消息发送者（Producer）并发读，保证消息发送时的高并发。但同一时刻NameServer只处理一个Broker心跳包，多个心跳包请求串行执行。这也是读写锁经典使用场景，更多关于读写锁的信息