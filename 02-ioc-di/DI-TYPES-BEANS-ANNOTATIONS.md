# DI types, bean creation & annotations (KT)

**Story:** Food-delivery app. `OrderService` (dependent) needs `PaymentClient` (dependency).  
You do **not** `new PaymentClient()` inside `OrderService`. The **container** creates both and **injects**.

```text
OrderService  ──needs──►  PaymentClient
  dependent                  dependency
```

---

## 1. Types of DI (how it is injected)

| Type | How | `@Autowired` | Real life | Prefer? |
|---|---|---|---|---|
| **Constructor** | Pass `PaymentClient` in `new OrderService(...)` | Optional if **only one** constructor | Kitchen built **with** stove already installed | **Yes** — required deps, `final`, fail at startup if missing |
| **Setter** | `setPaymentClient(...)` after object exists | **Required** (or `@Inject`) | Swap payment vendor later | Optional deps only |
| **Field** | `@Autowired PaymentClient client;` | **Required** | Hidden wiring | Avoid in new code (hard to test, not `final`) |

**Missing `PaymentClient` bean** → `UnsatisfiedDependencyException` (required injection). App does not start.

---

## 2. Ways to create a bean (how it enters the container)

Same `PaymentClient` — three doors. **Scan vs `@Bean` vs import.**

| Way | When | Example |
|---|---|---|
| **Stereotype + `@ComponentScan`** | Your own class, default package scan | `@Service class OrderService` |
| **`@Configuration` + `@Bean`** | 3rd-party class you cannot annotate, or custom setup | `@Bean PaymentClient paymentClient()` |
| **`@Import`** | Pull a `@Configuration` that is **outside** scan | `@Import(PaymentConfig.class)` on main / another config |
| **Auto-config** | Boot / library (previous topic) | `.imports` + conditions |

If `PaymentClient` has **no** stereotype and **no** `@Bean` → nothing to inject → startup fail.

---

## 3. Annotations (what to say in KT)

### Register beans

| Annotation | Meaning |
|---|---|
| `@Component` | “This class is a bean” (generic) |
| `@Service` | Same, business layer (`OrderService`) |
| `@Repository` | Persistence; extra exception translation |
| `@Controller` / `@RestController` | Web layer |
| `@Configuration` | This class can declare `@Bean` methods |
| `@Bean` | Method **return value** is a bean |
| `@Import` | Load another config class **without** scanning that package |

`@Service` / `@Repository` / `@Controller` = `@Component` + extra meaning. Scan still required.

### Inject

| Annotation | Meaning |
|---|---|
| `@Autowired` | Spring: inject by **type** (then `@Qualifier` / `@Primary`) |
| `@Inject` | Jakarta (JSR-330) — same idea as `@Autowired`; **no** `required` flag |
| `@Resource` | JSR-250 — inject by **name** first, then type |
| `@Qualifier("paypal")` | Several `PaymentClient`s — pick **paypal** |
| `@Primary` | Several beans — this is the **default** |
| `@Lazy` | Inject a **proxy**; create real bean later (also used to break cycles) |

**`@Autowired` vs `@Inject`:** both wire by type. Boot apps usually use **`@Autowired`**. `@Inject` is the standard; Spring supports it. Same constructor/setter/field rules: **one constructor** still works **without** either annotation.

**`@Import` vs `@Autowired`:** `@Import` brings a **config class** into the context. `@Autowired` fills a **field/ctor** with an existing bean. Not interchangeable.

### Optional extras

| Annotation | Meaning |
|---|---|
| `@Scope("prototype")` | New instance each ask (default = **singleton**) |
| `@PostConstruct` / `@PreDestroy` | After inject / before destroy |

---

## 4. Tiny snippets (only the two patterns)

Constructor (preferred):

```java
@Service
public class OrderService {
    private final PaymentClient payment;

    public OrderService(PaymentClient payment) {  // @Autowired optional
        this.payment = payment;
    }
}
```

Two payment beans — pick one:

```java
public OrderService(@Qualifier("paypal") PaymentClient payment) { ... }
```

`@Bean` when you cannot put `@Component` on a library class:

```java
@Configuration
public class PaymentConfig {
    @Bean
    public PaymentClient paymentClient() {
        return new StripePaymentClient(apiKey);
    }
}
```

---

## KT in 20 seconds

> Container owns objects (**IoC**) and **injects** `PaymentClient` into `OrderService` (**DI**).  
> Beans come from **scan**, **`@Bean`**, or **`@Import`**.  
> **Constructor** DI is default; `@Autowired` on setter/field is mandatory.  
> `@Inject` ≈ `@Autowired`. `@Qualifier` / `@Primary` when more than one implementation.
