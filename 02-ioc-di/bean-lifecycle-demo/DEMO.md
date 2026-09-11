# Bean scope & lifecycle demo

**Theory:** [`../BEAN-SCOPE-AND-LIFECYCLE.md`](../BEAN-SCOPE-AND-LIFECYCLE.md)

```bash
mvn -f 02-ioc-di/bean-lifecycle-demo/pom.xml spring-boot:run
```

No web server. `main` **closes** the context so `@PreDestroy` prints in the same run.

---

## Actual flow (this demo)

```text
main()
  run()
    refresh()                          ★ singletons born
      LifecycleBean     ctor → @PostConstruct     (prints 1 then 2)
      OrderService
      ScopeInspector    created only — run() NOT called yet
      + Boot’s own beans (Environment, etc. — you didn’t write them)
    ScopeInspector.run()               ★ after ALL singletons READY
      getBean(OrderService) × 2  → same object → true
      getBean(Cart) × 2           → new each time (prototype) → false
  return ctx
  ctx.close()                          ★ not inside run()
    LifecycleBean @PreDestroy           (prints 3)
```

`Cart` is **not** created in `refresh()` — only when `getBean(Cart)` runs. Prototype has **no** container `@PreDestroy`.

---

## What to watch

**Lifecycle** (`LifecycleBean` — singleton):

```text
1. ctor
2. @PostConstruct
   ... scopes block ...
3. @PreDestroy          ← after ctx.close()
```

**Scopes:**

| Line | Expect |
|---|---|
| `OrderService singleton same instance?` | `true` |
| `Cart prototype same instance?` | `false` |

---

## Classes

| Class | Role |
|---|---|
| `LifecycleBean` | ctor → `@PostConstruct` → `@PreDestroy` |
| `OrderService` | default **singleton** |
| `Cart` | `@Scope("prototype")` |
| `ScopeInspector` | `getBean` twice for each |

Internally that path is `AbstractAutowireCapableBeanFactory.doCreateBean()` — see the theory note.
