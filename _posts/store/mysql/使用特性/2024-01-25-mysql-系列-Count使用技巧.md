---
layout: post
title: "MySQL 系列 Count使用技巧"
subtitle: '探寻 InnoDB中 Count的实现机理...'
author: "lichao"
header-img: "img/post/bg/post-bg-夕阳.jpeg"
catalog: true
tags:
  - mysql
---


**先说结论：** 按照效率排序的话，`count(字段)`< `count(主键id)` < `count(1)`≈`count(*)`，所以建议，尽量使用`count(*)`。

`count()`是一个聚合函数，对于返回的结果集，一行行地判断，如果`count()`函数的参数不是`NULL`，累计值就加1，否则不加。最后返回累计值。所以`count(*)`、`count(主键id)`和`count(1)`都表示返回满足条件的结果集的总行数；而`count(字段）`，则表示返回满足条件的数据行里面，参数“字段”不为`NULL`的总个数。

### count(*)的实现方式

count(*)是例外，并不会把全部字段取出来，而是专门做了优化，不取值。count(*)肯定不是null，可以直接按行累加。对于count(*)这样的操作，遍历哪个索引树得到的结果逻辑上都是一样的。因此，MySQL优化器会找到最小的那棵树来遍历。在保证逻辑正确的前提下，尽量减少扫描的数据量，是数据库系统设计的通用法则之一。

### count(主键id)

对于count(主键id)来说，InnoDB引擎会遍历整张表，把每一行的id值都取出来，返回给server层。server层拿到id后，判断是不可能为空的，就按行累加。

### count(1)

对于count(1)来说，InnoDB引擎遍历整张表，但不取值。server层对于返回的每一行，放一个数字“1”进去，判断是不可能为空的，按行累加。

### count(字段)

对于count(字段)来说：

- 如果这个“字段”是定义为`not null`的话，一行行地从记录里面读出这个字段，判断不能为`null`，按行累加；
- 如果这个“字段”定义允许为`null`，那么执行的时候，判断到有可能是`null`，还要把值取出来再判断一下，不是 null 才累加。
