---
layout: post
title: "MySQL 系列 redo 日志"
subtitle: 'MySQL 技术内幕：InnoDB存储引擎'
author: "lichao"
header-img: "img/post/bg/post-bg-2015.jpg"
catalog: true
tags:
  - mysql
---

> 只有在数据库异常 crash 时，需要使用 redo log 恢复数据。

redo log 包括两部分：一是内存中的日志缓冲(redo log buffer)，该部分日志是易失性的。二是磁盘上的重做日志文件(redo log file)，该部分日志是持久的，并且是事务的记录是顺序追加的，性能非常高(磁盘的顺序写性能比内存的写性能差不了太多)。

**InnoDB 使用事务日志来减少提交事务时的开销。**因为日志中已经记录了事务，就无须在每个事务提交时把表空间缓冲池的脏块刷新(flush)到磁盘中。事务修改的数据和索引通常会映射到表空间的随机位置，所以刷新这些变更到磁盘需要很多随机IO。

InnoDB 用日志把随机 IO 变成顺序 IO。一旦日志安全写到磁盘，事务就持久化了，即使断电了，InnoDB 可以重放日志并且恢复已经提交的事务。

> 从重做日志缓冲往磁盘写入时，是按 512 个字节，是按一个扇区的大小进行写入。因为扇区是写入的最小单位，因此可以保证写入必然是成功的。因此在重做日志的写入过程中不需要有二次写。

## 写入逻辑
事务在执行过程中，生成的 redo log 是要先写到 redo log buffer 的。
如果事务执行期间 MySQL 发生异常重启，那这部分日志就丢了。由于事务并没有提交，所以这时日志丢了也不会有损失。
redo log 可能存在三种状态，对应的就是下图中的三个颜色块。
![redo log存储过程](/img/mysql/redolog存储过程.png){:height="80%" width="80%"}
这三种状态分别是：
* 存在 redo log buffer 中，物理上是在 MySQL 进程内存中，就是图中的红色部分；
* 写到磁盘(write)，但是没有持久化（fsync)，物理上是在文件系统的 page cache 里面，也就是图中的黄色部分；
* 持久化到磁盘，对应的是 hard disk，也就是图中的绿色部分。
  
为了控制 redo log 的写入策略，InnoDB 提供了 ```innodb_flush_log_at_trx_commit``` 参数，它有三种可能取值：
* 设置为 0 的时候，表示每次事务提交时都只是把 redo log 留在 redo log buffer 中;
* 设置为 1 的时候，表示每次事务提交时都将 redo log 直接持久化到磁盘；
* 设置为 2 的时候，表示每次事务提交时都只是把 redo log 写到 page cache。

InnoDB 有一个后台线程，每隔 1 秒，就会把 redo log buffer 中的日志，调用 write 写到文件系统的 page cache，然后调用 fsync 持久化到磁盘。

注意，事务执行中间过程的redo log也是直接写在redo log buffer中的，这些redo log也会被后台线程一起持久化到磁盘。也就是说，一个没有提交的事务的redo log，也是可能已经持久化到磁盘的。

实际上，除了后台线程每秒一次的轮询操作外，还有两种场景会让一个没有提交的事务的redo log写入到磁盘中。
1. 一种是，redo log buffer 占用的空间即将达到 ```innodb_log_buffer_size``` 一半的时候，后台线程会主动写盘。注意，由于这个事务并没有提交，所以这个写盘动作只是 write，而没有调用 fsync，也就是只留在了文件系统的 page cache。
2. 另一种是，并行的事务提交的时候，顺带将这个事务的 redo log buffer 持久化到磁盘。假设一个事务A执行到一半，已经写了一些redo log到buffer中，这时候有另外一个线程的事务B提交，如果```innodb_flush_log_at_trx_commit``` 设置的是 1，那么按照这个参数的逻辑，事务 B 要把 redo log buffer 里的日志全部持久化到磁盘。这时候，就会带上事务A在redo log buffer里的日志一起持久化到磁盘。

由两阶段提交可知，时序上redo log先prepare， 再写binlog，最后再把redo log commit。

如果把 ```innodb_flush_log_at_trx_commit``` 设置成 1，那么 redo log 在 prepare 阶段就要持久化一次，因为崩溃恢复逻辑是要依赖于 prepare 的 redo log，再加上 binlog 来恢复的。

每秒一次后台轮询刷盘，再加上崩溃恢复这个逻辑，InnoDB就认为redo log在commit的时候就不需要fsync了，只会write到文件系统的page cache中就够了。

## 检查点（checkpoint）
数据经过更新或者删除之后，数据页变为脏页，需要刷回磁盘，在事务提交时，先写 redo log，再修改页，再在合适的时机刷回磁盘。这样即使宕机，也可以通过 redo log 来恢复数据。InnoDB 的 redo log 是固定大小的，比如可以配置为一组 4 个文件，每个文件的大小是 1GB，那么总共就可以记录 4GB 的操作。从头开始写，写到末尾就又回到开头循环写。检查点表示脏页写入到磁盘的时候，所以检查点也就意味着脏数据的写入，其目的是：
* 缩短数据库恢复时间，将脏页刷新到磁盘
* 缓冲池不够用时，将脏页刷新到磁盘
* redo log 写满了，将脏页刷新到磁盘。要避免这种情况，因为此时整个系统就不能再接受更新了，所有的更新都必须停止。
* 正常关闭 数据库实例时，将脏页刷新到磁盘

检查点分为两种：
* sharp checkpoint：完全检查点，数据库正常关闭时，会触发把所有的脏页都写入到磁盘上。
* fuzzy checkpoint：模糊检查点，部分页写入磁盘。发生在数据库正常运行期间。主线程在空闲时会周期性执行，或者在空闲页不足，或者redo log文件快满时会执行模糊检查点。

## 参考文献

[MySQL checkpoint深入分析](https://www.cnblogs.com/geaozhang/p/7341333.html)