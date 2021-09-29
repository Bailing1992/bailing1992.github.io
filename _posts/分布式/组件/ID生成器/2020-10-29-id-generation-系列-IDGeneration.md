---
layout: post
title: "分布式 ID 生成器"
subtitle: '分析分布式 ID 生成器'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - distribute 
---

目前主流的分布式id生成方案都有第三方组件依赖，如：基于zk、基于mysql、基于缓存 


twitter的snowflake算法是一个完全去中心化的分布式id算法，但是限制workid最多能有1024，也就是说，应用规模不能超过1024。虽然可以进行细微的调整，但是总是有数量的限制。