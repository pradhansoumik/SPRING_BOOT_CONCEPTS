# Interview Revision Sheet: Data JPA & `@Transactional`

## 1. Quick definitions

- **Spring Data JPA**: reduces boilerplate repository code and works with JPA/Hibernate.
- **`@Entity`**: maps a Java class to a database table.
- **`@Id`**: primary key field.
- **`JpaRepository`**: CRUD repository abstraction.
- **`@Transactional`**: marks a method to run inside a database transaction.

---

## 2. Most asked questions

### Q1. What is `@Transactional`?

`@Transactional` tells Spring to wrap a method in a transaction so that the DB operations either commit together or rollback together.

### Q2. Why do we use `@Transactional` in service layer?

Because service methods usually represent a business unit of work, such as transfer, placeOrder, or createAccount.

### Q3. How does Spring implement it?

Spring uses AOP proxying and a transaction interceptor.

```text
method call
   ↓
Spring proxy
   ↓
Transaction interceptor
   ↓
TransactionManager
   ↓
DB transaction
```

### Q4. What is the default propagation?

`REQUIRED`.

If a transaction exists, the method joins it. If not, a new one is started.

### Q5. What happens on exception?

By default, rollback happens for `RuntimeException`.

If you want rollback on checked exceptions, use:

```java
@Transactional(rollbackFor = Exception.class)
```

### Q6. Why does self-invocation not work?

Because the method call is inside the same class. The Spring proxy does not intercept it.

### Q7. What is the difference between `@Entity` and `DTO`?

- `@Entity`: database model
- DTO: API transfer object

### Q8. What is the role of `JpaRepository`?

It provides CRUD methods such as:

- `save()`
- `findById()`
- `findAll()`
- `delete()`

### Q9. What is a transaction manager?

It is the component that manages the lifecycle of transactions, including commit and rollback.

### Q10. What is the job of `@Transactional(readOnly = true)`?

It marks the transaction as read-only, useful for fetch operations.

---

## 3. Interview-ready summary

> Spring Data JPA helps you access the database with repository interfaces. `@Transactional` ensures that a business operation behaves like a single atomic unit. If anything fails, the transaction rolls back and the database remains consistent.

---

## 4. High-confidence answer pattern

Use this short answer in interviews:

> In Spring Boot, service methods are often marked with `@Transactional` to define a database transaction boundary. Spring uses AOP proxies and a `TransactionManager` to begin a transaction before the method runs, commit it on success, and rollback on failure. This ensures atomicity and consistency for operations like money transfer or order placement.

---

## 5. Important code patterns to remember

```java
@Transactional
public void createOrder() {
    // save order
    // update stock
    // save audit log
}
```

```java
@Transactional(rollbackFor = Exception.class)
public void processPayment() throws Exception {
    // critical DB writes
}
```

```java
@Transactional(readOnly = true)
public List<User> findAllUsers() {
    return userRepository.findAll();
}
```

---

## 6. Very short focus list

- `@Transactional` = atomic unit of work
- proxy + AOP = transaction interception
- `REQUIRED` = default propagation
- `RuntimeException` = rollback by default
- checked exception = rollback only if configured
- self-invocation = no proxy interception
- service layer = best place for transaction boundaries
- repository = DB interaction
- `@Entity` = persistence model
