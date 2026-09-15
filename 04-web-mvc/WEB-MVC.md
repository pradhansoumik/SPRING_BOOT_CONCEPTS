# Web layer (Spring MVC)

**Demo:** [`web-mvc-demo/DEMO.md`](web-mvc-demo/DEMO.md)

**When:** after `refresh()`, **Tomcat** is up. HTTP hits **DispatcherServlet** → your `@RestController`.

```text
Client  →  Tomcat  →  DispatcherServlet  →  Controller  →  Service
                         Filter (before/after)
                         Interceptor (pre/post handle)
```

---

## 1. `@RestController` vs `@Controller`

| | `@Controller` | `@RestController` |
|---|---|---|
| Meaning | MVC + **views** (Thymeleaf) | `@Controller` + `@ResponseBody` |
| Return | View name | **JSON** (HTTP body) |
| APIs | Rare | **Use this** for REST |

---

## 2. Mappings

| Annotation | HTTP |
|---|---|
| `@GetMapping` | GET (read, safe) |
| `@PostMapping` | POST (create) |
| `@PutMapping` | PUT (replace) |
| `@PatchMapping` | PATCH (partial) |
| `@DeleteMapping` | DELETE |

Class-level `@RequestMapping("/orders")` + method `@GetMapping("/{id}")` → `GET /orders/{id}`.

---

## 3. Parameters

| Annotation | From | Example |
|---|---|---|
| `@PathVariable` | URL path | `/orders/42` → `id=42` |
| `@RequestParam` | Query string | `/orders?status=OPEN` |
| `@RequestBody` | JSON body | POST/PUT/PATCH |

**`@PathVariable` vs `@RequestParam` — what & why**

| | `@PathVariable` | `@RequestParam` |
|---|---|---|
| **What** | Piece **of the URL path** | **Query string** after `?` |
| Example | `GET /orders/42` | `GET /orders?status=OPEN` |
| Typical | **Which resource** (id, username) | **Filter / sort / page / optional flag** |
| Required? | Usually **yes** (no id → different URL) | Often **optional** (`required = false`, `defaultValue`) |

**Why choose**

- Path = **identity**: “this order”. REST: `/orders/{id}`, not `/orders?id=42` for the main resource.
- Query = **how to list/filter**: status, page, size — same resource collection, different view.
- Mixing: `GET /orders/42/items?page=0` — `42` is path, `page` is query.

**KT line:** **who/which** → path. **optional criteria** → query.

---

## 4. DTO — not the entity
A DTO is a POJO used as a data bag across a boundary (usually JSON ↔ controller ↔ service).
DTO = role  (transfer data)

```text
Controller  ←DTO→  Service  ←entity→  DB
```

JSON in/out = **DTO**. JPA `@Entity` stays behind the service. Prevents leaking DB shape and lazy-load surprises.

---

## 5. Validation

```java
public record OrderRequest(@NotBlank String item, @Min(1) int qty) {}

@PostMapping
public OrderResponse create(@Valid @RequestBody OrderRequest req) { ... }
```

`@Valid` → Bean Validation. Fail → `400` (handle in `@ControllerAdvice`).

> `@Valid` does not check HTTP, JSON syntax, or security. It means: run Bean Validation on this Java object (here, the OrderRequest already built from the body).
> `@Valid` validates the input requests & actual rules are defined in Requests classes.

---

## 6. Exception handling

```java
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> notFound(OrderNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }
}
```

Structured JSON (`error`, `code`) — don’t return stack traces.

**Behind the scenes** — not a proxy around the service. Throw **bubbles up**; **DispatcherServlet** routes it.

```text
OrderController / OrderService
        throws OrderNotFoundException
                ▼
DispatcherServlet  (does not crash Tomcat)
                ▼
HandlerExceptionResolver
  finds @ExceptionHandler on @RestControllerAdvice (ApiErrors)
                ▼
your method runs  →  JSON + HTTP status (404 / 400)
                ▼
response to client
```

1. Service throws (controller has no try/catch).  
2. Exception returns to **DispatcherServlet**.  
3. **HandlerExceptionResolver** (`ExceptionHandlerExceptionResolver`) looks up the exception type.  
4. It was indexed at `refresh()` from `@RestControllerAdvice` + `@ExceptionHandler`.  
5. **`ApiErrors.notFound(...)`** runs (normal bean method).  
6. Status + JSON written to the client.

