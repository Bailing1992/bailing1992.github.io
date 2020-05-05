---
layout: post
title: "Spring 系列 AspectJ"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - spring
---

![dubbo](/img/spring/5.png)

## 使用示例
```
public class HelloWord {

    public void sayHello(){
        System.out.println("hello world !");
    }
    public static void main(String args[]){
        HelloWord helloWord =new HelloWord();
        helloWord.sayHello();
    }
}
```

编写 AspectJ 类，注意关键字为 aspect(MyAspectJDemo.aj, 其中 aj 为 AspectJ 的后缀)，含义与class相同，即定义一个 AspectJ 的类:


```
public aspect MyAspectJDemo {
    /**
     * 定义切点, 日志记录切点
     */
    pointcut recordLog():call(* HelloWord.sayHello(..));

    /**
     * 定义切点,权限验证(实际开发中日志和权限一般会放在不同的切面中,这里仅为方便演示)
     */
    pointcut authCheck():call(* HelloWord.sayHello(..));

    /**
     * 定义前置通知!
     */
    before():authCheck(){
        System.out.println("sayHello方法执行前验证权限");
    }

    /**
     * 定义后置通知
     */
    after():recordLog(){
        System.out.println("sayHello方法执行后记录日志");
    }
}
```
运行结果：
![dubbo](/img/spring/6.png)

使用 aspect 关键字定义了一个类，这个类就是一个切面，它可以是单独的日志切面(功能)，也可以是权限切面或者其他，在切面内部使用了 pointcut 定义了两个切点，一个用于权限验证，一个用于日志记录，而所谓的切点就是那些需要应用切面的方法，如需要在 sayHello 方法执行前后进行权限验证和日志记录，那么就需要捕捉该方法，而pointcut就是定义这些需要捕捉的方法（常常是不止一个方法的），这些方法也称为目标方法，最后还定义了两个通知，通知就是那些需要在目标方法前后执行的函数，如 before() 即前置通知在目标方法之前执行，即在sayHello() 方法执行前进行权限验证，另一个是after()即后置通知，在sayHello()之后执行，如进行日志记录。到这里也就可以确定，切面就是切点和通知的组合体，组成一个单独的结构供后续使用。

## 定义
#### pointcut 切入点
定义切点，后面跟着函数名称，最后编写匹配表达式，此时函数一般使用call()或者execution()进行匹配
```
pointcut 函数名 : 匹配表达式

 pointcut recordLog():call(* HelloWord.sayHello(..)); //recordLog()是函数名称，自定义的，* 表示任意返回值，接着就是需要拦截的目标函数，sayHello(..)的..，表示任意参数类型。

```
#### advice 通知
通知表示在某个特定的 pointcut 切入点上需要执行的动作。通知有 5 种类型分别如下：
* before 目标方法执行前执行，前置通知
* after 目标方法执行后执行，后置通知
* after returning 目标方法返回时执行 ，后置返回通知
* after throwing 目标方法抛出异常时执行 异常通知
* around 在目标函数执行中执行，可控制目标函数是否执行，环绕通知

```
[返回值类型] 通知函数名称(参数) [returning/throwing 表达式]：连接点函数(切点函数){
函数体
}

/**
  * 定义前置通知
  *
  * before(参数):连接点函数{
  *     函数体
  * }
  */
 before():authCheck(){
     System.out.println("sayHello方法执行前验证权限");
 }

 /**
  * 定义后置通知
  * after(参数):连接点函数{
  *     函数体
  * }
  */
 after():recordLog(){
     System.out.println("sayHello方法执行后记录日志");
 }


 /**
  * 定义后置通知带返回值
  * after(参数)returning(返回值类型):连接点函数{
  *     函数体
  * }
  */
 after()returning(int x): get(){
     System.out.println("返回值为:"+x);
 }

 /**
  * 异常通知
  * after(参数) throwing(返回值类型):连接点函数{
  *     函数体
  * }
  */
 after() throwing(Exception e):sayHello2(){
     System.out.println("抛出异常:"+e.toString());
 }



 /**
  * 环绕通知 可通过proceed()控制目标函数是否执行
  * Object around(参数):连接点函数{
  *     函数体
  *     Object result=proceed();//执行目标函数
  *     return result;
  * }
  */
 Object around():aroundAdvice(){
     System.out.println("sayAround 执行前执行");
     Object result=proceed();//执行目标函数
     System.out.println("sayAround 执行后执行");
     return result;
 }
```

#### aspect 切面

切面是定义切入点和通知的组合，定义通知应用到那些切入点上。如上述使用 aspect 关键字定义的类。

#### weaving 织入

把切面应用到目标函数的过程称为织入(weaving)

> 对于织入这个概念，可以简单理解为 aspect(切面) 应用到目标函数(类)的过程。对于这个过程，一般分为动态织入和静态织入，动态织入的方式是在运行时动态将要增强的代码织入到目标类中，这样往往是通过动态代理技术完成的，如 Java JDK 的动态代理(Proxy，底层通过反射实现)或者 CGLIB 的动态代理(底层通过继承实现)，Spring AOP 采用的就是基于运行时增强的代理技术，这里主要重点分析一下静态织入，ApectJ 采用的就是静态织入的方式。ApectJ 主要采用的是编译期织入，在这个期间使用 AspectJ 的acj编译器(类似javac)把aspect类编译成class字节码后，在 Java 目标类编译时织入，即先编译 aspect 类再编译目标类。

> 关于 ajc 编译器，是一种能够识别 aspect 语法的编译器，它是采用 Java 语言编写的，由于 javac 并不能识别 aspect 语法，便有了 ajc 编译器，注意 ajc 编译器也可编译 Java 文件。

> 除了编译期织入，还存在链接期(编译后)织入，即将 aspect 类和 Java 目标类同时编译成字节码文件后，再进行织入处理，这种方式比较有助于已编译好的第三方 jar 和 Class 文件进行织入操作

#### join point 连接点
可以切入通知的函数统称为连接点，切入点(pointcut)的定义正是从这些连接点中过滤出来的


## 概述
AspectJ 是一个 Java 实现的 AOP 框架，它能够对 Java 代码进行 AOP 编译（一般在编译期进行），让 Java 代码具有 AspectJ 的 AOP 功能（当然需要特殊的编译器），可以这样说 AspectJ 是目前实现 AOP 框架中最成熟，功能最丰富的语言，更幸运的是，AspectJ 与 Java 程序完全兼容，几乎是无缝关联，因此对于有Java 编程基础的工程师，上手和使用都非常容易。

> 