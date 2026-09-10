# Bean scope & lifecycle demo

**Theory:** [`../BEAN-SCOPE-AND-LIFECYCLE.md`](../BEAN-SCOPE-AND-LIFECYCLE.md)

```bash
mvn -f 02-ioc-di/bean-lifecycle-demo/pom.xml spring-boot:run
```

No web server. `main` **closes** the context so `@PreDestroy` prints in the same run.

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
