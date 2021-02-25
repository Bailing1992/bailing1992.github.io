---
layout: post
title: "MySQL 系列 undo 日志"
subtitle: 'MySQL 技术内幕：InnoDB存储引擎'
author: "lichao"
header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
  - MySQL
---

```undo log``` 保存了事务发生之前的数据的一个版本，可以用于回滚，同时可以提供多版本并发控制下的读（MVCC），也即非锁定读。

undo log 存放在数据库内部的一个特殊段中，这个段称为 undo 段，位于共享表空间中。

undo log 和 redo log 不一样，undo log 是逻辑日志， redo log 是物理日志。可以认为当 delete 一条记录时，undo log 中会记录一条对应的 insert 记录，反之亦然，当 update 一条记录时，它记录一条对应相反的 update 记录。 因此只是将数据库逻辑的恢复到原来的样子。所有修改都被逻辑地取消了，但是数据结构和页本身在回滚之后可能大不相同。

undo log 的另一个作用是 MVCC，即在 InnoDB 存储引擎中 MVCC 的实现是通过 undo log 来完成。当用户读取一行记录时，若该记录已经被其他事务占用，当前事务可以通过 undo log 读取之前的行版本信息，以此实现非锁定读取。

事务开始之前，将当前是的版本生成 undo log，undo 也会产生 redo 来保证 undo log 的可靠性。

当事务提交之后，undo log 并不能立马被删除，而是放入待清理的链表，由 purge 线程判断是否由其他事务在使用 undo 段中表的上一个事务之前的版本信息，决定是否可以清理 undo log 的日志空间。

## 参考文献
[MySQL中的重做日志（redo log），回滚日志（undo log），以及二进制日志（binlog）的简单总结](https://www.cnblogs.com/wy123/p/8365234.html)