---
layout: post
title: "Zookeeper 系列 常用命令"
subtitle: '开启 Zookeeper 探索新篇章'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - zookeeper 
---




#### 启动：
```
./zkServer.sh start ../conf/zoo1.cfg
./zkServer.sh start ../conf/zoo2.cfg
./zkServer.sh start ../conf/zoo3.cfg
```

#### 停止：
```
./zkServer.sh stop ../conf/zoo1.cfg
./zkServer.sh stop ../conf/zoo2.cfg
./zkServer.sh stop ../conf/zoo3.cfg
```
#### 查看状态：
```
/zkServer.sh status ../conf/zoo1.cfg
/zkServer.sh status ../conf/zoo2.cfg
/zkServer.sh status ../conf/zoo3.cfg
```