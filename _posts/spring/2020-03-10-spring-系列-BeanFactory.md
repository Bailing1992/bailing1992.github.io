---
layout: post
title: "Spring 系列 BeanFactory"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - spring
---

> DefaultListableBeanFactory 实现了 ListableBeanFactory 和 BeanDefinitionRegistry 接口，基于 BeanDefinition 对象，是一个成熟的 BeanFactroy。

![dubbo](/img/spring/3.jpg)

1. BeanFactory 提供了根据 name、type 等获取一个 bean 对象的能力
2. ListableBeanFactory 继承 BeanFactory，提供了列出所有 bean 对象的能力
3. HierarchialBeanFacotry 继承 BeanFactory， 使得 factory 拥有一个 parent factory, 可以存在层级关系
4. SingletonBeanRegistry 定义对单例的注册及获取，提供单例对象缓存能力
5. BeanDefinitionRegistry 定义对 BeanDefinition 的各种增删改操作
5. AutowireCapableBeanFacory 继承 BeanFactory，提供创建 bean、自动注入、初始化以及应用bean 的后处理器
6. ConfigurableBeanFactory 继承 ListableBeanFactory 和 HierarchialBeanFacotry，提供配置 factory 的这种方法, 比如设置 BeanPostProcessor
7. AliasRegistry 支持别名

## DefaultListableBeanFactory
DefaultListableBeanFactory 通过 BeanDefinition 定义类, 在第一次 get 时实例化, 并在实例化过程中同时实例化依赖类, 并做属性填充, 并执行一些初始化前后的 processor.

> 常从 xml 、代码注解中定义 bean, 通过 XmlBeanDefinitionReader 和 ClassPathBeanDefinitionScanner 生成 BeanDefinition 注册到 DefaultListableBeanFactory 中。

## 容器启动流程
1. 定义 和 注册 bean
2. 获取 bean

#### 定义和注册 bean
定义 BeanDefinition 并注册到 BeanFactory 的这个过程, 就是 bean 的定义和注册, 通常使用 spring 时并不是把一个 bean 的实例注册, 而是一个 BeanDefinition.

>  常用的定义 bean 的方式：
1. 在 xml 中 定义 bean 标签
2. 注解扫描, 比如 @Service
3. 定义 Configuration 类, 在类中提供 @Bean 的方法来定义
4. 使用纯粹的 programmatically 的方式来定义和注册

###### 配置文件方式实现逻辑
BeanDefinitionReader 的作用是读取 Spring 配置文件中的内容，将其转换为 IoC 容器内部的数据结构：BeanDefinition。 其抽象类 AbstractBeanDefinitionReader 中有一属性为BeanDefinitionRegistry。BeanDefinitionRegistry 是 DefaultListableBeanFactory 的子类，定义对 BeanDefinition 的各种增删改操作。当调用loadBeanDefinitions 的 loadBeanDefinitions 方法时， 会把 BeanDefinition 注册到 BeanDefinitionRegistry。

> BeanDefinitionRegistry 接口的实现类 DefaultListableBeanFactory 拥有属性 beanDefinitionMap，通过调用 registerBeanDefinition 方法 把（beanName, beanDefinition）放在 beanDefinitionMap 中。
###### 注解方式实现逻辑
SpringBoot 项目中或者 Spring 项目中配置
```<context:component-scan base-package="com.example.demo" />```
，那么在 IOC 容器初始化阶段（调用 beanFactoryPostProcessor 阶段） 就会采用ClassPathBeanDefinitionScanner 进行扫描包下所有类，并将符合过滤条件的类注册到 IOC 容器内。


#### bean 加载
![dubbo](/img/spring/4.png)
当调用 DefaultListableBeanFactory 的 getBean 方法时:
* 第一个参数为 String 类型时，最终会调用的 AbstractBeanFactory#doGetBean
* 第一个参数为 Class 类型时，最终会调用的 DefaultListableBeanFactory#resolveBean

> 当调用 DefaultListableBeanFactory.getBean(java.lang.Class<T>) 时，最终调用的是 resolveBean 方法解析 bean。接着调用 resolveBean.resolveNamedBean() 获取 bean:
1. 根据传入的类型获取 bean 的所有名称 BeanName
2. 过滤候选 bean 名称
3. 如果 bean 名称只有一个，那么直接调用 AbstractBeanFactory 里的 doGetBean 进行实例化并返回
4. 如果 bean 名称有多个，则选出主要候选名称或者最高优先级的名称来帮助实例化。如果没有选出可用的名称，则抛出 bean 定义冲突异常

###### doGetBean 过程
1. 转换对应 beanName：传入的参数可能是别名，也可能是FactoryBean ，所以需要进行一系列的
解析，这些解析内容包括如下内容：
  * 去除 FactoryBean 的修饰符，也就是如果阳ηe＝吐aa”，那么会首先去除＆而使na1ne＝”aa”
  * 取指定 alias 所表示的最终 beanName，例如别名 A 指向名称为 B 的bean 则返回B;
若别名 A 指向别名 B ，另外别名 B 又指向名称为 C 的bean， 则返回 C

2. 检查缓存中是否存在实例 singletonObjects.get(beanName), 有则直接返回，无则下一步。
2. 检查此 bean 是否正在加载 earlySingletonObjects.get(beanName)，是则不处理，否进行下一步。
3. 检查是否注册了初始化策略 singletonFactories.get(beanName)，是则：
  * 调用 预先设定的 getObject 方法 singletonFactory.getObject()
  * 记录在缓存中：earlySingletonObjects.put(beanName, singletonObject)，singletonFactories.remove(beanName)
4. 除了

> SingletonBeanRegistry 定义对单例的注册及获取，提供单例对象缓存能力。 其实现类 DefaultSingletonBeanRegistry 提供了注册及获取bean的实现，其重要属性：
* singletonObjects：用于保存 BeanName 和 创建 bean 实例之间的关系
* singletonFactories： 用于保存 BeanName 和创建 bean 的工厂之间的关系
* earlySingletonObjects：保存 BeanName 和 创建 bean 实例 之间的关系
