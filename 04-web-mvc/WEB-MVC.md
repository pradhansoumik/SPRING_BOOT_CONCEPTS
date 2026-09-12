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

---

## 4. DTO — not the entity

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

---

## 7. Filter vs Interceptor

| | Filter (`jakarta.servlet`) | Interceptor (`HandlerInterceptor`) |
|---|---|---|
| Where | Servlet container (around DispatcherServlet) | Spring MVC (controller methods) |
| Sees | All requests (static too, depending on mapping) | Only handler mappings |
| Typical | CORS, security, wrapping | Logging handler, add model |

Both can log; **Filter** is lower; **Interceptor** knows the **Controller** method.

---

## 8. Swagger / OpenAPI

`springdoc-openapi` → `/swagger-ui.html` from your mappings. Contract for consumers. Not required for MVC to work.

---

## 30-second pitch

> `@RestController` returns JSON via `DispatcherServlet`. Path / query / body annotations bind HTTP. **DTO** at the edge; **service** holds logic. `@Valid` + `@RestControllerAdvice` for 400/404. Filter = servlet; Interceptor = Spring MVC.
