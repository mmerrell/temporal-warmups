import asyncio
import random
import time

from temporalio import activity
from database import user_db
from models import User

@activity.defn
async def create_user_record(user: User) -> str:
    """Create user in database"""
    print(f"Creating user record for {user.username}...")
    await asyncio.sleep(1)  # Simulate database write

    # Simulate occasional database failures
    if random.random() < 0.1:
        raise Exception("Database connection timeout")

    user_id = user_db.create_user(user.email, user.username, user.password)
    print(f"✓ User created with ID: {user_id}")
    return user_id

@activity.defn
async def send_welcome_email(user: User):
    """Send welcome email to new user"""
    print(f"Sending welcome email to {user.email}...")
    await asyncio.sleep(0.8)  # Simulate email sending

    # Simulate occasional email service failures
    if random.random() < 0.15:
        raise Exception("Email service unavailable")

    return True

@activity.defn
async def send_verification_email(user: User, user_id: str):
    """Send verification link"""
    print(f"Sending verification email to {user.email}...")
    await asyncio.sleep(0.8)

    # Simulate occasional email service failures
    if random.random() < 0.15:
        raise Exception("Email service unavailable")

    verification_token = f"token_{user_id}_{int(time.time())}"
    print(f"✓ Verification email sent with token: {verification_token}")
    return verification_token