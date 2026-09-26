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
