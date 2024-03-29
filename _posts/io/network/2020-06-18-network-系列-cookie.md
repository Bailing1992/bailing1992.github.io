---
layout: post
title: "网络 系列 Cookie"
subtitle: '开启 网络 探索新篇章'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - network 
---


cookie相关基础知识，可以逐步考察候选人或者有挑选的询问
cookie是什么？
为什么需要cookie？
cookie和session有什么区别？
cookie是怎么被种到浏览器中的？
cookie中包含哪些内容，其中的httpOnly具体含义是什么？
相关拓展知识：
如果浏览器开启了禁用cookie，要怎么处理？
如何解决session分布式存储问题？
<li class="ql-indent-1">问题描述：互联网公司往往都拥有着很大的流量，后端服务器都是部署多台来应对用户请求，那么如果用户在 A 服务器登录了，第二次请求跑到服务 B 就会出现登录失效问题，这种问题改如何解决呢？

> HTTP/1.1 虽然是无状态协议，但为了实现期望的保持状态功能，于是引入了 Cookie 技术。有了 Cookie 再用 HTTP 协议通信，就可以管理状态了

HTTP 是无状态协议，它不对之前发生过的请求和响应的状态进行管理。也就是说，无法根据之前的状态进行本次的请求处理。

假设要求登录认证的 Web 页面本身无法进行状态的管理（不记录已登录的状态），那么每次跳转新页面不是要再次登录，就是要在每次请求报文中附加参数来管理登录状态。

不可否认，无状态协议当然也有它的优点。由于不必保存状态，自然可减少服务器的 CPU 及内存资源的消耗。从另一侧面来说，也正是因为 HTTP 协议本身是非常简单的，所以才会被应用在各种场景里。

保留无状态协议这个特征的同时又要解决类似的矛盾问题，于是引入了 Cookie 技术。Cookie 技术通过在请求和响应报文中写入 Cookie 信息来控制客户端的状态。

Cookie 会根据从服务器端发送的响应报文内的一个叫做 Set-Cookie 的首部字段信息，通知客户端保存 Cookie。当下次客户端再往该服务器发送请求时，客户端会自动在请求报文中加入 Cookie 值后发送出
去。

服务器端发现客户端发送过来的 Cookie 后，会去检查究竟是从哪一个客户端发来的连接请求，然后对比服务器上的记录，最后得到之前的状态信息。