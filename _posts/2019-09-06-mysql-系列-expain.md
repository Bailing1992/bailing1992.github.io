---
layout: post
title: "MySQL 系列 EXPALIN"
subtitle: '《Mysql 技术内幕：InnoDB存储引擎》'
author: "lichao"
header-img: "img/post-bg-2015.jpg"
catalog: true
tags:
  - mysql
---

> 执行EXPALIN获取mysql数据库的执行计划


**字段：**   
rows：预计的返回函数   
possible_keys:   
SQL可能使用的索引   
Key：   
实际使用的索引    
type:   
查询类型   
range: 范围查询    
extra   
Using where：表示优化器需要通过索引回表查询数据；   
Using file sort:  需要额外的排序操作才能完成查询，在使用order by情况下；   
Using index：表示直接访问索引就足够获取到所需要的数据，不需要通过索引回表；      
Using intersect(b,a):表示根据两个索引得到的结果进行求交的数据运算，最后得到结果。       
Using index condition :在MySQL 5.6版本后加入的新特性（Index Condition Pushdown）;会先条件过滤索引，过滤完索引后找到所有符合索引条件的数据行，随后用 WHERE 子句中的其他条件去过滤这些数据行；   
Using MRR : 启用主键排序后回表或对查询条件进行拆分   


