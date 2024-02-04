---
layout: post
title: "Mongo WiredTiger存储引擎"
subtitle: 'WiredTiger存储引擎介绍...'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - mongo 
---
 
从 MongoDB 3.2 开始，WiredTiger 存储引擎开始作为默认的存储引擎。WiredTiger 使用文档级并发控制进行写操作，使用 MultiVersion 并发控制（MVCC）方式。

![BSON](/img/post/store/mongo/整体架构.PNG)

> **In-Memory**
> In-Memory 存储引擎将数据存储在内存中，除了少量的元数据和诊断（Diagnostic）日志，In-Memory存储引擎不会维护任何存储在硬盘上的数据（On-Disk Data），避免Disk的IO操作，减少数据查询的延迟。
>
> **MMAPv1**:
> 从MongoDB 4.2开始，MongoDB移除了MMAPv1存储引擎。并发级别上，MMAPv1支持到collection级别，所以对于同一个collection同时只能有一个write操作执行，这一点相对于wiredTiger而言，在write并发性上就稍弱一些。

## WiredTiger 特性&架构概览

![BSON](/img/post/store/mongo/WiredTiger特性.PNG)

- Row Storage & Column Storage：基于BTree的行存/列存；
- cache 模块：内存数据缓存，主要由内存中的 btree page(数据页，索引页，溢出页)构成；
- block management 模块：磁盘文件、空间管理，负责磁盘 IO 的读写，cache、evict、checkpoint 模块均通过该模块访问磁盘；
- Transaction & Snapshots：事务；
- Schema & Cursors：Connection,Session, Cursor等；

## 数据模型

![BSON](/img/post/store/mongo/数据模型.PNG)

- MongoDB 的 1 个 ```collection/index```，对应 WiredTiger 的一个 ```b+tree```；
- ```collection/index``` 中的每个 document，对应 WiredTiger b+tree leaf page 的一个 KV；Key（collection的键值）构成 internal page，即 ```b+tree``` 索引。
- WiredTiger 在内存 Cache 和磁盘上，都用 ```b+tree``` 来组织 KV。每个 ```b+tree``` 节点为一个 page，root page是 ```b+tree``` 的根节点，internal page 是 ```b+tree``` 的中间索引节点，leaf page 是真正存储数据的叶子节点。Cache中```b+tree```的数据以 page 为单位按需从磁盘加载或写入磁盘。
- WiredTiger 在磁盘上的数据组织方式和内存中是不同的，在磁盘上的数据一般是经过压缩以节省空间和读取时的 IO 开销。当从磁盘向内存中读取数据时，一般会经过解压缩、将数据重新构建为内存中的数据组织方式等步骤。
- 持久化时，修改操作不会在原来的 leaf page 上进行，而是写入新分配的 page，每次 checkpoint 都会产生一个新的 root page。 这样的好处是对不修改原有 page，就能更好的并发。

### Cache

![BSON](/img/post/store/mongo/cache_b+tree.png)

- Cache中的BTree包含全部或部分磁盘的数据page
- 存在In-Memory Page与On-Disk Page的换入换出
  - 如果需要读取的page不在Cache中，从磁盘文件加载到Cache，成为clean page；
  - 如果Cache占用内存过高时，clean page会被直接淘汰出cache；
  - 如果clean page被修改写入，变成dirty page，dirty page会被evict/checkpoint刷到data file

上图是 page 在内存中的数据结构，是一个典型的 ```b+tree```，每个叶节点的 page 上有 3 个重要的 list：```WT_ROW```、```WT_UPDATE```、```WT_INSERT```:

- 内存中的 ```b+tree``` 树：是一个checkpoint。初次加载时并不会将整个 ```b+tree``` 树加载到内存，一般只会加载根节点和第一层的数据页，后续如果需要读取数据页再从磁盘加载
- 叶节点Page的 WT_ROW：是从磁盘加载进来的数据数组
- 叶节点Page的 WT_UPDATE：是记录数据加载之后到下个checkpoint之间，该叶节点中被修改的数据
- 叶节点Page的 WT_INSERT：是记录数据加载之后到下个checkpoint之间，该叶节点中新增的数据
