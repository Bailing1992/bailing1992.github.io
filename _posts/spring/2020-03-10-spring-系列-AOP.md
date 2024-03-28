---
layout: post
title: "Spring 系列 AOP"
author: "lichao"
header-img: "img/post/bg/post-bg-distance.png"
catalog: true
tags:
  - spring
---


摘自：[Spring AOP (AspectJ)](https://blog.csdn.net/javazejian/article/details/56267036)

AOP（Aspect-Oriented Programming，面向切面编程）能够将那些与业务无关，却为业务模块所共同调用的逻辑或责任（例如事务处理、日志管理、权限控制等）封装起来，便于减少系统的重复代码，降低模块间的耦合度，并有利于未来的可扩展性和可维护性。使用 AOP 之后可以把一些通用功能抽象出来，在需要用到的地方直接使用即可，这样可以大大简化代码量，提高了系统的扩展性。

> AOP 用于处理系统中分布于各个模块的横切关注点，比如 事务管理、日志、缓存等

![dubbo](/img/spring/7.png)

Spring AOP 是基于动态织入的动态代理技术，动态代理技术分为 Java JDK 动态代理和 CGLIB 动态代理，前者是基于反射技术的实现，后者是基于继承的机制实现。

> JDK 动态代理通过反射来接收被代理的类，并且要求被代理的类必须实现一个接口。JDK动态代理的核心是 InvocationHandler接口和 Proxy类。
>
> 如果目标类没有实现接口，那么 Spring AOP 会选择使用 CGLIB 来动态代理目标类。CGLIB（Code Generation Library），是一个代码生成的类库，可以在运行时动态的生成某个类的子类，注意，CGLIB 是通过继承的方式做的动态代理，因此如果某个类被标记为 final，那么它是无法使用 CGLIB 做动态代理的。
>
> Spring 2.0 中，Spring AOP 可通过使用 @Aspect 注解定义一个切面，不需要实现任何接口

## Spring AOP / AspectJ AOP 的区别？

Spring AOP 属于运行时增强，而 AspectJ 是编译时增强。

Spring AOP 基于代理（Proxying），而 AspectJ 基于字节码操作（Bytecode Manipulation）。

AspectJ 相比于 Spring AOP 功能更加强大，但是 Spring AOP 相对来说更简单。如果切面比较少，那么两者性能差异不大。但是，当切面太多的话，最好选择 AspectJ，它比 Spring AOP 快很多。

## 附录

### POP 面向过程程序设计

面向过程程序设计是以功能为中心来进行思考和组织的一种编程方式，强调的是系统的数据被加工和处理的过程。

### OOP 面向对象的程序设计

OOP 注重封装，强调整体性的概念，以对象为中心，将对象的内部组织与外部环境区分开来。

## AOP 面向切面编程
