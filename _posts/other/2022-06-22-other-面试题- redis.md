---
layout: post
title: "面试题- Redis汇总"
author: "lichao"
header-img: "img/post/bg/host.png"
catalog: true
tags:
  - other
---

## 请问Redis提供了哪几种持久化方式？
Redis 持久化机制 Redis 4.0 之前有两种，第一种是 RDB 快照，第二种是 AOF 日志。
+ RDB持久化方式能够在指定的时间间隔能对数据进行快照存储。快照是一次全量备份，在停机的时候会导致大量丢失数据。
+ AOF 日志是连续的增量备份。AOF 持久化方式记录每次对服务器写的操作，当服务器重启的时候会重新执行这些命令来恢复原始的数据，AOF命令以Redis协议追加保存每次写的操作到文件末尾。

快照是内存数据的二进制序列化形式，在存储上非常紧凑，而 AOF 日志记录的是内存数据修改的指令记录文本。AOF 日志在长期的运行过程中会变的无比庞大，数据库重启时需要加载 AOF 日志进行指令重放，这个时间就会无比漫长。所以需要定期进行 AOF 重写，给 AOF 日志进行瘦身。

Redis 4.0 之后，持久化机制可选择混合持久化方案，混合持久化方案结合了快照和 AOF 的优点。

[了解更多...](https://bailing1992.github.io/2020/06/10/redis-%E7%B3%BB%E5%88%97-%E6%8C%81%E4%B9%85%E5%8C%96/)

#### 子问题： 快照（bgsave）原理是什么？
fork 和 cow。fork 是指 Redis 通过创建子进程来进行 ```bgsave``` 操作，cow 指的是 copy on write，子进程创建后，父子进程共享数据段，父进程继续提供读写服务，写脏的页面数据会逐渐和子进程分离开来。

## 请问 Redis 相比 memcached 有哪些优势？
1. memcached 所有的值均是简单的字符串，Redis 作为其替代者，支持更为丰富的数据类型。
2. Redis 的速度比 memcached 快很多。
3. Redis 可以持久化其数据。
4. Redis 支持数据的备份，即 master-slave 模式的数据备份。


## 请问 Redis 存储什么情况下会丢失

## Redis 过期策略和内存淘汰策略
**过期策略（redis key过期时间）**        
1. 定时过期，每个key都创建一个定时器，到期清除，内存友好，cpu不友好
2. 惰性过期，使用时才判断是否过期，内存不友好，cpu友好
3. 定期过期，隔一段时间扫描一部分key，并清除已经过期的key

Redis 为了平衡时间和空间，采用了惰性过期和定期过期后两种策略。

[了解更多](https://bailing1992.github.io/2020/05/29/redis-%E7%B3%BB%E5%88%97-%E8%BF%87%E6%9C%9F%E6%B8%85%E7%90%86/)


**内存淘汰策略**           
1. noeviction：当内存使用超过配置的时候会返回错误，不会驱逐任何键。
2. allkeys-lru：首先通过 LRU 算法驱逐最久没有使用的键。
3. volatile-lru：首先从设置了过期时间的键集合中驱逐最久没有使用的键。
4. allkeys-random：从所有 key 随机删除。
5. volatile-random：从过期键的集合中随机驱逐。
6. volatile-ttl：从配置了过期时间的键中驱逐马上就要过期的键。
7. volatile-lfu：从所有配置了过期时间的键中驱逐使用频率最少的键。
8. allkeys-lfu：从所有键中驱逐使用频率最少的键。

[了解更多](https://bailing1992.github.io/2019/12/24/redis-%E7%B3%BB%E5%88%97-%E5%86%85%E5%AD%98%E6%B7%98%E6%B1%B0/)



## Redis如何实现分布式锁
需要考虑过期时间、value的设置、超时锁被释放导致的数据更新覆盖的相关问题。

