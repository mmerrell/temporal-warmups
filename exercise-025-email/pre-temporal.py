# email_verification.py
import time
import random
import secrets

class EmailVerifier:
    def __init__(self):
        self.sent_emails = []
        self.tokens = {}

    def generate_token(self, email):
        """Generate a unique verification token"""
        print(f"Generating token for {email}...")
        time.sleep(0.3)

        token = secrets.token_urlsafe(32)
        self.tokens[email] = token

        print(f"✓ Token generated: {token[:16]}...")
        return token

    def send_verification_email(self, email, token):
        """Send verification email"""
        print(f"Sending verification email to {email}...")
        time.sleep(0.5)

        # Simulate email service failures (10% failure rate)
        if random.random() < 0.1:
            raise Exception("Email service temporarily unavailable")

        verification_link = f"https://example.com/verify?token={token}"
        self.sent_emails.append({
            'email': email,
            'link': verification_link,
            'sent_at': time.time()
        })

        print(f"✓ Verification email sent to {email}")
        print(f"  Link: {verification_link[:50]}...")
        return verification_link

    def verify_email(self, email):
        """Main flow"""
        print(f"\n{'=' * 50}")
        print(f"Starting verification for {email}")
        print(f"{'=' * 50}\n")

        try:
            # Step 1: Generate token
            token = self.generate_token(email)

            # Step 2: Send email
            link = self.send_verification_email(email, token)

            print(f"\n{'=' * 50}")
            print(f"✓ Verification initiated for {email}")
            print(f"{'=' * 50}\n")

            return {
                'success': True,
                'email': email,
                'token': token,
                'link': link
            }

        except Exception as e:
            print(f"\n{'=' * 50}")
            print(f"✗ Verification failed for {email}: {e}")
            print(f"{'=' * 50}\n")

            return {
                'success': False,
                'email': email,
                'error': str(e)
            }


# Usage
if __name__ == "__main__":
    verifier = EmailVerifier()

    emails = [
        "alice@example.com",
        "bob@example.com",
        "charlie@example.com"
    ]

    for email in emails:
        result = verifier.verify_email(email)
        time.sleep(1)

    print(f"\n\nSummary:")
    print(f"Emails sent: {len(verifier.sent_emails)}")
    print(f"Tokens generated: {len(verifier.tokens)}")