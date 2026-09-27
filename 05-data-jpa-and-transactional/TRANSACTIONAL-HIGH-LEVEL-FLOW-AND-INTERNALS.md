# Transactional High Level Flow & Internals

## High-Level Flow

```text
Client Request
      │
      ▼
   Proxy (AOP)
      │
      ▼
Transaction Interceptor → TransactionAspectSupport
      │
      ▼
Transaction Manager → PlatformTransactionManager
      │
      ▼
Connection / EntityManager
      │
      ▼
Business Logic
      │
      ▼
Commit / Rollback
      │
      ▼
Response
```

When a method is annotated with `@Transactional`, Spring wraps it in a proxy.

The proxy intercepts the call, delegates to a transaction interceptor, and coordinates with a transaction manager to open a connection or `EntityManager`.

The business logic runs inside this transactional context, and Spring decides whether to commit or rollback based on the outcome.

This ensures atomicity and consistency without manually handling transactions.

---

## ACID Properties

- **Atomicity**: All operations in a transaction are completed successfully. If any operation fails, the transaction is rolled back.
- **Consistency**: Ensures the database state is consistent before and after the transaction.
- **Isolation**: Ensures parallel transactions do not interfere with each other.
- **Durability**: Ensures committed transactions are never lost despite system failures.

---

## Internals

Spring Transaction Management uses AOP.

- It looks for methods annotated with `@Transactional` using pointcut expressions.
- Once the pointcut matches, it runs an **Around advice**.
- This advice runs before and after the actual method call.

### Execution flow

```text
@Transactional method call
        |
        v
TransactionAspectSupport.invokeWithinTransaction()
        |
        ├── createTransactionIfNecessary()
        ├── proceedWithInvocation() -> actual method body
        └── after method completion
              ├── commitTransactionAfterReturning()  (success)
              └── completeTransactionAfterThrowing() (exception -> rollback)
```

When controller calls a `@Transactional` method, the call is first delegated to `TransactionAspectSupport`.

---

## Transaction Context

A transaction context contains:

- transaction manager
- transaction propagation
- isolation levels
- transaction timeout
- read-only transactions

---

## Transaction Manager

The transaction manager is the core component responsible for committing and rolling back transactions.

### Types

#### Declarative transaction management

- Done via `@Transactional`
- Spring Boot chooses an appropriate transaction manager automatically
- You can also provide an explicit one

```java
@Bean
public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
}
```

Then use:

```java
@Transactional(transactionManager = "transactionManager")
```

#### Programmatic transaction management

- Handles transactions manually through code
- More flexible but harder to maintain
- Usually done with `TransactionTemplate` or custom transaction manager

### Hierarchy

```text
TransactionManager (interface)
    |
    └── PlatformTransactionManager (interface)
            |
            └── AbstractPlatformTransactionManager
                    |
                    ├── DataSourceTransactionManager
                    ├── HibernateTransactionManager
                    ├── JpaTransactionManager
                    └── JtaTransactionManager
```

---

## Transaction Propagation

Propagation defines how transactions behave when one transactional method calls another.

Available values:

- `REQUIRED`
- `SUPPORTS`
- `MANDATORY`
- `REQUIRES_NEW`
- `NOT_SUPPORTED`
- `NEVER`
- `NESTED`

Default:

```java
@Transactional(propagation = Propagation.REQUIRED)
```

### `REQUIRED`

- If a transaction already exists, join it.
- If not, start a new one.

```text
Non-Transactional method -> Transactional method [newTransaction = true]
Transactional method -> Transactional method [newTransaction = false]
```

`newTransaction = true` means a new transaction is started.
`newTransaction = false` means the method is joining the current transaction from the caller.

### `REQUIRES_NEW`

- Always creates a new transaction.
- Suspends the existing transaction before starting a new one.

```text
Non-Transactional method -> Transactional method [newTransaction = true]
Transactional method -> Transactional method [newTransaction = true]
```

This means the method will not join the existing transaction; it creates a fresh one even if another transaction is active.

### `MANDATORY`

