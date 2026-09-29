# Data JPA + `@Transactional` demo

**Theory:** [`../DATA-JPA-TRANSACTIONAL.md`](../DATA-JPA-TRANSACTIONAL.md)

```bash
mvn -f 05-data-jpa-and-transactional/data-jpa-demo/pom.xml spring-boot:run
```

This demo uses **H2 in-memory DB** and shows the real transaction flow through a simple REST API.

```text
AccountController
  ├─ POST /api/accounts/seed
  └─ POST /api/accounts/transfer
  └─ POST /api/accounts/transfer-without-transaction

AccountService.transferMoney()
  ├─ load from accountRepository
  ├─ validate funds
  ├─ update balances
  ├─ save both accounts
  └─ commit if success, rollback if exception
```

## What to observe

- The app starts with H2 and creates the table automatically.
- `seedData()` creates two sample accounts.
- `transferMoney()` runs inside `@Transactional`.
- If amount is too large, method throws and transaction rolls back.
- The controller exposes this as a small API so you can test the flow easily.

## H2 console

1. Start the app and open `http://localhost:8080/h2-console`.
2. Use these connection details:

```text
JDBC URL: jdbc:h2:mem:datajpa-demo
User Name: sa
Password: (leave blank)
```

3. Click **Connect**. The in-memory database is available only while the app is running.

## API endpoints

### 1) Seed accounts

```bash
curl -X POST http://localhost:8080/api/accounts/seed
```

Response:

```json
{
  "message": "Accounts seeded successfully",
  "count": 2
}
```

### 2) Transfer money

```bash
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromId":1,"toId":2,"amount":250}'
```

Response:

```json
{
  "message": "Transfer successful",
  "fromBalance": 750,
  "toBalance": 750
}
```

### 3) Observe failure without a service transaction

Restart the app to clear the in-memory database, then seed the accounts again. Call this endpoint with the same request:

```bash
curl -X POST http://localhost:8080/api/accounts/transfer-without-transaction \
  -H "Content-Type: application/json" \
  -d '{"fromId":1,"toId":2,"amount":250}'
```

The endpoint intentionally returns `500` after the sender update is saved. In the H2 console, run:

```sql
SELECT id, email, balance FROM accounts;
```

The sender will have `750.00`, while the recipient remains at `500.00`. The repository save committed independently, so there is no outer service transaction to roll it back. This endpoint is intentionally unsafe and exists only to demonstrate the partial-update scenario.

## Typical runtime behaviour

```text
1. app starts
2. seed accounts
3. transferMoney(1, 2, 250)
   -> success, both balances updated
4. transferMoney(1, 2, 10000)
   -> throws IllegalArgumentException
   -> transaction rolls back
```

**Key interview idea:** transaction should protect the entire operation, not just one DB call.

## Minimal idea to remember

`@Transactional` is not just a marker — it creates a transaction boundary for the whole method so the database state stays consistent.
