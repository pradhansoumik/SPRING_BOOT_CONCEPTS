# Auto-config annotations (cheat sheet)

**When:** `refresh()` → Boot reads `AutoConfiguration.imports` → these annotations decide *whether* and *what* beans to create.

**Demo:** [`autoconfig-demo/DEMO.md`](autoconfig-demo/DEMO.md)

---

## 1. Entry (your main class)

| Annotation | Role |
|---|---|
| `@SpringBootApplication` | Shortcut = the three below |
| `@SpringBootConfiguration` | This class is **primary** `@Configuration` (not a scanner) |
| `@EnableAutoConfiguration` | Load `AutoConfiguration.imports` + apply conditions |
| `@ComponentScan` | Find **your** `@Component` / `@Service` / `@Configuration` |

`exclude = …` on `@SpringBootApplication` / `@EnableAutoConfiguration` skips a listed auto-config.

---

## 2. Auto-config class (library / our `GreetAutoConfiguration`)

| Annotation | Role |
|---|---|
| `@AutoConfiguration` | Marks a Boot auto-config class (`@Configuration` + ordering). List it in `.imports`. |
| `@AutoConfigureBefore` / `@AutoConfigureAfter` | Run this auto-config before/after another |
| `@Import` | Pull in another `@Configuration` explicitly (no `.imports`) |
| `@Bean` | Define a bean inside that config |

---

## 3. Conditions (the filters)

**Class-level** = skip the whole auto-config. **Method-level** = skip that one `@Bean`.

| Annotation | Fires when                                                                                                      |
|---|-----------------------------------------------------------------------------------------------------------------|
| `@ConditionalOnClass` | Those classes **are** on the classpath (Maven put the JAR there)                                                |
| `@ConditionalOnMissingClass` | Those classes are **not** on the classpath                                                                      |
| `@ConditionalOnBean` | A bean of that type/name **already exists**                                                                     |
| `@ConditionalOnMissingBean` | You did **not** already define that bean (lets you override). Create this bean only if one isn’t there already. |
| `@ConditionalOnSingleCandidate` | Exactly one bean of that type                                                                                   |
| `@ConditionalOnProperty` | Property matches (`demo.greet.enabled=true`)                                                                    |
| `@ConditionalOnWebApplication` | Web app (`SERVLET` / `REACTIVE`)                                                                                |
| `@ConditionalOnNotWebApplication` | `WebApplicationType.NONE`                                                                                       |
| `@ConditionalOnResource` | A classpath file exists (e.g. a config file)                                                                    |
| `@ConditionalOnExpression` | SpEL is true                                                                                                    |

`@ConditionalOnClass` / `MissingClass` → **classpath story**.  
`@ConditionalOnMissingBean` → **your `@Bean` / `@Component` wins**.  
`@ConditionalOnProperty` → **`application.yml` / env switch**.

---

## 4. Properties (bind `application.yml` → Java)

Typical pair inside an auto-config:

```java
@AutoConfiguration
@ConditionalOnClass(Gson.class)
@EnableConfigurationProperties(GsonProperties.class)
public class GsonAutoConfiguration { ... }

@ConfigurationProperties(prefix = "spring.gson")
public class GsonProperties { ... }
```

| Annotation | Role |
|---|---|
| `@ConfigurationProperties` | Bind `prefix.*` to fields |
| `@EnableConfigurationProperties` | Register that properties class as a bean (common on auto-config) |
| `@ConfigurationPropertiesScan` | Scan for `@ConfigurationProperties` (app-level alternative) |
| `@NestedConfigurationProperty` | Nested object inside properties |

---

## Mini map

```text
@EnableAutoConfiguration
    → read .imports
    → @AutoConfiguration + @ConditionalOnClass / Property / …
         → @EnableConfigurationProperties  (optional)
         → @Bean + @ConditionalOnMissingBean
```

**One line:** `.imports` names the class; **conditions** decide if it runs; **`@EnableConfigurationProperties`** binds config; **`@ConditionalOnMissingBean`** lets you replace the default.
