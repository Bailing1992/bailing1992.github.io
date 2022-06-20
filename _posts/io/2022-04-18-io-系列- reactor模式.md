---
layout: post
title: "IO 系列 Reactor模式"
subtitle: '解析 IO...'
author: "lichao"
header-img: "img/post/bg/post-bg-2015.jpg"
catalog: true
tags:
  - io
---



1. Reactor 主线程 MainReactor 对象通过 select 监听连接事件，收到事件后，通过 Acceptor 处理连接事件。
2. 当 Acceptor 处理连接事件后，MainReactor 将连接分配给 SubAcceptor。
3. SubAcceptor 将连接加入到连接队列进行监听，并创建 handler 进行各种事件处理。
4. 当有新事件发生时，SubAcceptor 就会调用对应的 handler 进行各种事件处理。
5. handler 通过 read 读取数据，分发给后面的 work 线程处理。
6. work 线程池分配独立的work线程进行业务处理，并返回结果。
7. handler 收到响应的结果后，再通过send返回给client。