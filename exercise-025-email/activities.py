import random
import secrets
import time

from temporalio import activity

@activity.defn
async def generate_token(email) -> str:
    """Generate a unique verification token"""
    print(f"Generating token for {email}...")
    time.sleep(0.3)

    token = secrets.token_urlsafe(32)

    print(f"✓ Token generated: {token[:16]}...")
    return token

@activity.defn
async def send_verification_email(email, token) -> str:
    """Send verification email"""
    print(f"Sending verification email to {email}...")
    time.sleep(0.5)

    # Simulate email service failures (10% failure rate)
    if random.random() < 0.1:
        raise Exception("Email service temporarily unavailable")

    verification_link = f"https://example.com/verify?token={token}"

    print(f"✓ Verification email sent to {email}")
    print(f"  Link: {verification_link[:50]}...")
    return verification_link