- Must run inside an existing transaction.
- It will not create a new transaction.
- If no transaction exists, it throws `IllegalTransactionStateException`.

```text
Transactional method -> Transactional method [newTransaction = false]
Non-Transactional method -> Transactional method [throws IllegalTransactionStateException]
```

### `NESTED`

- Creates a nested transaction if supported.
- Uses a savepoint.
- A failure after the savepoint can roll back only the nested block.

```text
Non-Transactional method -> Transactional method [newTransaction = true]
Transactional method -> Transactional method [newTransaction = true]
```

This behaves like a nested transaction inside the same transaction flow and can support partial rollback.

### `SUPPORTS`

- Runs with a transaction if one exists.
- Otherwise runs without one.

```text
Non-Transactional method -> Transactional method [transaction is null]
Transactional method -> Transactional method [join existing transaction]
```

### `NOT_SUPPORTED`

- Suspends the current transaction and runs without one.

```text
Non-Transactional method -> Transactional method [transaction is null]
Transactional method -> Transactional method [transaction is null]
```

### `NEVER`

- Must not run inside a transaction.
- If a transaction exists, it throws `IllegalTransactionStateException`.

```text
Non-Transactional method -> Transactional method [transaction is null]
Transactional method -> Transactional method [throws IllegalTransactionStateException]
```

---

## Transaction Isolation Levels

Multiple transactions may run in parallel. Isolation controls how much they interfere with each other.

### Dirty Read

Transaction A updates a row while Transaction B reads it before A commits.

### Non-Repeatable Read

A transaction reads the same row twice and gets different values because another transaction updated it in between.

### Phantom Read

A transaction reads a range of rows and gets different results because another transaction inserted or deleted rows in between.

### Isolation options

- `READ_UNCOMMITTED`
- `READ_COMMITTED`
- `REPEATABLE_READ`
- `SERIALIZABLE`

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
```

### Meaning

- `READ_UNCOMMITTED`: lowest isolation; dirty reads possible.
- `READ_COMMITTED`: prevents dirty reads; non-repeatable/phantom reads may still occur.
- `REPEATABLE_READ`: prevents dirty and non-repeatable reads; phantom reads may remain.
- `SERIALIZABLE`: strongest isolation; most consistent but slower and more locking.

---

## Transaction Timeout and Read-Only

### Timeout

```java
@Transactional(timeout = 30)
```

Sets the maximum time allowed for the transaction.

### Read-only

```java
@Transactional(readOnly = true)
```

Used for read operations where writes are not expected.

---

## Important Interview Scenarios

### Scenario 1: Self-invocation problem

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        saveAuditLog();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog() {
        // audit entry
    }
}
```

This does not create a new transaction because the method call is inside the same class.

Spring proxy does not intercept self-invocation.

**Fix:** Move the inner operation to another Spring bean or call it through the Spring proxy.

---

### Scenario 2: Rollback on checked exceptions

By default, Spring only rolls back for unchecked exceptions (`RuntimeException`).

```java
@Transactional
public void processOrder() throws IOException {
    orderRepo.save(order);
    throw new IOException("Network failure");
}
```

This will not rollback unless configured explicitly.

Use:

```java
@Transactional(rollbackFor = Exception.class)
public void processOrder() throws IOException {
    orderRepo.save(order);
    throw new IOException("Now rollback will happen");
}
```

---

## Summary

> Spring Data JPA and `@Transactional` work together to provide reliable database operations. `@Transactional` creates a transaction boundary, and the Spring proxy + transaction interceptor coordinate with the transaction manager to commit or rollback based on the method result.

This is a very common interview topic because it directly relates to **atomicity**, **consistency**, and proper **database behavior** in real-world applications.

---

## Quick Recall

- `@Transactional` = DB transaction boundary
- Proxy + AOP = intercept call
- `TransactionManager` = commit/rollback
- `REQUIRED` = default
- `REQUIRES_NEW` = new independent transaction
- `NESTED` = nested savepoint-based rollback
- `MANDATORY` = must have active transaction
- `NEVER` = must not have transaction
- Rollback default = `RuntimeException`
- Checked exceptions require `rollbackFor = Exception.class`
