---
layout: post
title: "分布式协同服务 系列 Zookeeper常用命令"
subtitle: '开启 Zookeeper 探索新篇章'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - zookeeper 
---

## 部署

### 启动

```shall
./zkServer.sh start ../conf/zoo1.cfg
./zkServer.sh start ../conf/zoo2.cfg
./zkServer.sh start ../conf/zoo3.cfg
```

#### 停止

```shall
./zkServer.sh stop ../conf/zoo1.cfg
./zkServer.sh stop ../conf/zoo2.cfg
./zkServer.sh stop ../conf/zoo3.cfg
```

#### 查看状态

```shall
./zkServer.sh status ../conf/zoo1.cfg
./zkServer.sh status ../conf/zoo2.cfg
./zkServer.sh status ../conf/zoo3.cfg
```

#### 登录服务器

```shall
./zkCli.sh -server localhost:2181
```

## 命令

ZooKeeper 命令行工具 类似于 Linux 的 shell 环境，但是使用它我们可以简单的对 ZooKeeper 进行访问，数据创建，数据修改等操作。连接到 ZooKeeper 服务，连接成功后，系统会输出 ZooKeeper 的相关环境以及配置信息。

### 创建节点

```shall
create

[zk: 127.0.0.1:2181(CONNECTED) 5] create /zk "test"
Created /zk
[zk: 127.0.0.1:2181(CONNECTED) 20] create /zk/test "test"
Created /zk/test
不支持递归创建，必须先创建父节点
[zk: 127.0.0.1:2181(CONNECTED) 29] create /test/node "node1"
Node does not exist: /test/node
只能创建一级目录的节点，多级时，必须一级一级创建
create /zk null
create /zk/test1 null
节点不能以 / 结尾，会直接报错
[zk: 127.0.0.1:2181(CONNECTED) 60] create /zk/test2/ null
Command failed: java.lang.IllegalArgumentException: Path must not end with / character
```

### 查看节点下的信息

```shall
ls /zk

[zk: 127.0.0.1:2181(CONNECTED) 62] ls /zk
[test1, test2]
```

列出节点下的节点集合和更详细的信息:

```shall
ls2 /zk

[zk: 127.0.0.1:2181(CONNECTED) 63] ls2 /zk
[test1, test2]
cZxid = 0x600000054
ctime = Thu Jan 12 14:27:54 CST 2017
mZxid = 0x600000054
mtime = Thu Jan 12 14:27:54 CST 2017
pZxid = 0x600000059
cversion = 2
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 4
numChildren = 2
```

### 获取节点数据

```shall
[zk: 127.0.0.1:2181(CONNECTED) 42] get /test1
"{aaa}"

cZxid = 0x600000051
ctime = Thu Jan 12 14:23:37 CST 2017
mZxid = 0x600000051
mtime = Thu Jan 12 14:23:37 CST 2017
pZxid = 0x600000051
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 7
numChildren = 0
```

### 给节点赋值

```shall
[zk: 127.0.0.1:2181(CONNECTED) 43] set /test1 "{bbb}"

cZxid = 0x600000051
ctime = Thu Jan 12 14:23:37 CST 2017
mZxid = 0x600000052
mtime = Thu Jan 12 14:24:13 CST 2017
pZxid = 0x600000051
cversion = 0
dataVersion = 1
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 7
numChildren = 0
```

### 删除节点

```shall
delete /test1

节点不为空不能删除

[zk: 127.0.0.1:2181(CONNECTED) 58] delete /zk
Node not empty: /zk

删除时，须先清空节点下的内容，才能删除节点

delete /zk/test1
delete /zk/test2
delete /zk
```

## 代码实现

<https://zhuanlan.zhihu.com/p/80221503>
