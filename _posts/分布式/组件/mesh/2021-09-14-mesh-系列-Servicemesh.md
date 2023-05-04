---
layout: post
title: "mesh 系列 Service Mesh概述"
subtitle: 'Servicemesh概述...'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - mesh 
---

> Service Mesh：微服务的网络通信基础设施，负责（系统内部的）服务间的通讯

## 第一个视角

#### 起因
**微服务提供的核心好处是：**不同功能模块可以作为独立服务存在，独立的DB、缓存，服务之间通过约定的方式通信，大大地降低了耦合，可以各自单独运维管理，提供了资源和故障的隔离，提升了研发效率。

**这种好处伴随着一个明显的代价：**不同服务之间的异质化加剧，每个服务可以使用不同的编程语言编写，既是便利也是负担。

#### 问题
当一个问题需要使用一个 Library/SDK 去解决的时候，势必因此遇到2个问题：
1. 不同编程语言需要将问题重复解决一遍，过程中难免产生细微实现的差异，因为语言特性局限或者因为bug，维护的成本十分高。
2. 由于不同服务可以独立运维部署，研发和发布的周期各不同，Library/SDK的版本分裂严重，依赖特定版本在全局推进的功能，推广速度十分缓慢。不同版本的兼容性维护存在风险，成本高昂。

会能够遭遇这个问题的组件包括但不限于：微服务/RPC框架、DB缓存类中间件的SDK(MySQL/redis等等)、风控SDK、AB Testing SDK
#### 解决方案

这个问题来自微服务，也终结于微服务。首先面临这个问题最严重的是微服务框架。所以Service Mesh的架构应运而生，解决思路如下：

**将Library移动到外部作为一个独立进程存在，主业务进程通过IPC跟新的进程通信。（进程间通信）**

这个思路带来了如下变化：
1. 独立进程因为独立存在，可以使用单一编程语言实现。（解决多语言问题）
2. 独立进程因为独立存在，在IPC兼容的基础上，可以不侵入业务独立升级。（解决版本分裂问题）
3. 旧有的SDK可以用于维持简单IPC方式的约定，可以变得十分轻量。或者SDK可以彻底消灭，仅提供简单约定或者通过劫持请求的方式透明迁移。（多语言问题的补充说明）

## 第二个视角
#### 从边车模式到 Service Mesh

> 所谓边车模式（ Sidecar pattern ），也译作挎斗模式，是分布式架构中云设计模式的一种。边车模式通过给应用服务加装一个“边车”来达到控制和逻辑的分离的目的。比如日志记录、监控、流量控制、服务注册、服务发现、服务限流、服务熔断等在业务服务中不需要实现的控制面功能，可以交给“边车”，业务服务只需要专注实现业务逻辑即可。

边车模式有效的分离了系统控制和业务逻辑，可以将所有的服务进行统一管理，让开发人员更专注于业务开发，显著的提升开发效率。而遵循这种模式进行实践从很早以前就开始了，开发人员一直试图将通用功能（如：流量控制、服务注册、服务发现、服务限流、服务熔断等）提取成一个标准化的 Sidecar ，通过 Sidecar 代理来与其他系统进行交互，这样可以大大简化业务开发和运维。而随着分布式架构和微服务被越来越多的公司和开发者接受并使用，这一需求日益凸显。

这就是 Service Mesh 服务网格诞生的契机，它是 CNCF（Cloud Native Computing Foundation，云原生基金会）目前主推的新一代微服务架构。 William Morgan 在 [What’s a service mesh? And why do I need one? ](https://buoyant.io/2017/04/25/whats-a-service-mesh-and-why-do-i-need-one/)中解释了什么是 Service Mesh 。

Service Mesh 有如下几个特点：
- 应用程序间通讯的中间层
- 轻量级网络代理
- 应用程序无感知
- 解耦应用程序的重试/超时、监控、追踪和服务发现

Service Mesh 将底层那些难以控制的网络通讯统一管理，诸如：流量管控，丢包重试，访问控制等。而上层的应用层协议只需关心业务逻辑即可。Service Mesh 是一个用于处理服务间通信的基础设施层，它负责为构建复杂的云原生应用传递可靠的网络请求。

## 参考文献

[Sidecar pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/sidecar)

[从边车模式到 Service Mesh](https://www.servicemesher.com/blog/from-sidecar-to-servicemesh/)

[Do I Need a Service Mesh? ](https://www.nginx.com/blog/do-i-need-a-service-mesh/)