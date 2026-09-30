# Cross-cutting & Ops

**Scope:** Actuator health/info/metrics, Logging/MDC, `@Async` with an executor, `@Scheduled`, and AOP for logging/timing.

---

## 1. Actuator

Spring Boot Actuator exposes operational endpoints for application health and metrics.

Add `spring-boot-starter-actuator`, then configure the endpoints to expose:

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.info.env.enabled=true
info.app.name=orders-api
```

Examples:

```text
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

Expose only the endpoints the application needs, and secure them appropriately outside local development.

---

## 2. Logging and MDC

Use the application logger instead of `System.out.println`:

```java
private static final Logger log = LoggerFactory.getLogger(OrderService.class);

log.info("Processing orderId={}", orderId);
```

**MDC (Mapped Diagnostic Context)** adds per-request values, such as a correlation ID, to log entries.

```java
MDC.put("correlationId", correlationId);
try {
    log.info("Handling request");
} finally {
    MDC.remove("correlationId");
}
```

Always clear MDC values when the request finishes because servlet threads are reused. MDC context does not automatically move to `@Async` executor threads.

---

## 3. `@Async` with an executor

`@Async` runs a method on a separate executor thread. Configure a bounded executor instead of relying on an unbounded default.

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("appTaskExecutor")
    public Executor appTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("app-async-");
        executor.initialize();
        return executor;
    }
}
```

```java
@Async("appTaskExecutor")
public CompletableFuture<Void> sendNotification() {
    // asynchronous work
    return CompletableFuture.completedFuture(null);
}
```

Enable async processing with `@EnableAsync`. Calls must go through the Spring proxy; self-invocation does not trigger `@Async`. This section covers Spring-managed executor async work, not Kafka async processing.

---

## 4. `@Scheduled`

`@Scheduled` runs a Spring bean method on a schedule. Enable scheduling with `@EnableScheduling`.

```java
@Component
public class CleanupJob {

    @Scheduled(fixedDelay = 60_000)
    public void cleanUp() {
        // runs again 60 seconds after the previous run completes
    }
}
```

- `fixedRate`: schedule runs based on the start time of the previous run.
- `fixedDelay`: wait the specified time after the previous run completes.
- `cron`: run according to a cron expression.

---

## 5. AOP for logging/timing

An aspect can apply logging or timing around selected method calls without repeating that code in every method.

```java
@Aspect
@Component
public class TimingAspect {

    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Around("execution(* com.example..service..*(..))")
    public Object timeServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
            log.info("{} took {} ms", joinPoint.getSignature(), elapsedMillis);
        }
    }
}
```

AOP advice runs through Spring proxies, so proxy limitations such as self-invocation apply. Avoid logging secrets or sensitive request data.

---

## Quick recall

- **Actuator:** operational endpoints such as health, info, and metrics.
- **MDC:** attach request context to logs; clear it when finished.
- **`@Async`:** executor-backed asynchronous method invocation through a Spring proxy.
- **`@Scheduled`:** time-based method execution.
- **AOP:** apply cross-cutting logging or timing around selected methods.
