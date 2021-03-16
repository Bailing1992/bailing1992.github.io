---
layout: post
title: "网络 系列 UDP"
subtitle: '开启 网络 探索新篇章'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - network 
---

> UDP -> 面向无连接的，具有不可靠性的数据报协议。（让广播和细节控制交给应用的通信传输）

> UDP 主要用于那些对高速传输和实时性有较高要求的通信或广播通信。

> UDP(User Datagram Protocol)不提供复杂的控制机制，利用 IP 提供面向无连接的通信服务。因此，它不会负责：流量控制、丢包重发等。

UDP 广泛应用于：
* 包量较少的通信（DNS、SNMP登）
* 视频、音频等多媒体通信（即时通信）
* 限定于LAN等特定网络中的应用通信
* 广播通信（广播、多播）　　


