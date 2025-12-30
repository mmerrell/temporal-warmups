# exercise-01-registration Java

## Run the non-temporalized example

```bash
mvn compile exec:java
```

Expected output

```bash
Starting registration for alice (alice@example.com)
============================================================

Validating user data for alice@example.com...
✓ Validation passed
Creating user record for alice...
✓ User created with ID: user_1
Sending welcome email to alice@example.com...
✓ Welcome email sent (total sent: 1)
Sending verification email to alice@example.com...
✓ Verification email sent with token: token_user_1_1767105659268

============================================================
✓ Registration complete for alice!
User ID: user_1
Verification token: token_user_1_1767105659268
============================================================


============================================================
Starting registration for bob (bob@example.com)
============================================================

Validating user data for bob@example.com...
✓ Validation passed
Creating user record for bob...

============================================================
✗ Registration failed for bob: Database connection timeout
============================================================


============================================================
Starting registration for alice (alice2@example.com)
============================================================

Validating user data for alice2@example.com...
✓ Validation passed
Creating user record for alice...
✓ User created with ID: user_2
Sending welcome email to alice2@example.com...
✓ Welcome email sent (total sent: 3)
Sending verification email to alice2@example.com...
✓ Verification email sent with token: token_user_2_1767105663894

============================================================
✓ Registration complete for alice!
User ID: user_2
Verification token: token_user_2_1767105663894
============================================================



Final Results:
Users in database: 2
Emails sent: 4

============================================================
PROBLEMS WITH THIS APPROACH:
============================================================
1. No retry logic - transient failures cause complete failures
2. No durability - if process crashes, all state is lost
3. No visibility - can't see workflow progress in a UI
4. No recovery - can't resume from failure point
5. Manual error handling - error-prone and repetitive
6. All-or-nothing - can't retry just the failed step

Temporal solves all of these problems!
============================================================

```