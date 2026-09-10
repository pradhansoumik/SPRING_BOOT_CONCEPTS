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
| **Constructor** | Pass `PaymentClient` in `new OrderService(...)` | **Optional** if **one** ctor; **required** if **2+** ctors (mark the one to use) | Kitchen built **with** stove already installed | **Yes** — required deps, `final`, fail at startup if missing |
| **Setter** | `setPaymentClient(...)` after object exists | **Required** (or `@Inject`); `required = false` = optional dep | Swap payment vendor later | Optional deps only |
| **Field** | `@Autowired PaymentClient client;` | **Required** | Hidden wiring (reflection, **no** setter) | Avoid in new code (hard to test, not `final`) |

**Better option:** **constructor** for required deps. Setter = optional extras. Field = avoid in new code.

`@Autowired` is **not** “call setter.” It only injects **where you put it**.

No `PaymentClient` bean + **required** injection → `UnsatisfiedDependencyException`, app does not start.

### Constructor — **preferred**

Kitchen built **with** the stove already in place.

**`@Autowired` optional** — class has **exactly one** constructor (Spring 4.3+):

```java
@Service
public class OrderService {
    private final PaymentClient payment;

    public OrderService(PaymentClient payment) {  // no @Autowired needed
        this.payment = payment;
    }
}
```

**`@Autowired` required** — **two or more** constructors: tell Spring which one to use:

```java
public OrderService(PaymentClient payment, NotificationClient sms) { ... }

@Autowired
public OrderService(PaymentClient payment) { this.payment = payment; }
```

| | |
|---|---|
| **Benefits** | `final` / immutable; all required deps present; fail at startup if missing; easy unit test (`new OrderService(mock)`) |
| **Drawbacks** | Many ctor args = class doing too much (split it) |

**Rules**

- Spring uses **only the `@Autowired` constructor** to create the bean.
- Other constructors are **normal Java** — container **does not call** them; you can still `new OrderService(...)` in tests.
- **Two** `@Autowired` ctors (both `required = true`) → **error** (ambiguous).
- Several `@Autowired(required = false)` → Spring picks the one whose deps it **can** satisfy.
- Multiple ctors and **none** annotated → Spring looks for a **no-arg** ctor; if none → **fail**.

### Setter — optional dependency

Swap payment vendor **after** the object exists.

**`@Autowired` required** — without it Spring **never** calls `setPaymentClient`:

```java
@Service
public class OrderService {
    private PaymentClient payment;   // cannot be final

    @Autowired
    public void setPaymentClient(PaymentClient payment) {
        this.payment = payment;
    }
}
```

**Optional** (bean missing → still start, field may stay `null`):

```java
@Autowired(required = false)
public void setPaymentClient(PaymentClient payment) { this.payment = payment; }
```

| | |
|---|---|
| **Benefits** | Change / mock after creation; good for **optional** collaborators |
| **Drawbacks** | Not `final`; easy to forget `@Autowired`; `required = false` → NPE later if you use it |

**Rules**

- No `@Autowired` / `@Inject` on the setter → Spring **never** calls it.
- Default `@Autowired` → bean **must** exist or startup fails.
- `@Autowired(required = false)` → missing bean → setter **not** called; field stays `null`.
- Several setters can each have `@Autowired` (each is a separate injection point).
- Setter injection happens **after** the object is constructed (no-arg or `@Autowired` ctor first).

### Field — avoid for new code

Hidden wiring; Spring sets the field with **reflection** (setter is **not** called).

**`@Autowired` required** on the field (or it stays `null`):

```java
@Service
public class OrderService {
    @Autowired
    private PaymentClient payment;   // not final; hard to unit-test without Spring
}
```

| | |
|---|---|
| **Benefits** | Least code |
| **Drawbacks** | Not `final`; hidden deps; tests need Spring or reflection; NPE if you `new OrderService()` yourself |

**Rules**

- `@Autowired` on the **field** → Spring sets it via **reflection**; a setter is **not** used.
- No `@Autowired` on the field → stays `null` (unless you set it yourself).
- `@Autowired(required = false)` → missing bean → field stays `null`; app still starts.
- `new OrderService()` yourself → **no** injection; field stays `null` → NPE when used.
- Can mix with constructor (ctor for required, field for extras) — prefer not to; keep one style.

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

**Stereotype + scan** — your class, under the main package (or a subpackage):

```java
@Service   // or @Component
public class StripePaymentClient implements PaymentClient { ... }

@SpringBootApplication   // includes @ComponentScan
public class Application { ... }
```

**`@Configuration` + `@Bean`** — library class you cannot annotate, or you need `new` + setup:

```java
@Configuration
public class PaymentConfig {
    @Bean
    public PaymentClient paymentClient() {
        return new StripeSdkClient(apiKey);  // 3rd-party type
    }
}
```

**`@Import`** — `PaymentConfig` is **not** in the scanned package:

```java
@SpringBootApplication
@Import(PaymentConfig.class)   // loads @Bean methods on PaymentConfig
public class Application { ... }
```

If `PaymentConfig` **is** already under the main package, scan picks it up — `@Import` not needed.

**Auto-config** — listed in `AutoConfiguration.imports`, not scanned:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    → com.example.GreetAutoConfiguration
```

Same as internals demo: conditions decide whether the `@Bean` is created.

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

## 4. Tiny snippets (`@Qualifier` / `@Bean`)

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
