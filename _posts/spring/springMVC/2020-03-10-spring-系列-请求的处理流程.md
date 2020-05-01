---
layout: post
title: "Spring 系列 Spring MVC"
author: "lichao"
header-img: "img/netty/host.png"
catalog: true
tags:
  - spring
---

Spring Web MVC （Spring MVC) 是一套以 Servlet API 为基础平台的优雅的 Web 框架，一直是 Spring Framework 中重要的一个组成部分。 正式名称 “Spring Web MVC” 来自其源模块 spring-webmvc 的名称，但它通常被称为“Spring MVC”。

与 Spring Web MVC 并行，Spring Framework 5.0 引入了一个 Reactive stack —— Web框架，其名称 Spring WebFlux 也基于它的源模块 spring-webflux。

与许多其他 Web 框架一样，Spring MVC 同样围绕前端页面的控制器模式 (Controller) 进行设计，其中最为核心的 Servlet —— DispatcherServlet 为来自客户端的请求处理提供通用的方法，而实际的工作交由可自定义配置的组件来执行。 这种模型使用方式非常灵活，可以满足多样化的项目需求。

和任何普通的 Servlet 一样，DispatcherServlet 需要根据 Servlet 规范使用 Java 代码配置或在 web.xml 文件中声明请求和 Servlet 的映射关系。 DispatcherServlet 通过读取 Spring 的配置来发现它在请求映射，视图解析，异常处理等方面所依赖的组件。

以下是注册和初始化 DispatcherServlet 的 Java 代码配置示例。 该类将被 Servlet 容器自动检测到：

```
public class MyWebApplicationInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletCxt) {

        // 加载 Spring Web Application 的配置
        AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
        ac.register(AppConfig.class);
        ac.refresh();

        // 创建并注册 DispatcherServlet
        DispatcherServlet servlet = new DispatcherServlet(ac);
        ServletRegistration.Dynamic registration = servletCxt.addServlet("app", servlet);
        registration. (1);
        registration.addMapping("/app/*");
    }
}
```