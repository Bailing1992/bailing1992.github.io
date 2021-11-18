---
layout: post
title: "网络 系列 CNAME"
subtitle: '开启 网络 探索新篇章'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - network 
---

真实名称记录（Canonical Name Record），即 CNAME 记录，是域名系统（DNS）的一种记录。CNAME 记录用于将一个域名（同名）映射到另一个域名（真实名称），域名解析服务器遇到 CNAME 记录会以映射到的目标重新开始查询。

这对于需要在同一个 IP 地址上运行多个服务的情况来说非常方便。若要同时运行文件传输服务和 Web 服务，则可以把 ftp.example.com 和 www.example.com 都指向 DNS 记录 example.com，而后者则有一个指向 IP 地址的 A 记录。如此一来，若服务器IP地址改变，则只需修改 example.com 的 A 记录即可。

CNAME 记录必须指向另一个域名，而不能是 IP 地址。


## 参考文献
https://zh.wikipedia.org/wiki/CNAME%E8%AE%B0%E5%BD%95