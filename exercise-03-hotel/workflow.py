# hotel_reservation.py - Original messy implementation
from datetime import timedelta, datetime

from temporalio.common import RetryPolicy
from temporalio.exceptions import ApplicationError

from database import ReservationRequest, ReservationResult, HotelData

from temporalio import workflow

with workflow.unsafe.imports_passed_through():
    from activities import (
        check_room_availability,
        collect_payment,
        assign_room,
        send_email_notification,
        send_sms_notification,
        front_desk_confirmation
    )

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

@workflow.defn
class HotelReservationWorkflow:

    @workflow.run
    async def run(self, res: ReservationRequest):
        """
        Main reservation processing flow - MESSY!
        This is a monolithic function that does everything.
        """
        workflow.logger.info(f"\n{'=' * 70}")
        workflow.logger.info(f"Processing reservation for {res.guest_name}")
        workflow.logger.info(f"Room type: {res.room_type}")
        workflow.logger.info(f"Check-in: {res.check_in}, Check-out: {res.check_out}")
        workflow.logger.info(f"{'=' * 70}\n")

        # Not sure if the "calculate_nights" piece should be a different method call
        reservation_id = self.generate_reservation_id(res)
        nights = self.validate_res_request(res)
        workflow.logger.info(f"Calculating price for {nights} night(s)...")

        # Check room availability before calculating price
        workflow.logger.info(f"Checking {res.room_type} room availability...")
        available_rooms = await workflow.execute_activity(
            check_room_availability,
            args=[res],
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=DEFAULT_RETRY_POLICY,
        )
        workflow.logger.info(f"✓ Found {len(available_rooms)} available {res.room_type} room(s)")

        total_price = self.calculate_total_price(res.room_type, nights)
        workflow.logger.info(f"Total price: ${total_price:.2f}")

        payment_id = await workflow.execute_activity(
            collect_payment,
            args=[total_price],
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=DEFAULT_RETRY_POLICY,
        )
        workflow.logger.info(f"✓ Payment processed: {payment_id}")

        # Assign room - JUST FAILS, NO RETRY
        workflow.logger.info(f"Assigning {res.room_type} room...")

        room_number = await workflow.execute_activity(
            assign_room,
            args=[available_rooms],
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=DEFAULT_RETRY_POLICY,
        )
        workflow.logger.info(f"✓ Room {room_number} assigned")

        res_result = ReservationResult(
            success=True,
            reservation_id=reservation_id,
            room_number=room_number,
            total_price=total_price,
            payment_id=payment_id,
            nights=nights,
            error=None
        )

        # Send confirmation email - BASIC TRY/EXCEPT, NO RETRY
        workflow.logger.info(f"Sending confirmation email to {res.guest_email}...")

        try:
            await workflow.execute_activity(
                send_email_notification,
                args=[res, res_result],
                start_to_close_timeout=timedelta(seconds=10),
                retry_policy=DEFAULT_RETRY_POLICY,
            )

        except ApplicationError as e:
            workflow.logger.warn(f"⚠ Warning: Email failed - {e}")
            workflow.logger.warn(f"⚠ Payment succeeded and room assigned, but customer wasn't notified!")
            workflow.logger.warn(f"⚠ Sending SMS as a fallback")

            try:
                await workflow.execute_activity(
                    send_sms_notification,
                    args=[res, res_result],
                    start_to_close_timeout=timedelta(seconds=10),
                    retry_policy=DEFAULT_RETRY_POLICY,
                )
            except ApplicationError as e:
                workflow.logger.warn(f"⚠ Warning: SMS failed - {e}")
                workflow.logger.warn(f"⚠ Alerting front desk staff to notify manually - {e}")
                await workflow.execute_activity(
                    front_desk_confirmation,
                    args=[res, res_result],
                    start_to_close_timeout=timedelta(seconds=10),
                    retry_policy=DEFAULT_RETRY_POLICY,
                )

        workflow.logger.info(f"\n{'=' * 70}")
        workflow.logger.info(f"✓ Reservation {reservation_id} completed")
        workflow.logger.info(f"  Guest: {res.guest_name}")
        workflow.logger.info(f"  Room: {res_result} ({res.room_type})")
        workflow.logger.info(f"  Nights: {nights}")
        workflow.logger.info(f"  Total: ${total_price:.2f}")
        workflow.logger.info(f"{'=' * 70}\n")

        return res_result

    @staticmethod
    def validate_res_request(res: ReservationRequest) -> int:
        if not res.guest_name or len(res.guest_name) < 2:
            raise ValueError("Invalid guest name")

        if not res.guest_email or '@' not in res.guest_email:
            raise ValueError("Invalid email address")

        check_in_date = datetime.fromisoformat(res.check_in)
        check_out_date = datetime.fromisoformat(res.check_out)

        nights = (check_out_date - check_in_date).days
        if nights < 1:
            raise ValueError("Check-out must be after check-in")
        return nights

    @staticmethod
    def calculate_total_price(room_type: str, nights: int) -> float:
        hotel = HotelData()
        base_price = hotel.rates.get(room_type, 100.00)
        total_price = base_price * nights

        # Apply discount if staying 7+ nights
        if nights >= 7:
            total_price *= 0.9  # 10% discount
            workflow.logger.info(f"Applied 7+ night discount: 10% off")

        return total_price

    @staticmethod
    def generate_reservation_id(res: ReservationRequest) -> str:
        return f"RES-{res.guest_email}-{workflow.uuid4().hex[:6]}"
