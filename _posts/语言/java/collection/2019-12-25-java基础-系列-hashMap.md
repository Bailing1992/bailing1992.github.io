---
layout: post
title: "Java 基础 HashMap"
subtitle: '深究Java基础'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - java 
---

要点：

1. 数组初始长度为16
2. 当链表长度太长（默认超过8）时，链表就转换为红黑树

HashMap 允许有一条记录的 key 为 null，但是对值是否为 null 不做要求。

## 基础问题

### 数据底层具体存储的是什么？这样的存储方式有什么优点呢？

HashMap 使用哈希表来存储。哈希表为解决冲突，可以采用开放地址法和链地址法等来解决问题，HashMap采用了链地址法。链地址法就是数组加链表的结合。当链表长度太长（默认超过8）时，链表就转换为红黑树，利用红黑树快速增删改查的特点提高HashMap的性能。

### 优化处理点

#### 定位数组索引位置

```java
// 方法一：
static final int hash(Object key) {   //jdk1.8 & jdk1.7
     int h;
     // h = key.hashCode() 为第一步 取hashCode值
     // h ^ (h >>> 16)  为第二步 高位参与运算
     return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
// 方法二：
static int indexFor(int h, int length) {  //jdk1.7的源码，jdk1.8没有这个方法，但是实现原理一样的
     return h & (length-1);  //第三步 取模运算
}
```

通过 h & (table.length -1) 来得到该对象的保存位，而 HashMap 底层数组的长度总是 2 的 n 次方，这是HashMap 在速度上的优化。当 length 总是 2 的 n 次方时，h& (length-1) 运算等价于对 length 取模，也就是h%length，但是&比%具有更高的效率。

在JDK1.8的实现中，优化了高位运算的算法，通过hashCode()的高16位异或低16位实现的：(h = k.hashCode()) ^ (h >>> 16)，主要是从速度、功效、质量来考虑的，这么做可以在数组table的length比较小的时候，也能保证考虑到高低Bit都参与到Hash的计算中，同时不会有太大的开销。

### HashMap多线程并发存在什么问题？

#### 多线程put可能导致元素的丢失

#### put和get并发时，可能导致get为null

### JDK7中 HashMap 并发 put 会造成循环链表，导致 get 时出现死循环

此问题在JDK8中已经解决

### hashmap 处理循环删除 问题

在对hashmap集合进行遍历的时候，同时做了remove操作，这个操作最后导致抛出了java.util.ConcurrentModificationException的错误。

#### 使用迭代器提供的remove方法处理

```java
 Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator();
      while(it.hasNext()){
          Map.Entry<Integer, String> entry = it.next();
          Integer key = entry.getKey();
          if(key % 2 == 0){
               System.out.println("To delete key " + key);
               it.remove();    
               System.out.println("The key " + + key + " was deleted");

          }
      }
```

### ConcurrentHashMap 如果处理多线程并发问题？
