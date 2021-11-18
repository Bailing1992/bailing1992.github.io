---
layout: post
title: "mesh 系列 API Gateway概述"
subtitle: 'Servicemesh概述...'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - mesh 
---

> API Gateway 负责将服务以 API 的形式暴露（给系统外部），以实现业务功能，比如阿里 API 网关。另一个趋势是网关正由中心化向去中心化演进，比如蚂蚁 Mesh 网关。

现存在 2 种不同的网关产品，分别是中心化网关和分布式(sidecar)网关。从网关能力演进的角度来看，分布式网关是未来的趋势，在业界API Gateway Mesh越来越成为主流，尤其是在性能方面表现卓越，如蚂蚁金服的实践，阿里采用分布式网关，撑住了双 11 流量洪峰。同时，在分布式网关在也解决了中心化网关大促容量评估难、升级难等问题。

- 中心化网关，流量路径从负载均衡(Nginx)到网关集群，再到业务集群。此架构的优势是在AGW集群作为集中的网关节点，拥有非常灵活的流量调度能力，如集群分流、机房调度等。
- 分布式网关，网关以独立的进程部署在业务实例中，流量从负载均衡(Nginx)直接到业务集群，通过网关转发到业务实例。此架构的优势是流量路径中少了一跳，因此整体时延上收益明显，同时分布式网关以 sidecar 模式独立进程部署在业务实例中，业务完全隔离，隔离性以及稳定性会更好。

![中心化和分布式网关](/img/distributed/mesh/中心化和分布式网关.png)

上图展示的网关同时支持thrift服务的http/rpc入流量。
- agw sidecar 以 mesh sidecar 方式接入运行，对 http 和 thrift 协议框架的服务都是支持的。
- mesh 支持同时开启http、rpc入流量，具备协议嗅探能力来实现不同协议入流量的转发。
- http ingress/rpc ingress(mesh)定位主要对服务间底层通信治理。agw sidecar可以理解是更上层的接口抽象，包括请求响应的处理、应用层协议转换等。

## 中心化网关
优势：
- 支持灵活的流量调度
- 支持请求合并
  
劣势：
- 网关更变风险：网关的逻辑变更发布一旦有问题，将会影响所有业务
- 业务分级隔离：核心业务的 API 希望和非核心业务的接口做资源上隔离
- 资源评估容易：活动期间上万 API 接口的 QPS 很难评估，不同 API 的 RT、BodySize、QPS 对于网关性能的影响都是不同的，为了网关入口的稳定性，一般情况下，都会疯狂的扩容
- 成本分摊难

## 分布式网关
优势：
- 稳定性好：由于 sidecar 网关以独立进程部署在业务实例中，业务间完全隔离，配置逻辑变更，业务之间不会相互影响
- 接入成本低：无需 agw 独立集群等部署
- 资源评估容易：sidecar 网关以独立进程部署在业务实例中，业务方可以通过评估业务实例来进行精准预估网关资源
- 易用性高：sidecar 网关在业务侧可自行操作扩容，对比中心化网关需在 agw 平台操作，缩短了操作链路，提高效率

劣势：
- 无法实现灵活的流量调度
- 无法实现请求合并



## 参考文献
[阿里技术专家：“双11”亿级流量背后的API网关、微服务架构实践！](https://mp.weixin.qq.com/s?__biz=MzA5MjE3NDQ1Mw==&mid=2649704551&idx=3&sn=0b84432e5b854d7a803fb6b0b7023c01&chksm=886ac97dbf1d406bd233a651df547ff7a043f5d3116a4978ceefa32d2cc608e28eb3b0f60f33&mpshare=1&scene=1&srcid=&sharer_sharetime=1573533637037&sharer_shareid=0c4375dcbf29d7a097022333def16a5e#rd)

[蚂蚁金服 Service Mesh 大规模落地系列 - 网关篇](https://www.sofastack.tech/blog/service-mesh-practice-in-production-at-ant-financial-part5-gateway/)

