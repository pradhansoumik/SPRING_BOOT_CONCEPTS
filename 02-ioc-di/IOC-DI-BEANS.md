# IoC, DI & Beans

**Connects to:** after `refresh()`, beans live in `ApplicationContext` (the IoC container).

---

## 1. IoC (Inversion of Control)

**You** do not `new` the graph. The **container** creates beans and wires them.

```text
Without IoC:  A a = new A(new B());     // you control life
With IoC:     container.getBean(A)    // container created B, then A
```

| Piece | Remember |
|---|---|
| **IoC container** | Creates, wires, destroys beans |
| **`ApplicationContext`** | Spring’s IoC container (what `run()` returns) |
| **Bean** | Object managed by that container |

---

## 2. DI — what happens

Injecting dependencies → **avoid tight coupling**.

When the container creates bean **A**, it looks at declared dependencies (**B**):

1. If **B** is not ready → create and initialize **B** first  
2. Inject **B** into **A** (constructor / setter / field)  
3. **A** is fully built and registered in the context  

**Dependencies are created before the dependent bean is finalized.**

```text
Order needed:  B  →  then A(B)
                 └── inject
```

---

## 3. Three types of DI

### Field

```java
@Autowired
Order order;
```

| | |
|---|---|
| + | Easy |
| − | Not immutable; **NPE** if container didn’t inject (e.g. `new` yourself); hard to test |

### Setter

```java
@Autowired
public void setOrder(Order order) {
    this.order = order;
}
```

| | |
|---|---|
| + | Can change after creation; easy to pass mocks |
| − | Field cannot be `final`; harder to read / not the default style |

### Constructor (prefer this)

```java
public OrderService(Order order) {   // 1 constructor → @Autowired optional
    this.order = order;
}
```

| | |
|---|---|
| + | Resolved at construction; **no NPE** if required deps missing → fail fast |
| + | All required deps present; **immutable** (`final`) |
| + | One constructor → `@Autowired` **not** required |

**Interview:** constructor = required deps; setter = optional; field = avoid in new code.

---

## 4. Missing bean (your scenario)

Class **B** has **no** `@Component` / `@Service` / `@Bean`, but **A** `@Autowired` B.

→ **App does not start** → `UnsatisfiedDependencyException`

Container is creating **A**, needs **B**, **B** is not in the IoC container → nothing to inject.

Same rule as above: **create dependency beans first**, then the source bean.

**Fix:** `@Component` / `@Service` on B (and scanned), or `@Bean` B, or `@Import`.

---

## 5. How a class becomes a bean

| Way | Example |
|---|---|
| Stereotype + scan | `@Component` `@Service` `@Repository` `@Controller` |
| `@Configuration` + `@Bean` | you `new` / construct in a method |
| Auto-config | Boot’s `.imports` (previous topic) |

`@Service` / `@Repository` / `@Controller` are **`@Component`** with extra meaning (exception translation, MVC, etc.).

---

## 6. Scopes & lifecycle (short)

| Scope | Meaning |
|---|---|
| **singleton** (default) | One instance per context |
| **prototype** | New instance every `getBean` / inject |
| **request** / **session** | Web only — per HTTP request / session |

```text
instantiate → inject deps → @PostConstruct → ready → use → @PreDestroy (singleton)
```

---

## 7. Two beans of same type

```java
@Primary                    // default when type is enough
@Qualifier("paypal")        // pick by name
```

`@Qualifier` wins when you name it; `@Primary` is the fallback.

---

## 8. Circular dependency → `@Lazy`

```text
A needs B, B needs A
```

Constructor cycle → **fail** (Boot 2.6+ circular refs **off** by default).

`@Lazy` on one injection → inject a **proxy**, break the cycle (code smell; fix the design if you can).

---

## 30-second pitch

> IoC container (`ApplicationContext`) creates beans and injects deps **B before A**. Prefer **constructor** DI. No stereotype/`@Bean` on B → `UnsatisfiedDependencyException`. `@Primary` / `@Qualifier` if several implementations; `@Lazy` only as a last resort for cycles.