`@Valid` fail → `MethodArgumentNotValidException` (service **not** called) → same 2–6 → **400**.

No matching `@ExceptionHandler` (and no superclass match on your advice) → MVC **does not** call `ApiErrors`. Servlet error dispatch → **`GET /error`** → Boot `DefaultErrorAttributes` + `BasicErrorController` → generic JSON (or HTML whitelabel).

Typical default body: `timestamp`, `status`, `error`, `path` — usually **500**.

`@ResponseStatus` **on the exception class** can still set HTTP status without an handler; the **body** stays the default `/error` shape.

**KT line:** advice = **your** JSON. No match = generic `/error`. That’s why we add `@ExceptionHandler` for types we care about.

---

**Interview line:** Service throws, controller doesn’t catch, **DispatcherServlet** delegates to **HandlerExceptionResolver**, which maps the type to **`@ExceptionHandler`** on **`@RestControllerAdvice`**. That’s MVC exception routing, not AOP on the service. `@Transactional` proxy (if any) still **rethrows** so MVC can map it.

**try/catch in the controller vs `@RestControllerAdvice`**

You **can** catch in the controller. Then advice **does not** run for that exception — you already handled it.

```java
@GetMapping("/{id}")
public ResponseEntity<?> get(@PathVariable long id) {
    try {
        return ResponseEntity.ok(orders.get(id));
    } catch (OrderNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }
}
```

Mostly we use **`@RestControllerAdvice`** to **centralize** in one place. Otherwise the **same HTTP outcome** (for exceptions the controller method actually sees) can be done with try/catch — you just **repeat** it on every method.

| | try/catch in `get()` | `@RestControllerAdvice` |
|---|---|---|
| `OrderNotFoundException` from service | You can catch | Yes, one handler |
| **Every** controller | Must catch **in each** method | **One** class |
| **`@Valid` fail** | Happens **before** `create()` → **try never runs** | `MethodArgumentNotValidException` still handled |
| Other exceptions (filters, etc.) | Won’t see them | Only if they still reach DispatcherServlet |

If you catch and **rethrow**, advice **can** still run.

**KT line:** try/catch = **local**. Advice = **one place for the app**. Prefer advice; try/catch only when that method must do something **different**.

---

## 7. Filter vs Interceptor

| | Filter (`jakarta.servlet`) | Interceptor (`HandlerInterceptor`) |
|---|---|---|
| Where | Servlet container (around DispatcherServlet) | Spring MVC (controller methods) |
| Sees | All requests (static too, depending on mapping) | Only handler mappings |
| Typical | CORS, security, wrapping | Logging handler, add model |

Both can log; **Filter** is lower; **Interceptor** knows the **Controller** method.

HTTP hits the **servlet container** first. **`DispatcherServlet`** is Spring MVC’s front controller. Filter and Interceptor sit on **different sides** of that servlet.

```text
Request (client)
   ▼
Filter(s) in                 ← Jakarta Servlet (around the servlet)
   ▼
DispatcherServlet            ← Spring MVC front door
   ▼
Interceptor preHandle
   ▼
@Controller / @RestController  →  Service
   ▼
Controller returns (DTO / JSON)
   ▼
Interceptor postHandle
   ▼
DispatcherServlet writes HTTP body + status
   ▼
Interceptor afterCompletion
   ▼
Filter(s) out
   ▼
Response (client)
```

| | Tied to |
|---|---|
| Filter | Servlet container → **around** `DispatcherServlet` |
| Interceptor | **Inside** `DispatcherServlet` → your controller method |

Filter does not know “which `@GetMapping`” unless you dig. It can also see requests that never reach a controller (depending on mapping). Interceptor runs **after** Spring has chosen the handler (`OrderController.get`).

**KT line:** Filter = **outside** the dispatcher. Interceptor = **inside** it, on the way to the controller.

---

## 8. Swagger / OpenAPI

`springdoc-openapi` → `/swagger-ui.html` from your mappings. Contract for consumers. Not required for MVC to work.

---

## 30-second pitch

> `@RestController` returns JSON via `DispatcherServlet`. Path / query / body annotations bind HTTP. **DTO** at the edge; **service** holds logic. `@Valid` + `@RestControllerAdvice` for 400/404. Filter = servlet; Interceptor = Spring MVC.
