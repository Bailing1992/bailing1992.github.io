---
layout: post
title: "Spring 系列 Spring AOP"
author: "lichao"
header-img: "img/post/bg/post-bg-distance.png"
catalog: true
tags:
  - spring
---
![dubbo](/img/spring/8.png)

* Target(目标对象)：需要被代理增强的对象
* Proxy(代理对象)：目标对象被 AOP 织入 增强/通知后，产生的对象
* Joinpoint(连接点)：指那些被拦截到的点。在 Spring 中，这些点指方法(因为Spring只支持方法类型的连接点)
* Pointcut(切入点)：指需要(配置)被增强的 Joinpoint。
* Advice(通知/增强)：指拦截到 Joinpoint 后要做的操作。通知分为前置通知/后置通知/异常通知/最终通知/环绕通知等。
* Aspect(切面)：切入点和通知的结合。
* Weaving(织入)：指把增强/通知应用到目标对象来创建代理对象的过程(Spring采用动态代理织入,AspectJ采用编译期织入和 类装载期织入)。
* Introduction(引入增强)：在不修改类代码的前提下，可以在运行期为类动态地添加一些 Method/Field。

## 切面优先级

@Order(i)注解可用于标识切面的优先级。i值越小，优先级越高。

## 源码实现
