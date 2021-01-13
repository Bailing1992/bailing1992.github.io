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

undo log 有两个作用：提供回滚和多个行版本控制(MVCC)。

undo 存放在数据库内部的一个特殊段中，这个段称为 undo 段，位于共享表空间中。

undo log 和 redo log 记录物理日志不一样，它是逻辑日志。可以认为当 delete 一条记录时，undo log 中会记录一条对应的 insert 记录，反之亦然，当 update 一条记录时，它记录一条对应相反的 update 记录。 因此只是将数据库逻辑的恢复到原来的样子。所有修改都被逻辑地取消了，但是数据结构和页本身在回滚之后可能大不相同。

undo 的另一个作用是 MVCC，即在 InnoDB 存储引擎中 MVCC 的实现是通过 undo 来完成。当用户读取一行记录时，若该记录已经被其他事务占用，当前事务可以通过 undo 读取之前的行版本信息，以此实现非锁定读取。

undo log 会产生 redo log，只是因为 undo log 也需要持久性保护。