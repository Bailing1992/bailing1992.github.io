---
layout: post
title: "MySQL 系列 OrderBy使用技巧"
subtitle: '探寻 InnoDB中 OrderBy的实现机理...'
author: "lichao"
header-img: "img/post/bg/post-bg-夕阳.jpeg"
catalog: true
tags:
  - mysql
---


假设这个表的部分定义是这样的：

```sql
CREATE TABLE `t` (
  `id` int(11) NOT NULL,
  `city` varchar(16) NOT NULL,
  `name` varchar(16) NOT NULL,
  `age` int(11) NOT NULL,
  `addr` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `city` (`city`)
) ENGINE=InnoDB;
```

### 全字段排序

在`city`字段上创建索引，用`explain`命令来看看这个语句的执行情况。
![索引示例](/img/post/mysql/explain.webp)
Extra字段中的“Using filesort”表示需要排序，MySQL会给每个线程分配一块内存用于排序，称为sort_buffer。

通常情况下，这个语句执行流程如下所示：

- 初始化sort_buffer，确定放入name、city、age这三个字段；
- 从索引city找到第一个满足city='杭州’条件的主键id；
- 到主键id索引取出整行，取name、city、age三个字段的值，存入sort_buffer中；
- 从索引city取下一个记录的主键id；
- 重复步骤3、4直到city的值不满足查询条件为止；
- 对sort_buffer中的数据按照字段name做快速排序；
- 按照排序结果取前1000行返回给客户端；

sort_buffer_size，就是MySQL为排序开辟的内存（sort_buffer）的大小。如果要排序的数据量小于sort_buffer_size，排序就在内存中完成。但如果排序数据量太大，内存放不下，则不得不利用磁盘临时文件辅助排序。

**如果查询要返回的字段很多的话，那么sort_buffer里面要放的字段数太多，这样内存里能够同时放下的行数很少，要分成很多个临时文件，排序的性能会很差。所以如果单行很大，这个方法效率不够好。**

### rowid排序

如果排序的单行长度大于一定的阈值，就换为rowid排序。`max_length_for_sort_data`是MySQL中专门控制用于排序的行数据的长度的一个参数。它的意思是，如果单行的长度超过这个值，MySQL就认为单行太大。

新的算法放入sort_buffer的字段，只有要排序的列（即name字段）和主键id:

1. 初始化sort_buffer，确定放入两个字段，即name和id；
2. 从索引city找到第一个满足city='杭州’条件的主键id;
3. 到主键id索引取出整行，取name、id这两个字段，存入sort_buffer中；
4. 从索引city取下一个记录的主键id；
5. 重复步骤3、4直到不满足city='杭州’条件为止
6. 对sort_buffer中的数据按照字段name进行排序；
7. 遍历排序结果，取前1000行，并按照id的值回到原表中取出city、name和age三个字段返回给客户端。

特殊的，rowid排序后，需要回表；

### 不需排序

创建一个city和name的联合索引，可利用索引的有序性，直接返回有序的结果：
![索引示例](/img/post/mysql/explain2.webp)

1. 从索引(city,name)找到第一个满足city='杭州’条件的主键id；
2. 到主键id索引取出整行，取name、city、age三个字段的值，作为结果集的一部分直接返回；
3. 从索引(city,name)取下一个记录主键id；
4. 重复步骤2、3，直到查到第1000条记录，或者是不满足city='杭州’条件时循环结束。

Extra字段中没有Using filesort了，也就是不需要排序了。而且由于(city,name)这个联合索引本身有序，所以这个查询也不用把4000行全都读一遍，只要找到满足条件的前1000条记录就可以退出了。也就是说，在我们这个例子里，只需要扫描1000次。

OrderBy 同样可以使用 覆盖索引的特性，如果创建一个city、name和age的联合索引，可以省去回表的环节；
![索引示例](/img/post/mysql/explain3.webp)

可以看到，Extra字段里面多了“Using index”，表示的就是使用了覆盖索引，性能上会快很多。
