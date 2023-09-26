---
layout: post
title: "MySQL 系列 bin 日志"
subtitle: 'MySQL 技术内幕：InnoDB存储引擎'
author: "lichao"
header-img: "img/post/bg/mac.jpg"
catalog: true
tags:
  - mysql
---

MySQL 归档日志（binlog）是二进制日志，主要记录所有数据库表结构变更（例如CREATE、ALTER TABLE…）以及表数据修改（INSERT、UPDATE、DELETE …）的所有操作。二进制日志（binary log）中记录了对 MySQL 数据库执行更改的所有操作，并且记录了语句发生时间、执行时长、操作数据等其它额外信息，但是它不记录 SELECT、SHOW 等那些不修改数据的 SQL 语句。

默认情况下，二进制日志并不是在每次写的时候同步到磁盘。因此，当数据库所在地操作系统发生宕机时，可能会有最后一部分数据没有写入二进制日志文件中，这会给恢复和复制带来问题。当使用事务的表储存引擎时，所有未提交的二进制日志会被记录到一个缓存中，等该事务提交时直接将缓冲中的二进制日志写入二进制日志文件。

## 作用

* 恢复：某些数据的恢复需要二进制日志，例如，在一个数据库全备文件恢复后，用户可通过二进制日志进行即时点（```point-in-time```）恢复。
* 主从复制：通过复制和执行二进制日志使一台远程的 Mysql 数据库（一般称为 slave）与一台 MySQL 数据库（一般称为 master）进行实时同步。
* 审计：用户可以通过二进制日志中的信息来进行审计，判断是否对数据库进行了注入的攻击。

除了上面介绍的几个作用外，binlog 对于事务存储引擎的崩溃恢复也有非常重要的作用。在开启 binlog 的情况下，为了保证 binlog 与 redo 的一致性，MySQL 将采用事务的两阶段提交协议。当 MySQL 系统发生崩溃时，事务在存储引擎内部的状态可能为 prepared 和 commit 两种。对于 prepared 状态的事务，是进行提交操作还是进行回滚操作，这时需要参考 binlog：如果事务在 binlog 中存在，那么将其提交；如果不在 binlog 中存在，那么将其回滚，这样就保证了数据在主库和从库之间的一致性。

二进制日志记录的是逻辑性的语句。即便它是基于行格式的记录方式，其本质也还是逻辑的 SQL 设置，如该行记录的每列的值是多少。

二进制日志只在每次事务提交的时候一次性写入缓存中的日志文件，所以二进制日志中的记录方式和提交顺序有关，且一次提交对应一次记录。

## 写入逻辑

binlog 写入逻辑比较简单：事务执行过程中，先把日志写到 binlog cache，事务提交的时候，再把 binlog cache 写到 binlog 文件中。
一个事务的 binlog 是不能被拆开的，因此不论这个事务多大，也要确保一次性写入。这就涉及到了 binlog cache 的保存问题。
系统给 binlog cache 分配了一片内存，每个线程一个，参数 ```binlog_cache_size``` 用于控制单个线程内 binlog cache 所占内存的大小。如果超过了这个参数规定的大小，就要暂存到磁盘。

事务提交的时候，执行器把 binlog cache 里的完整事务写入到 binlog 中，并清空 binlog cache。状态如下图所示。
![binlog存储过程](/img/mysql/binlog存储过程.png){:height="80%" width="80%"}

可以看到，每个线程有自己 binlog cache，但是共用同一份 binlog 文件。

图中的 write，指的就是指把日志写入到文件系统的 page cache，并没有把数据持久化到磁盘，所以速度比较快。
图中的 fsync，才是将数据持久化到磁盘的操作。一般情况下认为 fsync 才占磁盘的 IOPS。

write 和 fsync 的时机，是由参数 sync_binlog 控制的：

* sync_binlog=0 的时候，表示每次提交事务都只 write，不 fsync；
* sync_binlog=1 的时候，表示每次提交事务都会执行 fsync；
* sync_binlog=N(N>1) 的时候，表示每次提交事务都 write，但累积 N 个事务后才 fsync。

因此，在出现 IO 瓶颈的场景里，将 sync_binlog 设置成一个比较大的值，可以提升性能。在实际的业务场景中，考虑到丢失日志量的可控性，一般不建议将这个参数设成 0，比较常见的是将其设置为 100~1000 中的某个数值。

但是，将 sync_binlog 设置为N，对应的风险是：如果主机发生异常重启，会丢失最近 N 个事务的 binlog 日志。

## sync_binlog 参数配置

0：刷新binlog_cache中的信息到磁盘由 os 决定。
N：每 N 次事务提交刷新 binlog_cache 中的信息到磁盘。
