# Data JPA & `@Transactional`

**When:** after the Web MVC layer; this is the step where Spring Boot apps start persisting and updating data.

```text
Controller → Service → Repository (JpaRepository / EntityManager)
                            │
                            └─ Database
```

---

## 1. What is Spring Data JPA?

Spring Data JPA is a Spring project that makes database access easier by reducing boilerplate repository code.

It builds on JPA (Java Persistence API) and Hibernate.

Typical flow:

- define an `@Entity`
- define a repository interface extending `JpaRepository`
- let Spring create the implementation automatically
- call repository methods from the service layer

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    // getters, setters
}
```

```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmail(String email);
}
```

```java
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
```

**Interview line:** Spring Data JPA makes repository code small; Spring creates the implementation for you.

---

## 2. Important JPA concepts

### `@Entity`
Marks a Java class as a JPA entity mapped to a database table.

### `@Table`
Optional table name customization.

```java
@Entity
@Table(name = "users")
public class User {
    // ...
}
```

### `@Id`
Primary key.

### `@GeneratedValue`
Auto-generates the primary key.

### `@Column`
Maps a field to a column and can specify constraints.

```java
@Column(nullable = false, unique = true)
private String email;
```

### Repository
The repository is the DAO-like abstraction for database access.

Common methods from `JpaRepository`:

- `save()`
- `findById()`
- `findAll()`
- `delete()`
- `deleteById()`
- `count()`

#### `save()` vs `saveAndFlush()`

- `save()` persists or updates the entity; JPA flushes changes at commit or when needed.
- `saveAndFlush()` also flushes pending changes immediately, sending SQL to the database.
- So the key difference: **`saveAndFlush()` sends SQL sooner; it does not commit sooner.**
- Both follow the transaction boundary and can be rolled back before commit.

```text
Before flush
    JPA keeps entity changes in the EntityManager's persistence context in application memory.
            |
            v
After flush
    SQL has reached the database, but changes are still uncommitted.
    Other transactions generally cannot see them, depending on isolation level.
            |
            +---- Success ----> Commit: changes become permanent and visible.
            |
            +---- Failure ----> Rollback: changes are discarded and prior state is restored.
```

---

## 3. CRUD flow in Spring Data JPA

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Repository (JpaRepository)
  ↓
Hibernate / JPA provider
  ↓
Database
```

The service usually owns the business logic. The repository only does persistence operations.

---

## 4. `@Transactional` - the core concept

`@Transactional` tells Spring: “this method should run inside a transaction.”

It ensures:

- all DB operations succeed together
- rollback happens on failure
- consistency is maintained for business operations

### Example

```java
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }
}
```

If any exception is thrown, the transaction rolls back automatically.

---

## 5. Why is `@Transactional` needed?

Without a transaction:

- first update might succeed
- second update might fail
- partial data may be left in DB

With `@Transactional`:

- either all changes commit
- or all changes rollback

This is important for money transfer, order creation, payment flow, inventory updates, etc.

---

## 6. How Spring does it

Spring creates a proxy around the bean and intercepts calls to methods annotated with `@Transactional`.

```text
Client calls service.transferMoney(...)
    ↓
Spring proxy intercepts
    ↓
Starts transaction
    ↓
Runs method body
    ↓
Commit if success / rollback if exception
```

This is why `@Transactional` works even if you call the method from another bean, as long as the call goes through Spring-managed proxying.

> Important: calling a `@Transactional` method from within the same class usually does not trigger the proxy again.

---

## 7. `@Transactional` attributes

### `readOnly = true`
Useful for read-only operations.

```java
@Transactional(readOnly = true)
public User getUser(Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

### `rollbackFor` / `noRollbackFor`
Useful when you want custom rollback behavior.

```java
@Transactional(rollbackFor = Exception.class)
public void process() {
    // custom logic
}
```

### `timeout`
Sets transaction timeout.

### `isolation`
Controls transaction isolation level.

### `propagation`
Controls how a transaction is created or reused.

---

## 8. Common propagation types

| Propagation | Meaning |
|---|---|
| `REQUIRED` | default; join existing transaction or create new one |
| `REQUIRES_NEW` | always create a new transaction |
| `MANDATORY` | must run inside an existing transaction |
| `SUPPORTS` | run with or without transaction |
| `NOT_SUPPORTED` | run without transaction |
| `NEVER` | must not run inside a transaction |
| `NESTED` | nested transaction if supported |

**Default is `REQUIRED`** in most cases.

---

## 9. `@Transactional` on class vs method

You can place it on the class or on a method.

```java
@Service
@Transactional
public class OrderService {
    public void createOrder() { ... }
}
```

Method-level annotation overrides class-level behavior.

---

## 10. Common pitfalls

### 1. Runtime exceptions trigger rollback
Checked exceptions do not trigger rollback by default.

```java
@Transactional
public void method() throws Exception {
    // checked exception
}
```

If you want rollback on checked exceptions, use:

```java
@Transactional(rollbackFor = Exception.class)
```

### 2. `@Transactional` on private methods does nothing
The proxy cannot intercept private methods.

### 3. Self-invocation won’t trigger proxy

```java
@Service
public class AService {
    @Transactional
    public void save() {
        // works
    }

    public void doWork() {
        save(); // this is self-invocation, may not trigger proxy
    }
}
```

Call `save()` from another Spring-managed bean instead.

### 4. `@Transactional` does not magically make everything thread-safe
It manages DB transaction boundaries, not general application concurrency.

---

## 11. JPA repository example

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
}
```

Service:

```java
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void createAccount(Account account) {
        accountRepository.save(account);
    }
}
```

---

## 12. Short interview summary

> Spring Data JPA gives you repository abstractions for database work. `@Transactional` ensures that a unit of work is atomic, consistent, isolated, and durable. In practice, service methods use repositories and `@Transactional` to protect business operations like transfers, order placement, and payment flows.

---

## 13. Quick recall table

| Concept | Meaning |
|---|---|
| `@Entity` | JPA entity mapped to table |
| `@Id` | Primary key |
| `@GeneratedValue` | Auto key generation |
| `JpaRepository` | CRUD repository abstraction |
| `@Transactional` | Wrap method in DB transaction |
| Rollback | Revert DB changes on failure |
| `REQUIRED` | Default transaction propagation |

---

## 14. 30-second explanation

> Spring Data JPA helps you interact with the database using repository interfaces, while `@Transactional` makes a service method execute as a database transaction. That means either the whole business operation succeeds or it is rolled back, which is essential for reliable systems like bank transfers, order creation, and payment flows.
