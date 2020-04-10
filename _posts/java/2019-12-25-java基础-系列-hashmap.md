---
layout: post
title: "Java 基础 HashMap"
subtitle: '深究Java基础'
author: "lichao"
header-img: "img/post-bg-rwd.jpg"
catalog: true
tags:
  - Java 
---

## HashMap多线程并发存在什么问题？
#### 多线程put可能导致元素的丢失
#### put和get并发时，可能导致get为null
#### JDK7中 HashMap 并发 put 会造成循环链表，导致 get 时出现死循环
**此问题在JDK8中已经解决**

#### hashmap 处理循环删除 问题
在对hashmap集合进行遍历的时候，同时做了remove操作，这个操作最后导致抛出了java.util.ConcurrentModificationException的错误。

###### 使用迭代器提供的remove方法处理

```
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


## ConcurrentHashMap 如果处理多线程并发问题？