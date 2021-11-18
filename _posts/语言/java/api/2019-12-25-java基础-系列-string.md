---
layout: post
title: "Java 基础 String"
subtitle: '深究Java基础'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - java 
---


## hashcode
[引用自](http://www.tianxiaobo.com/2018/01/18/String-hashCode-%E6%96%B9%E6%B3%95%E4%B8%BA%E4%BB%80%E4%B9%88%E9%80%89%E6%8B%A9%E6%95%B0%E5%AD%9731%E4%BD%9C%E4%B8%BA%E4%B9%98%E5%AD%90/)

hashcode 的计算公式： s[0]31^(n-1) + s[1]31^(n-2) + ... + s[n-1]

```
    // 每个string 的哈希值只计算一次
    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            char val[] = value;

            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }

```

#### 选择 31 作为乘子的原因

1. 31 可以被 JVM 优化， `31 * i = (i << 5) - i`
2. 31 是一个不大不小的质数，是作为 hashCode 乘子的优选质数之一。


> 《Effective Java》: 选择数字 31 是因为它是一个奇质数，如果选择一个偶数会在乘法运算中产生溢出，导致数值信息丢失，因为乘二相当于移位运算。选择质数的优势并不是特别的明显，但这是一个传统。同时，数字 31 有一个很好的特性，即乘法运算可以被移位和减法运算取代，来获取更好的性能： **31*i==(i<<5)-i** ，现代的 Java 虚拟机可以自动的完成这个优化

> 正如 Goodrich 和 Tamassia 指出的那样，如果对超过 50,000 个英文单词（由两个不同版本的 Unix 字典合并而成）进行 hashCode 运算，并使用常数 31, 33, 37, 39 和 41 作为乘子，每个常数算出的哈希值冲突数都小于 7 个，所以在上面几个常数中，常数 31 被 Java 实现所选用也就不足为奇了