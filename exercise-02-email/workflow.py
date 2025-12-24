# email_verification.py
from datetime import timedelta
from temporalio import workflow
from temporalio.common import RetryPolicy

with workflow.unsafe.imports_passed_through():
    from activities import (
        send_verification_email, generate_token
    )

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

@workflow.defn
class EmailVerificationWorkflow:
    @workflow.run
    async def run(self, email) -> dict:
        """Main flow"""
        workflow.logger.info(f"\n{'=' * 50}")
        workflow.logger.info(f"Starting verification for {email}")
        workflow.logger.info(f"{'=' * 50}\n")

        try:
            # Step 1: Generate token
            token = await workflow.execute_activity(
                generate_token,
                args=[email],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            # Step 2: Send email
            link = await workflow.execute_activity(
                send_verification_email,
                args=[email, token],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

            workflow.logger.info(f"\n{'=' * 50}")
            workflow.logger.info(f"✓ Verification initiated for {email}: {link}")
            workflow.logger.info(f"{'=' * 50}\n")

        except Exception as e:
            workflow.logger.info(f"\n{'=' * 50}")
            workflow.logger.info(f"✗ Verification failed for {email}: {e}")
            workflow.logger.info(f"{'=' * 50}\n")

        return {
            'success': True,
            'email': email,
            'token': token,
            'link': link
        }