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
