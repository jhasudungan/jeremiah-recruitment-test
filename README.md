# Jeremiah Recruitment Test

Simple service for balance transfer. 

Tech Stack :
- Java 21
- Spring Boot
- PostgreSQL 
- Flyway
- JWT

---

## Table Of Content

1. [Start with Docker (recommended)](#1-quick-start-with-docker-recommended)
2. [Running without Docker](#2-running-without-docker)
3. [Trying the API](#3-trying-the-api)
4. [How a transfer works](#4-how-a-transfer-works)
5. [Error handling](#5-error-handling)

---

## 1. Quick start with Docker (recommended)

**Prerequisite:** Docker Desktop (or Docker Engine with the Compose plugin).

```bash
git clone <repository-url>
cd <repository-folder>
docker compose up --build -d
```

The first build will downloads dependencies and takes a few minutes. Follow the startup with:

```bash
docker compose logs -f <container_id>
```

The app is ready when you see `Successfully applied 3 migrations` followed by `Started ...Application`. 
Database migrations and sample data are applied automatically on startup; there is no separate migration step.

| Service | Address from your machine | Notes |
|---|---|---|
| API | `http://localhost:8080` | |
| PostgreSQL | `localhost:5433` | db `kopnus_test`, user/password `postgres/root` |

PostgreSQL is published on **5433**, not 5432, so it does not clash with a PostgreSQL you may already run locally. 
Inside Docker, the app reaches the database at `postgres:5432` over the compose network.

### Configuration

Ports and the JWT secret can be overridden with environment variables (or a `.env` file next to `docker-compose.yml`):

| Variable | Default | Purpose |
|---|---|---|
| `APP_HOST_PORT` | `8080` | Host port for the API |
| `POSTGRES_HOST_PORT` | `5433` | Host port for PostgreSQL |
| `JWT_SECRET` | development key | Base64 HMAC key, at least 256 bits |

### Connect to Database
You can connect and reach the database through local client like DBEaver through `localhost:5433` by default, or any port you assigned in docker compose

---

## 2. Running without Docker

**Prerequisites**

- JDK 21 or newer
- PostgreSQL 15 or newer (developed and tested on 16)
- Maven 3.9+, or the included Maven wrapper (`mvnw` / `mvnw.cmd`)

**Step 1: create the database**:

```sql
CREATE DATABASE kopnus_test;
```

**Step 2: configure the connection** if yours differs from the defaults. The defaults in `application.properties` are:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/kopnus_test` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `root` |
| `JWT_SECRET` | development key |

**Step 3: run**

```bash
# Linux / macOS
./mvnw spring-boot:run
```

```powershell
# Windows
mvnw.cmd spring-boot:run
```

Or build a jar and run it:

```bash
./mvnw clean package
java -jar target/*.jar
```

Flyway applies the migrations on startup, exactly as in Docker.

---

## 3. Trying the API

### Sample data

All users have the password **`password123`**.

| User | Account | Balance (Rp) | Notes |
|---|---|---|---|
| alice | `1000000001` | 10,000,000 | |
| alice | `1000000002` | 500,000 | second account, same owner |
| bob | `2000000001` | 5,000,000 | |
| charlie | `3000000001` | 1,000,000 | **inactive**, transfers are rejected |

Amounts are in Rupiah (`100000` = Rp100.000).

### Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/login` | none | Exchange username + password for a JWT |
| `GET` | `/api/auth/me` | Bearer | Show the user behind the token |
| `POST` | `/api/transfers` | Bearer | Transfer money |

### Example with curl

**1. Log in**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 900 }
```

Tokens are valid for 15 minutes.

**2. Transfer**

```bash
curl -i -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer <accessToken>" \
  -H "Idempotency-Key: <your_idempotency_key>" \
  -H "Content-Type: application/json" \
  -d '{"originatorAccountNumber":1000000001,"beneficiaryAccountNumber":2000000001,"amount":100000,"remark":"Lunch"}'
```

```
HTTP/1.1 201 Created
Idempotent-Replayed: false
```

```json
{
  "referenceNumber": 1,
  "originatorAccountNumber": 1000000001,
  "beneficiaryAccountNumber": 2000000001,
  "amount": 100000,
  "status": "SUCCESS",
  "remark": "Lunch",
  "transferredAt": "2026-11-02T03:15:42.123Z"
}
```

Send **the exact same request again** and you get `200 OK` with `Idempotent-Replayed: true` and the same `referenceNumber`. No money moves the second time.

### Request rules

| Field | Rule |
|---|---|
| `Idempotency-Key` header | required, max 64 characters, a new value (e.g. a UUID) per new transfer, the easiest way is to use a new generated UUID |
| `originatorAccountNumber` | required, must belong to the logged-in user, must be active |
| `beneficiaryAccountNumber` | required, must exist, must be active, must differ from the originator |
| `amount` | required, greater than 0, not more than the originator's balance |
| `remark` | optional, max 255 characters |

---

## 4. How a transfer works

Everything below runs in **one database transaction**. If any step fails, everything is rolled back: no half-finished transfer can ever exist.

1. **Validate the request**: required fields, positive amount, different accounts.
2. **Lock both accounts** with `SELECT ... FOR UPDATE`, always in **ascending account-number order**.
3. **Authorize**: the logged-in user must own the originator account.
4. **Check the idempotency key**: if this originator already used this key, return the original transfer instead of executing again.
5. **Check business rules**: both accounts active, sufficient balance.
6. **Move the money**: debit the originator, credit the beneficiary.
7. **Record** the `balance_transfer` row and the two `ledger` rows.
8. **Commit.**

You can see the ERD in /erd.png

### Checking a successful transfer in the database

After the transfer from [section 3](#3-trying-the-api) (Rp100.000 from Alice to Bob, on a fresh database), connect to PostgreSQL and run the queries below.

**1. The transfer and its two ledger entries.** Use the `referenceNumber` from the API response:

```sql
SELECT t.reference_number,
       t.idempotency_key,
       l.entry_type,
       l.account_number,
       l.amount,
       l.balance_after
FROM balance_transfer t
JOIN ledger l ON l.transfer_reference_number = t.reference_number
WHERE t.reference_number = 1
ORDER BY l.id;
```

```
 reference_number |           idempotency_key            | entry_type | account_number | amount | balance_after
------------------+--------------------------------------+------------+----------------+--------+---------------
                1 | 3f9c2b1e-7a4d-4c8e-9b2f-1d6e8a0c5f47 | DEBIT      |     1000000001 | 100000 |       9900000
                1 | 3f9c2b1e-7a4d-4c8e-9b2f-1d6e8a0c5f47 | CREDIT     |     2000000001 | 100000 |       5100000
```

Exactly one DEBIT and one CREDIT of the same amount. Sending the same request again (same `Idempotency-Key`) does not add rows.

**2. Balances match the ledger.** For every account that has moved money, the current balance equals the `balance_after` of its latest ledger entry:

```sql
SELECT a.number,
       a.full_name,
       a.balance,
       (SELECT l.balance_after
        FROM ledger l
        WHERE l.account_number = a.number
        ORDER BY l.id DESC
        LIMIT 1) AS last_ledger_balance
FROM account a
ORDER BY a.number;
```

```
   number   |    full_name    | balance | last_ledger_balance
------------+-----------------+---------+---------------------
 1000000001 | Alice Wijaya    | 9900000 |             9900000
 1000000002 | Alice Wijaya    |  500000 |
 2000000001 | Bob Santoso     | 5100000 |             5100000
 3000000001 | Charlie Hutapea | 1000000 |
```

Accounts without transfers show an empty `last_ledger_balance`: their balance is still the opening balance from the sample data.

**3. Account statement.** All movements of one account, oldest first:

```sql
SELECT created_at, entry_type, amount, balance_after, transfer_reference_number
FROM ledger
WHERE account_number = 1000000001
ORDER BY id;
```

### Concurrency

Without protection, two simultaneous transfers from the same account could both read a balance of 10,000,000, both subtract, and one debit would be lost. Three layers prevent this:

1. **Pessimistic row locks** (`PESSIMISTIC_WRITE`, i.e. `SELECT ... FOR UPDATE`). A second transaction on the same account waits until the first commits, then reads the updated balance.
2. **Fixed lock order.** If transfer A→B locked A then B while transfer B→A locked B then A, each would wait for the other forever (deadlock). Always locking the lower account number first makes that impossible.
3. **Safety nets.** `@Version` on `Account` detects any update that bypassed the lock, and the `balance >= 0` constraint rejects an overdraft no matter how it was attempted.

### Duplicate request protection (idempotency)

When a network error hides the response, a client cannot know whether the transfer happened. The `Idempotency-Key` header makes retrying safe:

| Situation | Result |
|---|---|
| New key | Transfer executes: `201 Created` |
| Same key, same payload (a retry) | Original result, no money moves: `200 OK`, `Idempotent-Replayed: true` |
| Same key, different payload | Rejected: `409 IDEMPOTENCY_KEY_REUSED` |

The client generates one key per transfer (e.g. a UUID) and reuses it for every retry of that transfer. This is the same idea as the `X-EXTERNAL-ID` header in the SNAP BI standard.

The key is checked **after** the originator account is locked. Two identical requests arriving at the same moment are therefore processed one after the other, and the second one sees the first one's result. The unique constraint in the database is a backstop that should never fire.

### The ledger

`account.balance` answers *"how much is there now?"*; the ledger answers *"how did it get there?"*.

- **Double entry.** Each transfer produces a DEBIT and a CREDIT of the same amount, so the ledger always balances.
- **Append-only.** Enforced by a database trigger; history cannot be edited or deleted.
- **`balance_after`** stores the balance after each entry, giving an account statement without recalculation.

### Rate limiting

Token buckets (Bucket4j) cap how fast a client can call the API. A bucket holds `capacity` tokens, each request takes one, and tokens refill gradually over the period (30 per minute = one token every 2 seconds), 
so short bursts are allowed but the sustained rate is capped.

| Endpoint | Default limit | Counted per | Purpose |
|---|---|---|---|
| `POST /api/transfers` | 30 per minute | user | Stop a client or stolen token from flooding transfers |
| `POST /api/auth/login` | 10 per minute | client IP + username | Stop password guessing without letting anyone lock out a user from elsewhere |

When a bucket is empty the API answers `429 RATE_LIMIT_EXCEEDED` with a `Retry-After` header. Successful transfers include `X-RateLimit-Remaining`. The check runs before any expensive work (BCrypt, database locks). Limits are configurable (`app.rate-limit.*` in `application.properties`, or environment variables such as `APP_RATELIMIT_TRANSFER_CAPACITY`).
To test or trigger rate limit the best way is repeatedly hit a login request. 
---

## 5. Error handling

Every error, whether a business rule, a validation failure, a missing token or an unknown URL, uses the same JSON shape:

```json
{
  "errorCode": "INSUFFICIENT_BALANCE",
  "message": "Insufficient balance on account 1000000001",
  "details": []
}
```

`errorCode` is stable and intended for clients; `message` is for humans; `details` lists field-level validation errors.

| HTTP | `errorCode` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | A field or header breaks a validation rule (`details` says which) |
| 400 | `MISSING_HEADER` | `Idempotency-Key` is missing |
| 400 | `MALFORMED_REQUEST` | Body is not valid JSON or has wrong types |
| 400 | `INVALID_TRANSFER` | Originator and beneficiary are the same account |
| 401 | `UNAUTHORIZED` | Token missing, invalid or expired |
| 401 | `INVALID_CREDENTIALS` | Wrong username or password |
| 403 | `ACCOUNT_ACCESS_DENIED` | Originator account belongs to another user |
| 404 | `ACCOUNT_NOT_FOUND` | An account number does not exist |
| 404 | `NOT_FOUND` | Unknown URL |
| 409 | `IDEMPOTENCY_KEY_REUSED` | Same key sent with a different payload |
| 409 | `CONCURRENT_UPDATE` | Concurrent modification detected; safe to retry |
| 422 | `ACCOUNT_INACTIVE` | Originator or beneficiary account is inactive |
| 422 | `INSUFFICIENT_BALANCE` | Balance lower than the amount |
| 429 | `RATE_LIMIT_EXCEEDED` | Too many requests; wait the number of seconds in the `Retry-After` header |
| 500 | `INTERNAL_ERROR` | Unexpected error (details are logged, never returned) |