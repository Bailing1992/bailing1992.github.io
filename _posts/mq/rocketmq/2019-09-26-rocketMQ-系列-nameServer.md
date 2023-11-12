---
layout: post
title: "RocketMQ 系列 NameServer"
subtitle: '刨析 RocketMQ 的底层实现'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - rocket_mq
---

> 本文主要介绍 RocketMQ 路由管理、服务注册及服务发现的机制。RocketMQ 的架构设计决定了只需要一个轻量级的元数据服务器就足够了，只需要保持最终一致，而不需要 Zookeeper这样的强一致性解决方案，不需要再依赖另一个中间件，从而减少整体维护成本。

NameServer 主要作用是**为消息生产者和消息消费者提供关于 Topic 的路由信息**，那么 NameServer 需要存储路由的基础信息，还要能够管理 Broker 节点，包括 路由注册、路由删除等功能。

**NameServer 高可用可通过部署多台 NameServer 服务器来实现，但彼此之间互不通信，也就是 NameServer 服务器之间在某一时刻的数据并不会完全相同，但这对消息发送不会造成任何影响，这也是 NameServer 设计的一个亮点，NameServer 设计追求简单高效。**

## NameServer 整体架构设计

![存储概览](/img/rocketmq/framework2.png)

1. NameServer 是一个几乎无状态节点，可集群部署，节点之间无任何信息同步。
2. Broker 分为 Master 与 Slave，一个 Master 可以对应多个 Slave，但是一个 Slave 只能对应一个 Master，Master 与 Slave 的对应关系通过指定相同的 BrokerName，不同的 BrokerId 来定义，BrokerId 为 0 表示 Master，非 0 表示 Slave。
3. 每个 Broker 与 NameServer 集群中的每个节点建立长链接，Broker 在启动时向所有 NameServer 注册，并且每隔 30s 向 NameServer 发送心跳。
4. Producer 与 NameServer 集群中的其中一个节点（随机选择）建立长链接，在发送消息之前先从此 NameServer 获取 Broker 服务器地址列表，然后根据负载算法从列表中选择一台消息服务器进行消息发送。定期从 NameServer 取 Topic 路由信息。Producer 向提供 Topic 服务的 Master 建立长连接，且定时向 Master 发送心跳。Producer 完全无状态，可集群部署。
5. Consumer 与 NameServer 集群中的其中一个节点（随机选择）建立长连接，定期从 NameServer 取 Topic 路由信息，并向提供 Topic 服务的 Master、Slave 建立长连接，且定时向 Master、Slave 发送心跳。Consumer 既可以从 Master 订阅消息，也可以从 Slave 订阅消息，订阅规则由 Broker 配置决定。
6. NameServer 与每台 Broker 服务器保持长连接，并间隔 10s 检测 Broker 是否存活，如果检测到 Broker 宕机， 则从路由注册表中将其移除。

### NameServer 动态路由发现与剔除机制

RocketMQ 路由注册是通过 Broker 与 NameServer 的心跳功能实现的。Broker 启动时向集群中所有的 NameServer 发送心跳语句，每隔 30s 向集群中所有 NameServer 发送心跳包，NameServer 收到 Broker 心跳包时会更新 brokerLiveTable 缓存中 BrokerLivelnfo.lastUpdateTimestamp，然后 NameServer 每隔 10s 扫描 brokerLiveTable ，如果连续 120s 没有收到心跳包，NameServer 将移除该 Broker 的路由信息同时关闭 Socket 连接。

### 客户端 NameServer 节点的选择策略

RocketMQ 会将用户设置的 NameServer 列表会设置到 NettyRemotingClient 类的 namesrvAddrList 字段中。具体选择哪个 NameServer，也是使用 随机初始位置+ round-robin 的策略。需要注意的是，尽管使用 round-robin 策略，但是在选择了一个 NameServer 节点之后，后面总是会优先选择这个 NameServer，除非与这个 NameServer 节点通信出现异常的情况下，才会选择其他节点。

### 路由注册

![存储概览](/img/rocketmq/rocketmq_3.png)
Broker 每隔 30s 向 NameServer 发送一个心跳包，心跳包中包含 BrokerId 、Broker 地址、Broker 名称、Broker 所属集群名称、Broker 关联的 FilterServer 列表。心跳包中还包括主题的路由信息（主题的读写队列数、操作权限等），NameServer 会通过 HashMap 更新 Topic 的路由信息，并记录最后一次收到 Broker的时间戳。

1. 路由注册需要加写锁， 防止并发修改 RoutelnfoManager 中的路由表。首先判断 Broker 所属集群是否存在，如果不存在，则创建，然后将 Broker 加入到集群 Broker 集合中clusterAddrTable
2. 维护 BrokerData 信息，首先从 brokerAddrTable 根据 BrokerName 尝试获取 Broker 信息，如果不存在则新建 BrokerData 并放入到 brokerAddrTable , registerFirst 设置为 true。如果存在，直接替换原先的，registerFirst 设置为 false，表示非第一次注册。
3. 如果 Broker 为 Master ，并且 BrokerTopic 配置信息发生变化或者是初次注册，则需要创建或更新 Topic 路由元数据，填充topicQueueTable ， 其实就是为默认主题自动注册路由信息，其中包含 MixAII.DEFAULTTOPIC 的路由信息。当消息生产者发送主题时，如果该主题未创建并且 BrokerConfig 的 autoCreateTopicEnable 为 true 时， 将返回 MixAII.DEFAULT TOPIC 的路由信息。
4. 更新 BrokerLivelnfo ，存活 Broker信息表， BrokeLivelnfo 是执行路由删除的重要依据
5. 注册 Broker 的过滤器 Server 地址列表，一个Broker上会关联多个FilterServer消息过滤服务器。如果此Broker 为从节点，则需要查找该Broker 的Master 的节点信息，并更新对应的masterAddr 属性。

