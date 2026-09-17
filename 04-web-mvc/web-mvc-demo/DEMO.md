# Web MVC demo

**Theory:** [`../WEB-MVC.md`](../WEB-MVC.md)

```bash
mvn -f 04-web-mvc/web-mvc-demo/pom.xml spring-boot:run
```

Port **8090**. In-memory store (no DB).

```text
POST /orders          JSON body  →  @RequestBody + @Valid
GET  /orders/{id}     path       →  @PathVariable
GET  /orders?status=  query      →  @RequestParam
404 / 400             →  @RestControllerAdvice
```

```bash
curl -s -X POST http://localhost:8090/orders -H "Content-Type: application/json" -d "{\"item\":\"pizza\",\"qty\":2}"
curl -s http://localhost:8090/orders/1
curl -s http://localhost:8090/orders/99
curl -s "http://localhost:8090/orders?status=OPEN"
curl -s -X POST http://localhost:8090/orders -H "Content-Type: application/json" -d "{\"item\":\"\",\"qty\":0}"
```

Expect: create JSON → 200 get → 404 `{"error":"..."}` → query ping → 400 validation.

**Filter vs Interceptor (console)** — after `GET /orders/1`:

```text
FILTER  in  GET /orders/1
INTERCEPTOR preHandle        handler=...OrderController...
INTERCEPTOR postHandle
INTERCEPTOR afterCompletion  status=200
FILTER  out 200
```

`RequestLogFilter` = around DispatcherServlet. `HandlerLogInterceptor` = inside it (`WebConfig` registers it). Interceptor `handler=` is the controller method — Filter does not print that.

**404** `GET /orders/99` (service throws) — **`postHandle` is missing**:

```text
FILTER  in  GET /orders/99
INTERCEPTOR preHandle        handler=...OrderController#get(long)
INTERCEPTOR afterCompletion  status=404
FILTER  out 404
```

`postHandle` runs only if the handler **returns normally**. Exception → advice still maps 404; `afterCompletion` still runs.
