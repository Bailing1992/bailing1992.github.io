---
layout: post
title: "IO 系列 FileChannel 文件通道"
subtitle: '解析 IO...'
author: "lichao"
header-img: "img/post/bg/post-bg-2015.jpg"
catalog: true
tags:
  - io
---

```
FileChannel fileChannel = new RandomAccessFile(new File("data.txt"), "rw").getChannel()

```

* 全双工通道，可以同时读和写，采用内存缓冲区 ByteBuffer 且是线程安全的

