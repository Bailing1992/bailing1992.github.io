---
layout: post
title: "MySQL 系列 EXPALIN"
subtitle: '《MySQL 技术内幕：InnoDB存储引擎》'
author: "lichao"
header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
  - MySQL
---

> 执行EXPALIN获取mysql数据库的执行计划


**字段：**   
rows: 预计的返回函数   
possible_keys: SQL可能使用的索引   
key: 实际使用的索引    
type: 查询类型   
range: 范围查询    
extra:   
  * Using where:表示优化器需要通过索引回表查询数据；   
  * Using filesort:需要额外的排序操作才能完成查询，通常在使用```order by```情况下。MySQL 会给每个线程分配一块内存用于排序，称为 sort_buffer。排序可能在内存中完成，也可能需要使用外部排序，这取决于排序所需的内存和参数sort_buffer_size。  
  * Using index:表示直接访问索引就足够获取到所需要的数据，不需要通过索引回表，即优化器进行了覆盖索引操作。   
  * Using intersect(b,a):表示根据两个索引得到的结果进行求交的数据运算，最后得到结果。       
  * Using index condition: 在 MySQL 5.6 版本后加入的索引下推优化（Index Condition Pushdown）会先条件过滤索引，过滤完索引后找到所有符合索引条件的数据行，随后用 WHERE 子句中的其他条件去过滤这些数据行。   
  * Using MRR:启用主键排序后回表或对查询条件进行拆分。


