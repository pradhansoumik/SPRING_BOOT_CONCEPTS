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

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

- Always creates a new transaction.
- Suspends the existing transaction before starting a new one.

```text
Non-Transactional method -> Transactional method [newTransaction = true]
Transactional method -> Transactional method [newTransaction = true]
```

This means the method will not join the existing transaction; it creates a fresh one even if another transaction is active.

### `MANDATORY`

```java
@Transactional(propagation = Propagation.MANDATORY)
```

- Must run inside an existing transaction.
- It will not create a new transaction.
- If no transaction exists, it throws `IllegalTransactionStateException`.

```text
Transactional method -> Transactional method [newTransaction = false]
Non-Transactional method -> Transactional method [throws IllegalTransactionStateException]
```

### `NESTED`

```java
@Transactional(propagation = Propagation.NESTED)
```

- Creates a nested transaction if supported.
- Uses a savepoint.
- A failure after the savepoint can roll back only the nested block.

```text
Non-Transactional method -> Transactional method [newTransaction = true]
Transactional method -> Transactional method [newTransaction = true]
```

This behaves like a nested transaction inside the same transaction flow and can support partial rollback.

### `SUPPORTS`

```java
@Transactional(propagation = Propagation.SUPPORTS)
```

- Runs with a transaction if one exists.
- Otherwise runs without one.

```text
Non-Transactional method -> Transactional method [transaction is null]
Transactional method -> Transactional method [join existing transaction]
```

### `NOT_SUPPORTED`

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
```

Definition:
- If you want to skip the transaction for a particular method, use this propagation.
- It suspends the current transaction and runs the method without one.

Usage:
- Useful for non-critical side effects such as sending notifications, logging, or calling external systems where a transaction is not needed.

```text
Non-Transactional method -> Transactional method [transaction is null]
Transactional method -> Transactional method [transaction is null]
```

### `NEVER`

```java
@Transactional(propagation = Propagation.NEVER)
```

Definition:
- This propagation is used when a method must never run inside a transaction.
- If a transaction already exists, it throws `IllegalTransactionStateException`.
- Here, we don't need any transactions to be happened.

Usage:
- Use it for read-only checks, monitoring, health checks, or any method that should always execute without transaction context.

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

Scenario:

```text
User-1 is trying to fetch one specific data [Transaction A] = result abc, user-2 updates the same data [Transaction B].
user-1 is trying to fetch the same data [within same Transaction A] = result xyz

so, in a same Transaction if user is getting different value for the same query - it is called non-repeatable Txn & it is for single row.
```

### Phantom Read

kind of similar to non-repeatable but it deals with multiple records.

```text
select * from book where price < 100;

in Transaction B some user has inserted new data with price less than 100;
then if you select * from book where price < 100 = you will get different data (multiple rows)
```

This means the same query in the same transaction can return a different set of rows because another transaction inserted or deleted rows in between.

`Each Database has its own isolation level.`

### Isolation options

- `READ_UNCOMMITTED`
- `READ_COMMITTED`
- `REPEATABLE_READ`
- `SERIALIZABLE`

```text
syntax = @Transactional(propagation = Propagation.REQUIRED, isolation = READ_UNCOMMITTED)
```

```text
isolation = READ_UNCOMMITTED

    > when we are dealing with read only data we can use this.

isolation = READ_COMMITTED

    > dirty reads can be prevented but non-repeatable & phantom reads can occur problem.
    > can acquire locks [shared & exclusive lock]

        shared lock - is partial lock on read operation for particular row.
        exclusive lock - hard lock while updating the data.

isolation = REPEATABLE

    > dirty reads, non-repeatable can be prevented but phantom reads can occur problem.
    > it will acquire shared lock for read only data but for the entire transactions.

isolation = SERIALIZABLE

    > all the problems will be solved here.
    > it acquires lock for the entire range of the transaction. so no other thread can interrupt.
```

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
