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

> 只有在数据库异常 crash 时，需要使用 redo log 恢复数据。

Redo Log 包括两部分：一是内存中的日志缓冲(redo log buffer)，该部分日志是易失性的。二是磁盘上的重做日志文件(redo log file)，该部分日志是持久的，并且是事务的记录是顺序追加的，性能非常高(磁盘的顺序写性能比内存的写性能差不了太多)。

**InnoDB 使用事务日志来减少提交事务时的开销。**因为日志中已经记录了事务，就无须在每个事务提交时把表空间缓冲池的脏块刷新(flush)到磁盘中。事务修改的数据和索引通常会映射到表空间的随机位置，所以刷新这些变更到磁盘需要很多随机IO。

InnoDB 用日志把随机 IO 变成顺序 IO。一旦日志安全写到磁盘，事务就持久化了，即使断电了，InnoDB 可以重放日志并且恢复已经提交的事务。

为了确保每次日志都能写入到事务日志文件中，在每次将 log buffer 中的日志写入日志文件的过程中都会调用一次操作系统的 fsync 操作(即```fsync()```系统调用)。因为 MySQL 是工作在用户空间的，MySQL 的 log buffer 处于用户空间的内存中。要写入到磁盘上的 log file 中，中间还要经过操作系统内核空间的os buffer，调用fsync()的作用就是将OS buffer中的日志刷到磁盘上的log file中。也就是说，从redo log buffer写日志到磁盘的redo log file中，过程如下：

![fsync操作](/img/mysql/fsync操作.jpg)   

在概念上，innodb 通过 force log at commit 机制实现事务的持久性，即在事务提交的时候，必须先将该事务的所有事务日志写入到磁盘上的 redo log file 和 undo log file 中进行持久化。

MySQL 支持用户自定义在 commit 时如何将 log buffer 中的日志刷 log file中。这种控制通过变量 ```innodb_flush_log_at_trx_commit``` 的值来决定。该变量有 3 种值：0、1、2，默认为 1。但注意，这个变量只是控制 commit 动作是否刷新 log buffer 到磁盘:
* 当设置为 1 的时候，事务每次提交都会将 log buffer 中的日志写入 os buffer 并调用 fsync() 刷到 log file on disk 中。这种方式即使系统崩溃也不会丢失任何数据，但是因为每次提交都写入磁盘，IO 的性能较差。
* 当设置为 0 的时候，事务提交时不会将 log buffer 中日志写入到 os buffer，而是每秒写入 os buffer 并调用 fsync() 写入到 log file on disk 中。也就是说设置为 0 时是(大约)每秒刷新写入到磁盘中的。如果发生 crash，即丢失 1s 内的事务修改操作。
* 当设置为 2 的时候，每次提交都仅写入到 os buffer，然后是系统内部来 fsync() 将 os buffer 中的日志写入到 log file on disk。如果数据库实例 crash，不会丢失redo log，但是如果服务器 crash，由于 os buffer 还来不及 fsync 到磁盘文件，所以会丢失这一部分的数据。

![redo log刷盘策略](/img/mysql/redolog刷盘策略.png)   

> 从重做日志缓冲往磁盘写入时，是按 512 个字节，是按一个扇区的大小进行写入。因为扇区是写入的最小单位，因此可以保证写入必然是成功的。因此在重做日志的写入过程中不需要有二次写。

#### 检查点（checkpoint）
数据经过更新或者删除之后，数据页变为脏页，需要刷回磁盘，在事务提交时，先写 redo log，再修改页，再在合适的时机刷回磁盘。这样即使宕机，也可以通过 redo log 来恢复数据。InnoDB 的redo log 是固定大小的，比如可以配置为一组 4 个文件，每个文件的大小是 1GB，那么总共就可以记录 4GB 的操作。从头开始写，写到末尾就又回到开头循环写。检查点表示脏页写入到磁盘的时候，所以检查点也就意味着脏数据的写入，其目的是：
* 缩短数据库恢复时间，将脏页刷新到磁盘
* 缓冲池不够用时，将脏页刷新到磁盘
* redo log 写满了，将脏页刷新到磁盘。要避免这种情况，因为此时整个系统就不能再接受更新了，所有的更新都必须停止。
* 正常关闭 数据库实例时，将脏页刷新到磁盘

检查点分为两种：
* sharp checkpoint：完全检查点，数据库正常关闭时，会触发把所有的脏页都写入到磁盘上。
* fuzzy checkpoint：模糊检查点，部分页写入磁盘。发生在数据库正常运行期间。主线程在空闲时会周期性执行，或者在空闲页不足，或者redo log文件快满时会执行模糊检查点。

## 参考文献

[MySQL checkpoint深入分析](https://www.cnblogs.com/geaozhang/p/7341333.html)