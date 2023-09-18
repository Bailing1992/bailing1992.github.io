---
layout: post
title: "mesh 系列 Service Mesh和API Gateway之间的关系"
subtitle: 'Tomcat...'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - mesh 
---


首先，Service Mesh 和 API Gateway 在功能定位和承担的职责上有非常清晰的界限：

* Service Mesh：微服务的网络通信基础设施，负责（系统内部的）服务间的通讯
* API Gateway： 负责将服务以 API 的形式暴露（给系统外部），以实现业务功能

![servicemesh和apigatewat的关系](/img/distributed/mesh/servicemesh和apigatewat的关系.png)

从功能和职责上说：

* 位于最底层的是拆分好的原子微服务，以服务的形式提供各种能力。
* 在原子微服务上是（可选的）组合服务，某些场景下需要将若干微服务的能力组合起来形成新的服务
* 原子微服务和组合服务部署于 系统内部，在采用servicemesh的情况下，由servicemesh提供服务间通讯的能力
* API Gateway用于将系统内部的这些服务暴露给 系统外部，以API的形式接受外部请求。

从部署上说：

* Servicemesh 部署在系统内部：因为原子微服务和组合服务通常不会直接暴露给外部系统。
* API Gateway 部署在系统的边缘：一方面暴露在系统之外，对外提供API供外部系统访问；一方面部署在系统内部，以访问内部的各种服务。
  
![南北向和东西向](/img/distributed/mesh/南北向和东西向.png)

* 东西向通讯：指服务间的相互访问，其通讯流量在服务间流转，流量都位于系统内部。
* 南北向通讯：指服务对外部提供访问，通常是通过 API Gateway 提供的 API 对外部提供，其通讯流量是从系统外部进入系统内部。

## 网关访问内部服务，算东西向还是南北向？

如下图所示，图中黄色的线条表示的是 API Gateway 访问内部服务：
![apigatewat访问内部服务](/img/distributed/mesh/apigatewat访问内部服务.png)
问题来了，从流量走向看：这是外部流量进入系统后，开始访问对外暴露的服务，应该属于“南北向”通讯，典型如上图的画法。但从另外一个角度，如果我们将 API Gateway 逻辑上拆分为两个部分，先忽略对外暴露的部分，单独只看 API Gateway 访问内部服务的部分，这时可以视 API Gateway 为一个普通的客户端服务，它和内部服务的通讯更像是“东西向”通讯：
![apigatewat访问内部服务](/img/distributed/mesh/apigatewat访问内部服务2.png)
这个哲学问题并非无厘头，在 API Gateway 的各种产品中，关于如何实现 “API Gateway 作为一个客户端访问内部服务” ，就通常分成两个流派：

* 泾渭分明：视 API Gateway 和内部服务为两个独立事物，API Gateway访问内部服务的通讯机制自行实现，独立于服务间通讯的机制
* 兼容并济：视 API Gateway 为一个普通的内部服务的客户端，重用其内部服务间通讯的机制。

而最终决策通常也和产品的定位有关：如果希望维持 API Gateway 的独立产品定位，希望可以在不同的服务间通讯方案下都可以使用，则通常选择前者，典型如 kong；如果和服务间通讯方案有非常深的渊源，则通常选择后者，典型如 springcloud 生态下的 zuul 和 springcloud gateway。

但无论选择哪个流派，都改变不了一个事实，当 “API Gateway 作为一个客户端访问内部服务” 时，它的确和一个普通内部服务作为客户端去访问其他服务没有本质差异：服务发现，负载均衡，流量路由，熔断，限流，服务降级，故障注入，日志，监控，链路追踪，访问控制，加密，身份认证…… 当我们把网关访问内部服务的功能一一列出来时，发现几乎所有的这些功能都是和服务间调用重复。

这也就造成了一个普遍现象：如果已有一个成熟的服务间通讯框架，再去考虑实现API Gateway，重用这些重复的能力就成为自然而然的选择。典型如前面提到的 springcloud 生态下的 zuul 以及后面开发的 springcloud gateway，就是以重用类库的方式实现了这些能力的重用。

## Sidecar：真正的重合点

融合东西向和南北向的通讯方案中，一个做法就是基于 Servicemesh 的 Sidecar 来实现 API Gateway，从而在南北向通讯中引入 Servicemesh 这种东西向通讯的方案。
![sidecar](/img/distributed/mesh/sidecar.png)
因为 servicemesh 中 sidecar 的引入，所以前面的“哲学问题”又有了一个新的解法：API Gateway 这次真的可以分拆为两个独立部署的物理实体，而不是逻辑上的两个部分：

* API Gateway本体：实现 API Gateway 除了访问内部服务之外的功能
* Sidecar：按照 servicemesh 的标准做法， 我们视 API Gateway 为一个部署于 servicemesh 中的普通服务，为这个服务1:1的部署sidecar
![sidecar](/img/distributed/mesh/sidecar2.png)
在这个方案中，原来用于 servicemesh 的 sidecar，被用在了 API Gateway 中，替代了 API Gateway 中原有的客户端访问的各种功能。这个方案让 API Gateway 的实现简化了很多，也实现了东西向和南北向通讯能力的重用和融合，而 API Gateway可以更专注于 “API Management” 的核心功能。

上述方案的优势在于API Gateway和Sidecar独立部署，职责明确，架构清晰。但是，和servicemesh使用sidecar被质疑多一跳会造成性能开销影响效率一样，API Gateway使用Sidecar也被同样的质疑：多了一跳……

解决“多一跳”问题的方法简单而粗暴，基于sidecar，将API Gateway的功能加进来。这样API Gateway本体和Sidecar再次合二为一：
![sidecar](/img/distributed/mesh/sidecar3.png)

## 参考文献

[相爱相杀：Servicemesh和API Gateway关系深度探讨](https://skyao.io/post/202004-servicemesh-and-api-gateway/)
