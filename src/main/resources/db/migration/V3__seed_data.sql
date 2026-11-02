-- V3: sample data. All users' password: password123 (BCrypt, strength 10)

INSERT INTO users (id, username, password_hash) VALUES
    (1, 'alice',   '$2a$10$zRlcONm9stmxgBSdn8Ud2ODp04/HebCSh.a2S3hU6tu4NaLGvSRSW'),
    (2, 'bob',     '$2a$10$zRlcONm9stmxgBSdn8Ud2ODp04/HebCSh.a2S3hU6tu4NaLGvSRSW'),
    (3, 'charlie', '$2a$10$zRlcONm9stmxgBSdn8Ud2ODp04/HebCSh.a2S3hU6tu4NaLGvSRSW');

-- Explicit ids were inserted, so move the identity sequence past them
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));

INSERT INTO account (number, user_id, full_name, is_active, balance) VALUES
    (1000000001, 1, 'Alice Wijaya',    TRUE,  10000000),
    (1000000002, 1, 'Alice Wijaya',    TRUE,    500000),
    (2000000001, 2, 'Bob Herlan',     TRUE,   5000000),
    (3000000001, 3, 'Charlie Hutapea', FALSE,  1000000);  -- inactive