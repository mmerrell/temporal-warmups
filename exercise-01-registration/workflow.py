from datetime import timedelta

from temporalio import workflow
from temporalio.common import RetryPolicy

from models import User

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

from activities import (
    send_verification_email,
    send_welcome_email,
    create_user_record,
)

@workflow.defn
class RegistrationWorkflow:
    # This is not an activity, since it's 100% deterministic given the same input
    def validate_user_data(self, user: User):
        """Validate user input"""
        workflow.logger.info(f"Validating user data for {user.email}...")

        if not user.email or '@' not in user.email:
            raise ValueError("Invalid email address")
        if len(user.password) < 8:
            raise ValueError("Password must be at least 8 characters")

        return True

    @workflow.run
    async def run(self, user: User):
        """Main registration flow - runs all steps in sequence"""
        workflow.logger.info(f"\n{'=' * 60}")
        workflow.logger.info(f"Starting registration for {user.username} ({user.email})")
        workflow.logger.info(f"{'=' * 60}\n")

        try:
            # Step 1: Validate
            self.validate_user_data(user)

            # Step 2: Create user
            user_id = await workflow.execute_activity(
                create_user_record,
                args=[user],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            # Step 3: Send welcome email
            await workflow.execute_activity(
                send_welcome_email,
                args=[user],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            # Step 4: Send verification email
            verification_token = await workflow.execute_activity(
                send_verification_email,
                args=[user, user_id],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            workflow.logger.info(f"\n{'=' * 60}")
            workflow.logger.info(f"✓ Registration complete for {user.username}!")
            workflow.logger.info(f"User ID: {user_id}")
            workflow.logger.info(f"Verification token: {verification_token}")
            workflow.logger.info(f"{'=' * 60}\n")

            return {
                'success': True,
                'user_id': user_id,
                'verification_token': verification_token
            }

        except Exception as e:
            workflow.logger.info(f"\n{'=' * 60}")
            workflow.logger.info(f"✗ Registration failed for {user.username}: {str(e)}")
            workflow.logger.info(f"{'=' * 60}\n")
            return {
                'success': False,
                'error': str(e)
            }
