---
layout: post
title: "Spring 系列 Spring MVC"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - spring
---

Spring Web MVC （Spring MVC) 是一套以 Servlet API 为基础平台的优雅的 Web 框架，一直是 Spring Framework 中重要的一个组成部分。

与许多其他 Web 框架一样，Spring MVC 同样围绕前端页面的控制器模式 (Controller) 进行设计，其中最为核心的 Servlet —— DispatcherServlet 为来自客户端的请求处理提供通用的方法，而实际的工作交由可自定义配置的组件来执行。 这种模型使用方式非常灵活，可以满足多样化的项目需求。

## 组件
 
#### DispatcherServlet 
包含了 SpringMVC 的请求逻辑， Spring 使用此类拦截 Web 请求并进行相应的逻辑处理。

> 和任何普通的 Servlet 一样，DispatcherServlet 需要根据 Servlet 规范使用 Java 代码配置或在 web.xml 文件中声明请求和 Servlet 的映射关系。 DispatcherServlet 通过读取 Spring 的配置来发现它在请求映射，视图解析，异常处理等方面所依赖的组件。
#### model
模型对于 SpringMVC 来说并不是必不可少，如果处理程序非常简单，完全可以忽略。模
型创建主要的目的就是承载数据，使数据传输更加方便。
#### controller
控制器用于处理 Web 请求， 每个控制器都对应着一个逻辑处理。
#### ModelAndView
在请求的最后返回了 ModelAndView 类型的实例。ModelAndView 类在SpringMVC 中占有很重要的地位，控制器执行方法都必须返回一个ModelAndView, Mode!A ndVi ew 对象保存了
视图以及视图显示的模型数据。

## 启动

ContextLoaderListener 的作用就是启动 Web 容器时，自动装配 ApplicationContext 的配置信息。因为它实现了 ServletContextListener 这个接口，在 web.xml 配置这个监昕器，启动容器时，就会默认执行它实现的 contextlnitialized 方法.

> 使用 ServletContextListener 接口，开发者能够在为客户端请求提供服务之前向ServletContext 中添加任意的对象。这个对象在 ServletContext 启动的时候被初始化， 然后在ServletContext 整个运行期间都是可见的。

> 每一个 Web 应用都有一个 ServletContext 与之相关联。ServletContext 对象在应用启动时被创建，在应用关闭的时候被销毁。S巳rvletContext 在全局范围内有效，类似于应用中的一个全局变量。

> 在ServletContextListener 中的核心逻辑便是初始化WebApplication Context 实例并存放至
ServletContext 中。

#### 初始化 WebApplicationContext
1. 通过反射的方式创建 WebApplicationContext 实例。
2. 将实例记录在 servletContext 中。
3. 映射当前的类加载器与创建的实例到全局变量currentContextPerThread 中。

#### 启动 DispatcherServlet
Servlet 是按照 Servlet 规范编写的一个 Java 类，此程序是基于 Http 协议，在服务器端运行（如Tomcat）， 主要是处理客户端的请求并将其结果发送到客户端。Servlet 的生命周期是由 Servlet 的容器来控制的，它可以分为 3 个阶段：初始化、运行和销毁。
1. 初始化阶段： 创建对象并初始化 init
2. 运行：当 servlet 容器接收到一个请求时，servlet 容器会针对这个请求创建 servletRequest 和 servletResponse 对象，然后调用 service 方法。
3. 销毁阶段：当 Web 应用被终止时，servlet 容器会先调用 servlet 对象的 destrory 方法，然后再销毁 Servlet对象，同时也会销毁与 servlet 对象相关联的 servletConfig 对象。

#### DispatcherServlet 初始化
在 servlet 初始化阶段会调用其 init 方法，DispatcherServlet 同样需要初始化。

1. 通过将当前的 servlet 类型实例转换为 BeanWrapper 类型实例，以便使用 Spring 中提供的注入功能进行对应属性的注入。
2. 属性注入
2. initServletBean 初始化: 在 ContextLoaderListener 加载的时候已经创建了WebApplicationContext 实例，而在这个函数中最重要的就是对这个实例进行进一步的补充初始化。

> PropertyAccessorFactory.forBeanPropertyAccess 是 Spring 中提供的工具方法，主要用于将指定实例转化为Spring 中可以处理的BeanWrapp er 类型的实例。

##### initServletBean 初始化 WebApplicationContent
initWebApplicationContext 函数的主要工作就是创建或刷新 WebApplicationContext 实例并
对 servlet 功能所使用的变量进行初始化。

1. 寻找或创建对应的 WebApplicationContext 实例