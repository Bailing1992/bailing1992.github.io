---
layout: post
title: "RocksDB"
subtitle: '分析 高可用的分布式 KV 存储'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - nosql 
---

RocksDB 是 facebook 基于 LevelDB 开发的单机存储引擎，主要开发语言为 C++。

## LSM

LSM Tree，即 Log-Structured Merge-Tree，它不是一个数据结构，而是一种数据存储引擎的设计思想和算法，主要为了提升写多读少场景下的性能。

LSM Tree提升写性能的关键点在于将所有的写操作都变成顺序写，避免随机 IO。核心思想就是异步对数据做合并、压缩，维护数据的有序性。

## 整体架构

![rocketdb](/img/post/store/rocketdb_overview.png)

### Memtable

数据在内存中的结构，分为memtable和immutable memtable，其中memtable为读写结构，当memtable增长到一定大小的时候就会变成immutable memtable，只读不写；memtable 和 immutable memtable 的数据结构是一样的，其中LevelDB只支持一个immutable memtable，RocksDB在此基础上做了优化，支持多个immutable memtable，提高写入性能。

![memtable](/img/post/store/memtable.png)

RockDB 默认使用跳表作为 memtable 的数据结构，跳表在读、写、范围查询、支持无锁并发方面都有较好的性能
![memtable](/img/post/store/skiplist.png)

### WAL log

RocksDB 写入一个 Record 时，都会先向日志里写入一条记录，这种日志一般称为 Write Ahead Log，类似于 MySQL 的 Redo Log。这种日志最大的作用就是将对磁盘的随机写转换成了顺序写。当故障宕机时，可以通过 Write Ahead Log 进行故障恢复。控制每次写入磁盘的方式，可以控制最多可能丢失的数据量。如果全部数据刷入 SST File，Write Ahead Log 文件就会删除

**故障恢复级别：**

故障恢复级别定义了 RocksDB 故障重启以后，对未提交的数据的恢复策略。故障重启以后，日志文件可能不完整，读取的时候会产生错误，故障恢复级别就是针对这些错误所采取的不同策略。

- **kTolerateCorruptedTailRecords**：忽略一些在末尾写入失败的请求，数据异常仅限于log文件末尾写入失败。如果出现了其他的异常，都无法进行数据重放
- **kAbsoluteConsistency**：对一致性要求最高的级别，不允许有任何的 IO 错误，不能出现一个record的丢失
- **kPointInTimeRecovery**：默认的 mode，当遇到 IO 错误的时候会停止重放，将出现异常之前的所有数据进行完成重放
- **kSkipAnyCorruptedRecords**：一致性要求最低的，会忽略所有的 IO error，尝试尽可能多得恢复数据

### SST File

Sorted String Table File，数据按照key有序存储后，在磁盘上最终存储为SST File，通过Compaction来维护每层数据，其中最下面一层存储全量数据。

**产生的问题：**

- *读放大：*数据可能存在Memtable，immutable memtable，以及多层SST File中，所以一次读可能放大为多次数据查找（多层SST File的数据是存在重叠冗余的）；
- *写放大：*写放大主要是由Compaction引入的；
- *空间放大：*数据在多层之间是冗余的，同时WAL日志也会占用一定的空间；
