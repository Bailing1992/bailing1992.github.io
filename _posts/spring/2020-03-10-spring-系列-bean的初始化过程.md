---
layout: post
title: "Spring 系列 bean的初始化过程"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - spring
---

> Spring Ioc 容器功能非常强大，负责 Spring Bean 的创建和管理等功能。

> Spring 管理单例模式 bean 的完整生命周期，对于 prototype 的 bean，Spring 在创建好交给使用者之后则不会再管理后续的生命周期。


## 过程
1. 定义 和 注册 Bean
2. 获取 Bean

## 定义和注册 bean
定义 BeanDefinition 并注册到 BeanFactory 的这个过程, 就是 bean 的定义和注册 , 通常使用 spring 时并不是把一个 bean 的实例注册, 而是一个 BeanDefinition.

> 常用的定义 bean 的方式：
1. 在 xml 中 定义 bean 标签
2. 注解扫描, 比如 @Service
3. 定义 Configuration 类, 在类中提供 @Bean 的方法来定义
4. 使用纯粹的 programmatically 的方式来定义和注册

## BeanDefinition
完全的定义了一个 bean 的实例化、初始化方式。

## Bean 实例化过程(单例模式)
![dubbo](/img/spring/2.webp)

1. get bean from singleton cache: 首先从 **singleton cache** 获取对象, 如果有, 说明初始化过, 直接返回, 如果没有, 继续
2. create merged definition: 找到 bean 的 definition (bd), 然后生成 merged bean definition (mbd). 这个主要是因为 bd 可以有 parent, 这个步骤的作用就是把 bd 和 它的 parent bd (如果有的话), 进行 merge, 生成 mbd, 之后就要根据这个 mbd 来实例化和初始化 bean。
3. check bean definition: 检查 mbd , 把 mbd 中 dependsOn 的 bean 都先初始化.
4. get singleton, if not found, then create bean: 再次从 singleton cache 中获取 bean, 如果没有, 则会真正 create bean
  1. resolve class：查找并 load 这个 bean 的 class
  2. resolveBeforeInstantiation: 在真正的实例化之前进行一次预先操作, 目的是给用户一个机会来进行非正常的实例化, 用户注入的 InstantiationAwareBeanPostProcessor 子类, 可以做一些 proxy, mock, 来取代真实的实例化返回, 如果没有产生 bean, 则继续往下走去正常的实例化阶段.
    * InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()
      * if return bean, BeanPostProcessor.postProcessAfterInitialization(), then return bean.
      * if return null, go on
  3. do create bean
    1. create instance: 真正的实例化, 调用 mbd 中定义的 factoryMethod, 或者类的构造方法, 来生成对象.
    2. MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition // 比如 autowire 等 注解注入 mbd
    3. add early singleton cache: 这一步主要是为了解决循环引用, 再把未初始化的 bean 的 reference 提供出来.
    4. populate bean: 填充属性阶段, properties values (pvs), 这一大步骤中的前三小步都是在构造 pvs, 并在最后一步 apply 进去
      * InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()
      * autowire: 把能够 set 的的属性写入 pvs
      * InstantiationAwareBeanPostProcessor.postProcessPropertyValues(): 把一些特殊属性写入, 比如没有 set 的 autowired 属性, 一些 @Value 的属性
      * apply property values
    5. initialize bean: 实例化完毕之后, 进行初始化.
      * aware beanName, classLoader, beanFactory: aware beanName, classLoader, beanFactory
      * BeanPostProcessor.postProcessBeforeInitialization()
      * init: 真正的初始化操作。如果 bean 是 InitializingBean, afterPropertiesSet()。调用自定义 init
      * BeanPostProcessor.postProcessAfterInitialization()
    6. register disposable bean





#### Context
Context 类实现了 BeanFacotry 接口, Context 是通过继承和组合的方式对 BeanFactory 的一层封装, 避免直接访问到 BeanFactroy, 保证运行时不能随意对 BeanFactroy 进行修改. 除此之外还提供了更多的功能: 它不仅有 BeanFactory 管理 bean 池的功能, 还要负责环境配置管理, 生命周期管理, 复杂的初始化操作等.


## 生成过程
在第一次从 BeanFactory 当中 getBean() 时, 这个 bean 的实例才会真正生成.