---
layout: post
title: "MySQL 系列 redo 日志"
subtitle: 'MySQL 技术内幕：InnoDB存储引擎'
author: "lichao"
header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
  - MySQL
---

> Redo 为物理日志。Redo 记录的时机是 事务过程中。

Redo Log 包括两部分：一是内存中的日志缓冲(redo log buffer)，该部分日志是易失性的。二是磁盘上的重做日志文件(redo log file)，该部分日志是持久的，并且是事务的记录是顺序追加的，性能非常高(磁盘的顺序写性能比内存的写性能差不了太多)。

**InnoDB 使用事务日志来减少提交事务时的开销。**因为日志中已经记录了事务，就无须在每个事务提交时把缓冲池的脏块刷新(flush)到磁盘中。事务修改的数据和索引通常会映射到表空间的随机位置，所以刷新这些变更到磁盘需要很多随机IO。

InnoDB 用日志把随机 IO 变成顺序 IO。一旦日志安全写到磁盘，事务就持久化了，即使断电了，InnoDB 可以重放日志并且恢复已经提交的事务。

为了确保每次日志都能写入到事务日志文件中，在每次将 log buffer 中的日志写入日志文件的过程中都会调用一次操作系统的fsync操作(即fsync()系统调用)。因为MariaDB/MySQL是工作在用户空间的，MariaDB/MySQL的log buffer处于用户空间的内存中。要写入到磁盘上的log file中(redo:ib_logfileN文件,undo:share tablespace或.ibd文件)，中间还要经过操作系统内核空间的os buffer，调用fsync()的作用就是将OS buffer中的日志刷到磁盘上的log file中。也就是说，从redo log buffer写日志到磁盘的redo log file中，过程如下：

![存储概览](/img/mysql/19.jpg)   

在概念上，innodb 通过 force log at commit 机制实现事务的持久性，即在事务提交的时候，必须先将该事务的所有事务日志写入到磁盘上的 redo log file 和 undo log file 中进行持久化。

MySQL 支持用户自定义在 commit 时如何将 log buffer 中的日志刷 log file中。这种控制通过变量 innodb_flush_log_at_trx_commit 的值来决定。该变量有 3 种值：0、1、2，默认为 1。但注意，这个变量只是控制 commit 动作是否刷新 log buffer 到磁盘。

* 当设置为 1 的时候，事务每次提交都会将 log buffer 中的日志写入 os buffer 并调用 fsync() 刷到log file on disk 中。这种方式即使系统崩溃也不会丢失任何数据，但是因为每次提交都写入磁盘，IO 的性能较差。

* 当设置为 0 的时候，事务提交时不会将 log buffer 中日志写入到 os buffer，而是每秒写入 os buffer 并调用 fsync() 写入到 log file on disk 中。也就是说设置为 0 时是(大约)每秒刷新写入到磁盘中的。如果发生crash，即丢失1s内的事务修改操作。

* 当设置为 2 的时候，每次提交都仅写入到 os buffer，然后是系统内部来 fsync() 将 os buffer 中的日志写入到 log file on disk。如果数据库实例crash，不会丢失redo log，但是如果服务器crash，由于 os buffer 还来不及fsync到磁盘文件，所以会丢失这一部分的数据。

![存储概览](/img/mysql/16.png)   

> 在主从复制结构中，要保证事务的持久性和一致性，需要对日志相关变量设置为如下：
* 如果启用了二进制日志，则设置 sync_binlog=1 ，即每提交一次事务同步写到磁盘中。
* 总是设置 innodb_flush_log_at_trx_commit=1，即每提交一次事务都写到磁盘中。
