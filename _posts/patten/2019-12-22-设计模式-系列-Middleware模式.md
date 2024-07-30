---
layout: post
title: "设计模式 系列 Middleware模式"
subtitle: '开启 设计模式 探索新篇章'
author: "lichao"
header-img: "img/post/bg/post-bg-rwd.jpg"
catalog: true
tags:
  - design_pattern 
---

![jvm](/img/post/design_pattern/1280X1280.PNG)

核心思想：核心逻辑与通用逻辑分离，适用于日志记录、性能统计、安全控制、事务处理、异常处理等。

- Handler是业务的核心逻辑，请求在进入核心业务逻辑之前会进过很多的中间层。比如一个请求在进入核心的Handler的时候会经过日志和Metric的中间层，产生相关的日志和打点。Response返回的时候会先经过Metrics，然后再经过日志。
- 中间件内部的执行顺序：
  - 预处理：pre-handler。
  - Next()：进入下一层函数。
  - post-handler：后处理。

```go

import "context"

// Endpoint represent one method for calling from remote.
type Endpoint func(ctx context.Context, req, resp interface{}) (err error)

// Middleware deal with input Endpoint and output Endpoint.
type Middleware func(Endpoint) Endpoint

// MiddlewareBuilder builds a middleware with information from a context.
type MiddlewareBuilder func(ctx context.Context) Middleware

// Chain connect middlewares into one middleware.
func Chain(mws ...Middleware) Middleware {
	return func(next Endpoint) Endpoint {
		for i := len(mws) - 1; i >= 0; i-- {
			next = mws[i](next)
		}
		return next
	}
}

// Build builds the given middlewares into one middleware.
func Build(mws []Middleware) Middleware {
	if len(mws) == 0 {
		return DummyMiddleware
	}
	return func(next Endpoint) Endpoint {
		return mws[0](Build(mws[1:])(next))
	}
}

// DummyMiddleware is a dummy middleware.
func DummyMiddleware(next Endpoint) Endpoint {
	return next
}

// DummyEndpoint is a dummy endpoint.
func DummyEndpoint(ctx context.Context, req, resp interface{}) (err error) {
	return nil
}

```