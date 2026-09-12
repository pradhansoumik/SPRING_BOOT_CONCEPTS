# Configuration & Profiles

**Demo:** [`config-profiles-demo/DEMO.md`](config-profiles-demo/DEMO.md)

**When:** `run()` step 3 — **Environment** is built **before** `refresh()`. Beans and `@ConditionalOn*` then **read** that Environment.

```text
run()
  1–2  type + container
  3    Environment   ← files, env vars, CLI, profiles   ★ this chapter
  4    refresh()     ← beans + @ConditionalOnProperty see the Environment
```

---

## 1. `application.properties` vs `.yml`

Same keys. YAML is nested; properties are flat.

```properties
app.payment.url=http://pay
app.payment.timeout=5s
```

```yaml
app:
  payment:
    url: http://pay
    timeout: 5s
```

| | `.properties` | `.yml` |
|---|---|---|
| Style | `a.b=c` | Indentation |
| Lists / maps | Awkward | Natural |
| Pick one | Either is fine — don’t mix both for the **same** key (surprise override) |

In Spring Boot, both `.properties` and `.yaml` files are supported for externalized configuration, but the **priority order** is important.

**Priority rules**

- `application.properties` and `application.yml` are both valid.
- If both exist in the **same location**, **`.properties` takes precedence over `.yaml` / `.yml`**.
- Spring Boot’s Environment abstraction loads configuration in a defined order (called the **PropertySource** order).

`.yml` and `.yaml` are both YAML. Prefer **one** format for the app so it is obvious who won.

---

## 2. Profiles

**Profile** = named Environment slice: `dev`, `prod`, `test`.

| Piece | Role |
|---|---|
| `application.yml` | Always loaded (defaults) |
| `application-dev.yml` | Loaded when profile **`dev`** is active |
| `spring.profiles.active=dev` | Activate (file, env, CLI) |
| `@Profile("dev")` | This **bean** only if `dev` is on |

```yaml
# application-dev.yml
app:
  payment:
    url: http://localhost:9999
```

```java
@Component
@Profile("dev")
public class FakePaymentClient implements PaymentClient { }
```

```text
java -jar app.jar --spring.profiles.active=dev
# or  SPRING_PROFILES_ACTIVE=dev
```

Active profile **overrides** matching keys from `application.yml`.

**Scenario — `@Profile("dev")` does not load `application-dev.yml`**

They both **listen** to “is `dev` active?” They do **not** trigger each other.

| | What it does |
|---|---|
| `spring.profiles.active=dev` | **Activates** the profile |
| `application-dev.yml` | Loaded **because** `dev` is active (file name) |
| `@Profile("dev")` on a class | That **bean** only if `dev` is active |

```text
--spring.profiles.active=dev
        ├── Boot loads application-dev.yml
        └── @Profile("dev") beans are registered
```

- Only `@Profile("dev")`, profile **not** active → bean skipped; `application-dev.yml` **not** loaded.  
- Only `spring.profiles.active=dev`, no `@Profile` → **yml still loads**; other beans unchanged.

**KT line:** active profile drives the **file**. `@Profile` only **gates beans**.

---

## 3. `@Value` vs `@ConfigurationProperties`

```java
@Value("${app.payment.url}")
String url;                    // one key — OK for a flag

@ConfigurationProperties(prefix = "app.payment")
public class PaymentProps {    // group — prefer this
    private String url;
    private Duration timeout;
}
```

Need `@EnableConfigurationProperties(PaymentProps.class)` or `@ConfigurationPropertiesScan` / `@Component` on the props class.

| | `@Value` | `@ConfigurationProperties` |
|---|---|---|
| Best for | One property | A **prefix** group |
| Relaxed names | Limited | `APP_PAYMENT_URL` → `url` |
| Validation | Awkward | `@Validated` + Jakarta constraints |
| IDE / metadata | Weak | `spring-boot-configuration-processor` |

---

## 4. Externalized config (who wins)

Boot **overlays** sources. **Later / higher wins.**

```text
default application.yml
    ← application-{profile}.yml
        ← OS env vars     (APP_PAYMENT_URL)
            ← command line  (--app.payment.url=...)
```

Interview: **CLI > env > profile file > base file**.  
(There’s more — random, `config/` folder — remember the four above.)

K8s: inject env vars; don’t bake prod URLs into the JAR.

---

## 5. Conditional beans (again)

Same `@ConditionalOn*` as auto-config, now on **your** beans:

```java
@Bean
@ConditionalOnProperty(name = "app.payment.enabled", havingValue = "true")
PaymentClient paymentClient() { ... }

@Bean
@Profile("dev")
PaymentClient fakePayment() { ... }
```

| | |
|---|---|
| `@Profile("dev")` | Profile is active |
| `@ConditionalOnProperty` | Key/value in Environment |
| Missing property | `@ConditionalOnProperty` **skips** the bean (default); app still starts |

---

## 30-second pitch

> Environment is ready **before** `refresh()`. YAML/properties + profiles + env/CLI fill it. Prefer **`@ConfigurationProperties`** for groups. Profile-specific files and CLI override defaults. `@Profile` / `@ConditionalOnProperty` decide which beans exist.
