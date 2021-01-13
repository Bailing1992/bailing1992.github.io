---
layout: post
title: "消息中间件 系列 RocketMQ与Kafka的对比"
subtitle: '消息中间件基本问题'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - MQ
---

> 2011 年初， Linkin 开源了 Kafka 这个优秀的消息中间件，淘宝中间件团队在对 Kafka 做过充分 Review 之后，Kafka 无限消息堆积，高效的持久化速度吸引了我们，但是同时发现这个消息系统主要定位于日志传输，对于使用在淘宝交易、订单、充值等场景下还有诸多特性不满足，为此重新用 Java 语言编写了 RocketMQ，定位于非日志的可靠消息传输。

## 特征对比
#### 数据可靠性
* RocketMQ 支持异步刷盘，同步刷盘，同步复制，异步复制
* Kafka 使用异步刷盘方式，异步复制/同步复制

> 总结：RocketMQ 的同步刷盘在单机可靠性上比 Kafka 更高，不会因为操作系统 Crash，导致数据丢失。Kafka 同步 Replication 理论上性能低于 RocketMQ 的同步 Replication，原因是 Kafka 的数据以分区为单位组织，意味着一个 Kafka 实例上会​​有几百个数据分区，RocketMQ 一个实例上只有一个数据分区，RocketMQ 可以充分利用 IO 组 Commit 机制，批量传输数据，配置同步 Replication 与异步 Replication 相比，性能损耗约 20%~30%.

#### 性能对比

* Kafka 单机写入 TPS 约在百万条/秒，消息大小10个字节
* RocketMQ 单机写入 TPS: 单实例约 7 万条/秒，单机部署 3 个 Broker，可以跑到最高12万条/秒，消息大小10个字节

总结：Kafka的 TPS 跑到单机百万，主要是由于 Producer 端将多个小消息合并，批量发向 Broker。

> RocketMQ 为什么不支持 Producer 端将多个小消息合并？
1. Producer 通常使用的 Java 语言，缓存过多消息，GC 是个很严重的问题
2. Producer 调用发送消息接口，消息未发送到Broker，向业务返回成功，此时 Producer 宕机，会导致消息丢失，业务出错
3. Producer 通常为分布式系统，且每台机器都是多线程发送，我们认为线上的系统单个 Producer 每秒产生的数据量有限，不可能上万。
4. 缓存的功能完全可以由上层业务完成。

#### 单机支持的队列数
* Kafka单机超过 64 个队列/分区，Load 会发生明显的飙高现象，队列越多，load越高，发送消息响应时间变长。Kafka分区数无法过多的问题
* RocketMQ单机支持最高5万个队列，负载不会发生明显变化

> 队列多有什么好处？
1. 单机可以创建更多 topic，因为每个主题都是由一批队列组成
2. 消费者的集群规模和队列数成正比，队列越多，消费类集群可以越大

#### 消息投递实时性
* Kafka使用短轮询方式，实时性取决于轮询间隔时间，0.8以后版本支持长轮询。
* RocketMQ使用长轮询，同Push方式实时性一致，消息的投递延时通常在几个毫秒。

####  消费失败重试
* Kafka 消费失败不支持重试。
* RocketMQ 消费失败支持定时重试，每次重试间隔时间顺延


#### 严格的消息顺序
* Kafka 支持消息顺序，但是一台代理宕机后，就会产生消息乱序
* RocketMQ 支持严格的消息顺序，在顺序消息场景下，一台 Broker 宕机后，发送消息会失败，但是不会乱序
MySQL的二进制日志分发需要严格的消息顺序

#### 定时消息
* Kafka 不支持定时消息
* RocketMQ支持两类定时消息
  * 开源版本RocketMQ仅支持定时级别，定时级用户可定制
  * 阿里云MQ指定的毫秒级别的延时时间


#### 分布式事务消息
* Kafka 不支持分布式事务消息
* 阿里云MQ支持分布式事务消息，未来开源版本的RocketMQ也有计划支持分布式事务消息

#### 消息查询
* Kafka 不支持消息查询
* RocketMQ 支持根据消息标识查询消息，也支持根据消息内容查询消息（发送消息时指定一个消息密钥，任意字符串，例如指定为订单编号）

#### 消息回溯
* Kafka 理论上可以按照偏移来回溯消息
* RocketMQ 支持按照时间来回溯消息，精度毫秒，例如从一天之前的某时某分某秒开始重新消费消息

#### 消费并行度
* Kafka的消费并行度依赖Topic配置的分区数，如分区数为10，那么最多10台机器来并行消费（每台机器只能开启一个线程），或者一台机器消费（10个线程并行消费）。即消费并行度和分区数一致。

* RocketMQ消费并行度分两种情况
  * 顺序消费方式并行度同卡夫卡完全一致
  * 乱序方式并行度取决于Consumer的线程数，如Topic配置10个队列，10台机器消费，每台机器100个线程，那么并行度为1000。

#### 消息轨迹
* Kafka不支持消息轨迹
* 阿里云MQ支持消息轨迹

#### 开发语言友好性
* Kafka 采用斯卡拉编写
* RocketMQ采用的Java语言编写

#### 消息过滤
* 不支持代理端的消息过滤
* RocketMQ支持两种代理端消息过滤方式
  * 根据消息变量来过滤，相当于子主题概念
  * 向服务器上传一段Java代码，可以对消息做任意形式的过滤，甚至可以做Message身体的过滤拆分。

#### 消息堆积能力
理论上Kafka要比RocketMQ的堆积能力更强，不过RocketMQ单机也可以支持亿级的消息堆积能力，我们认为这个堆积能力已经完全可以满足业务需求。

## 架构设计对比
#### Namesrv VS zk
Kafka 通过 zookeeper 来进行协调，而rocketMq通过自身的namesrv进行协调。

RocketMQ 在协调节点的设计上显得更加轻量，用了另外一种方式解决高可用的问题，思路也是可以借鉴的。

> Kafka 具备选举功能，在Kafka里面，Master/Slave的选举，有2步：第1步，先通过ZK在所有机器中，选举出一个KafkaController；第2步，再由这个Controller，决定每个partition的Master是谁，Slave是谁。因为有了选举功能，所以kafka某个partition的master挂了，该partition对应的某个slave会升级为主对外提供服务。

> RocketMQ 不具备选举，Master/Slave 的角色也是固定的。当一个 Master 挂了之后，你可以写到其他 Master 上，但不能让一个 Slave 切换成 Master。那么 RocketMQ 是如何实现高可用的呢，其实很简单，RocketMQ 的所有broker节点的角色都是一样，上面分配的 topic 和对应的 queue 的数量也是一样的，Mq只能保证当一个broker挂了，把原本写到这个broker的请求迁移到其他 broker 上面，而并不是这个 broker 对应的 slave 升级为主。

#### 参考文献
[引用自](http://jm.taobao.org/2016/03/24/rmq-vs-kafka/)

[引用自](https://www.jianshu.com/p/c474ca9f9430)