> NameServe 与 Broker 保持长连接， Broker 状态存储在 brokerLiveTable 中，NameServer 每收到一个心跳包，将更新brokerLiveTable 中关于Broker 的状态信息以及路由表（ topicQueueTable 、brokerAddrTable 、brokerLiveTable 、filterServerTable ） 。更新上述路由表（ HashTable ）使用了锁粒度较少的读写锁，允许多个消息发送者（Producer ）并发读，保证消息发送时的高并发。但同一时刻 NameServer 只处理一个Broker心跳包，多个心跳包请求串行执行。这也是读写锁经典使用场景，更多关于读写锁的信息。
>
### 路由删除

NameServer 会每隔 1Os 扫描 brokerLiveTable 状态表，如果 BrokerLive的lastUpdateTimestamp 的时间戳距当前时间超过120s ，则认为Broker失效，移除该Broker，关闭与 Broker 连接，并同时更新topicQueueTable 、brokerAddrTable 、brokerLive Table 、filterServerTable 。RocktMQ 有两个触发点来触发路由删除：

1. NameServer 定时扫描 brokerLiveTable 检测上次心跳包与当前系统时间的时间差，
如果时间戳大于 120s ，则需要移除该 Broker 信息。
2. Broker 在正常被关闭的情况下，会执行 unregisterBroker 指令。由于不管是何种方式触发的路由删除，路由删除的方法都是一样的，就是从topicQueueTable 、brokerAddrTable 、brokerLiveTable 、filterServerTable 删除与该 Broker 相关的信息

### 路由发现

RocketMQ 路由发现是非实时的，当 Topic 路由出现变化后，NameServer 不主动推送给客户端， 而是由客户端定时拉取主题最新的路由。

消息生产者以每 30s 的频率去拉取主题的路由信息，即消息生产者并不会立即感知 Broker 服务器的新增和删除。

这个问题，可以通过客户端重试机制来解决。

### 路由元信息

```go
private final ReadWriteLock lock = new ReentrantReadWriteLock();

private final HashMap<String/* topic */, List<QueueData>> topicQueueTable;

private final HashMap<String/* brokerName */, BrokerData> brokerAddrTable;

private final HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable;

private final HashMap<String/* brokerAddr */, BrokerLiveInfo> brokerLiveTable;

private final HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable;
```

* topicQueueTable：topic 路由信息(消息队列信息)，包括topic所在的Broker名称，读队列数量，写队列数量，同步标记等信息，RocketMQ根据topicQueueTable的信息进行负载均衡消息发送。
* brokerAddrTable：Broker节点信息，包括BrokerName，所在集群名称，还有主备节点信息。
* clusterAddrTable：Broker集群信息，存储了集群中所有的Brokername
* brokerLiveTable：Broker状态信息，Nameserver每次收到Broker的心跳包就会更新该信息

----

## 问题

### NameServer 的职责？

为了避免消息服务器 broker 的单点故障导致整个系统瘫痪，通常会部署多台消息服务器共同承担消息的存储。那消息生产者如何知道消息要发往哪台消息服务器呢？如果某一台消息服务器宕机了，那么生产者如何在不重启服务的情况下感知呢？ NameServer 就是为了解决上述问题而设计的。

### 路由信息使用读写锁的原因？

路由表（ HashTable ）使用了锁粒度较少的读写锁，允许多个消息发送者（Producer）并发读，保证消息发送时的高并发。但同一时刻 NameServer 只处理一个 Broker 心跳包，多个心跳包请求串行执行。这也是读写锁经典使用场景，更多关于读写锁的信息

### RocketMQ 采用拉模式获取 Topic 路由信息有什么缺点？

1. Topic 路由中心（NameServer）Topic 是基于最终一致性，极端情况下会出现数据不一致。
2. 客户端无法实时感知路由信息的变化，例如某台消息存储 Brocker 自身进程为关闭，但停止向 NameServer 发送心跳包，但生产者无法立即感知该Brocker服务器的异常，会对消息发送造成一定的影响。

RocketMQ 并不打算解决上述问题，因为基于上述的设计，RocketMQ NameServer 的实现非常简单高效，至于消息发送的高可用，则有消息发送客户端自己保证。

RocketMQ 的设计遵循的一个设计理念：崇尚“缺陷美”，简单，高性能。

### NameServer 的路由变化不会马上通知消息生产者，为什么要这样设计呢？

这是为了降低 NameServer 实现的复杂性，在消息发送端提供容错机制来保证消息发送的高可用性

### 如何避免 NameServer 的单点故障，提供高可用？

NameServer 本身的高可用可通过部署多台 NameServer 服务器来实现，但彼此之间互不通信，也就是 NameServer 服务器之间在某一时刻的数据并不会完全相同，但这对消息发送不会造成任何影响，这也是 RocketMQ NameServer 设计的一个亮点， RocketMQ NameServer 设计追求简单高效。

### NameServer 数据不一致有什么问题？

NameServer 集群中的多个实例，彼此之间是不通信的，这意味着某一时刻，不同实例上维护的元数据可能是不同的，客户端获取到的数据也可能是不一致的。

### 为什么使用 nameServer，而不用 zookeeper ?
