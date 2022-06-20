---
layout: post
title: "网络 系列 WebSocket"
subtitle: 'WebSocket 的出现，使得浏览器具备了实时双向通信的能力。'
author: "lichao"
header-img: "img/post/bg/post-bg-web.jpg"
catalog: true
tags:
  - network 
---

WebSocket 设计出来的目的就是要取代轮询和 Comet技术，使客户端浏览器具备像 C/S架构下桌面系统的实时通讯能力。浏览器通过 JavaScript 向服务器发出建立 WebSocket连接的请求，连接建立以后，客户端和服务器端就可以通过 TCP 连接直接交换数据。因为 WebSocket 连接本质上就是一个 TCP 连接，所以在数据传输的稳定性和数据传输量的大小方面，和轮询以及 Comet 技术比较，具有很大的性能优势。


> WebSocket 与 HTTP的对比：
> 1. 支持双向通信，实时性更强。
> 2. 更好的二进制支持。
> 3. 较少的控制开销。连接创建后，WebSocket 客户端、服务端进行数据交换时，协议控制的数据包头部较小。在不包含头部的情况下，服务端到客户端的包头只有2~10字节（取决于数据包长度），客户端到服务端的的话，需要加上额外的4字节的掩码。而HTTP协议每次通信都需要携带完整的头部。
> 4. 支持扩展。WebSocket协议定义了扩展，用户可以扩展协议，或者实现自定义的子协议。（比如支持自定义压缩算法等）

## 建立连接
WebSocket 复用了 HTTP 的握手通道。具体指的是，客户端通过 HTTP 请求与 WebSocket 服务端协商升级协议。协议升级完成后，后续的数据交换则遵照WebSocket的协议。


## 参考文献
[](https://blog.csdn.net/liuyez123/article/details/50510579)



