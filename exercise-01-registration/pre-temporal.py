# registration_service.py
import time
import random
import smtplib
from email.mime.text import MIMEText


class RegistrationService:
    def __init__(self):
        self.email_sent_count = 0

    def validate_user_data(self, email, username, password):
        """Validate user input"""
        print(f"Validating user data for {email}...")
        time.sleep(0.5)  # Simulate validation work

        if not email or '@' not in email:
            raise ValueError("Invalid email address")
        if len(password) < 8:
            raise ValueError("Password must be at least 8 characters")

        print("✓ Validation passed")
        return True

    def create_user_record(self, email, username, password):
        """Create user in database"""
        print(f"Creating user record for {username}...")
        time.sleep(1)  # Simulate database write

        # Simulate occasional database failures
        if random.random() < 0.1:
            raise Exception("Database connection timeout")

        user_id = f"user_{len(self.users_db) + 1}"
        self.users_db[user_id] = {
            'email': email,
            'username': username,
            'password': password,  # In real life, hash this!
            'verified': False,
            'created_at': time.time()
        }

        print(f"✓ User created with ID: {user_id}")
        return user_id

    def send_welcome_email(self, email, username):
        """Send welcome email to new user"""
        print(f"Sending welcome email to {email}...")
        time.sleep(0.8)  # Simulate email sending

        # Simulate occasional email service failures
        if random.random() < 0.15:
            raise Exception("Email service unavailable")

        self.email_sent_count += 1
        print(f"✓ Welcome email sent (total sent: {self.email_sent_count})")
        return True

    def send_verification_email(self, email, user_id):
        """Send verification link"""
        print(f"Sending verification email to {email}...")
        time.sleep(0.8)

        # Simulate occasional email service failures
        if random.random() < 0.15:
            raise Exception("Email service unavailable")

        verification_token = f"token_{user_id}_{int(time.time())}"
        self.email_sent_count += 1
        print(f"✓ Verification email sent with token: {verification_token}")
        return verification_token

    def register_user(self, email, username, password):
        """Main registration flow - runs all steps in sequence"""
        print(f"\n{'=' * 60}")
        print(f"Starting registration for {username} ({email})")
        print(f"{'=' * 60}\n")

        try:
            # Step 1: Validate
            self.validate_user_data(email, username, password)

            # Step 2: Create user
            user_id = self.create_user_record(email, username, password)

            # Step 3: Send welcome email
            self.send_welcome_email(email, username)

            # Step 4: Send verification email
            verification_token = self.send_verification_email(email, user_id)

            print(f"\n{'=' * 60}")
            print(f"✓ Registration complete for {username}!")
            print(f"User ID: {user_id}")
            print(f"Verification token: {verification_token}")
            print(f"{'=' * 60}\n")

            return {
                'success': True,
                'user_id': user_id,
                'verification_token': verification_token
            }

        except Exception as e:
            print(f"\n{'=' * 60}")
            print(f"✗ Registration failed for {username}: {str(e)}")
            print(f"{'=' * 60}\n")
            return {
                'success': False,
                'error': str(e)
            }


# Usage example
if __name__ == "__main__":
    service = RegistrationService()

    # Try registering a few users
    result1 = service.register_user(
        email="alice@example.com",
        username="alice",
        password="secure123"
    )

    result2 = service.register_user(
        email="bob@example.com",
        username="bob",
        password="password456"
    )

    # This one should fail (duplicate username)
    result3 = service.register_user(
        email="alice2@example.com",
        username="alice",
        password="another_password"
    )

    print("\n\nFinal Results:")
    print(f"Users in database: {len(service.users_db)}")
    print(f"Emails sent: {service.email_sent_count}")