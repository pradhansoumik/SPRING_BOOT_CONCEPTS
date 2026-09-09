# Auto-config demo — connect the dots

**Theory:** `../MAVEN-CLASSPATH-AUTOCONFIG.md`  
**Run:** `mvn -f 01-fundamentals/spring-boot-internals/autoconfig-demo/pom.xml spring-boot:run`

Watch the console block `after refresh() (ApplicationRunner)`.

```text
pom.xml
  ├── starter-web  →  DispatcherServlet + Tomcat on classpath
  └── gson         →  Gson class on classpath (normal JAR, not a starter)
          ↓
SpringApplication.run() → refresh()
          ↓
Boot reads AutoConfiguration.imports (Boot’s file + ours)
          ↓
@ConditionalOnClass / @ConditionalOnProperty / @ConditionalOnMissingBean
          ↓
beans created  →  ApplicationRunner prints what exists
```

Main class is in `...app` so **scan does not see** `GreetAutoConfiguration`. That bean exists **only** via our `AutoConfiguration.imports`.

---

## What you should see (default)

| Probe | Why |
|---|---|
| `Gson` class PRESENT + `gson` bean | Maven added the JAR → Boot’s `GsonAutoConfiguration` |
| `DispatcherServlet` bean | `starter-web` on classpath |
| `Greeter` bean | Our `.imports` + `demo.greet.enabled=true` |

---

## Three flips (one at a time, then revert)

**1. Classpath (Maven)** — comment out the `gson` dependency in `pom.xml`, rerun.  
`Gson` class ABSENT → **no** `gson` bean. Boot still listed `GsonAutoConfiguration`; `@ConditionalOnClass` failed.

**2. Property (class-level condition)** — `demo.greet.enabled=false`, rerun.  
**No** `Greeter` bean. File still lists our auto-config; `@ConditionalOnProperty` failed.

**3. Missing bean** — add this in `app` package, rerun:

```java
@Component
class MyGreeter extends Greeter {
    MyGreeter() { super("hello from MY @Component"); }
}
```

Auto-config **skips** its `@Bean` (`@ConditionalOnMissingBean`). Runner still finds a `Greeter`.

**4. (Optional) `.imports` is the switch** — delete the line in  
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, rerun.  
**No** `Greeter` even if `enabled=true`: class is on disk, but Boot never loads it (not scanned).

---

## One sentence

Maven fills the **classpath**; Boot at **`refresh()`** reads **`.imports`** and creates beans only when **conditions** match.
