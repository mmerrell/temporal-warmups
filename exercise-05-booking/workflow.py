from temporalio import workflow
from temporalio.common import RetryPolicy
from temporalio.exceptions import ActivityError

from database import FlightReservationRequest, HotelReservationRequest, CarReservationRequest, TravelBookingRequest
from datetime import timedelta

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

with workflow.unsafe.imports_passed_through():
    from activities import (
        store_booking_request,
        book_flight,
        book_car,
        book_hotel,
        accept_payment,
        cancel_flight,
        cancel_car,
        cancel_hotel
    )

@workflow.defn
class BookingWorkflow:
    @workflow.run
    async def run(self, booking: TravelBookingRequest):
        flight_result = None
        hotel_result = None
        car_result = None

        workflow.logger.info("\n{'=' * 70}")
        workflow.logger.info(f"Processing travel booking for {booking.customer_name}")
        workflow.logger.info(f"Carriage Class:  {booking.carriage_class}")
        workflow.logger.info(f"Route: {booking.departure_city} → {booking.destination_city}")
        workflow.logger.info(f"Dates: {booking.departure_date} to {booking.return_date}")
        workflow.logger.info(f"Passengers: {booking.num_passengers}")
        workflow.logger.info(f"{'=' * 70}\n")

        confirmation_code = booking.confirmation_code

        try:
            workflow.logger.info(f"Persisting booking request: Confirmation code: {confirmation_code}")
            await workflow.execute_activity(
                store_booking_request,
                args=[booking],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )

            # Step 1: Book Flight
            workflow.logger.info("Step 1: Booking flight...")
            flight_request = FlightReservationRequest(
                confirmation_code=confirmation_code,
                carriage_class=booking.carriage_class,
                airline=booking.airline,
                departure_date=booking.departure_date,
                departure_city=booking.departure_city,
                destination_city=booking.destination_city,
            )

            flight_result = await workflow.execute_activity(
                book_flight,
                args=[flight_request],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )
            workflow.logger.info(f"✓ Flight booked: {confirmation_code}")
            workflow.logger.info(f"  Price: ${flight_result.price:.2f}")

            # Step 2: Book Hotel
            workflow.logger.info("\nStep 2: Booking hotel...")

            hotel_request = HotelReservationRequest(
                confirmation_code=confirmation_code,
                city=booking.destination_city,
                hotel_name="Grand Plaza Hotel",
                room_type=booking.room_type,
                check_in=booking.departure_date,
                check_out=booking.return_date
            )

            hotel_result = await workflow.execute_activity(
                book_hotel,
                args=[hotel_request],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )
            workflow.logger.info(f"✓ Hotel booked: {confirmation_code}")
            workflow.logger.info(f"  {hotel_result.nights()} nights at ${hotel_result.price:.2f}")

            # Step 3: Book Rental Car
            workflow.logger.info("\nStep 3: Booking rental car...")
            car_request = CarReservationRequest(
                confirmation_code=confirmation_code,
                rental_company="Enterprise",
                car_type=booking.car_type,
                pickup_date=booking.departure_date,
                return_date=booking.return_date
            )

            car_result = await workflow.execute_activity(
                book_car,
                args=[car_request],
                start_to_close_timeout=timedelta(minutes=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )

            workflow.logger.info(f"✓ Car rental booked: {confirmation_code}")
            workflow.logger.info(f"  ${car_result.price:.2f}")

            # Step 4: Process Payment
            workflow.logger.info("\nStep 4: Processing payment...")

            total_price = flight_result.price + hotel_result.price + car_result.price

            payment_result = await workflow.execute_activity(
                accept_payment,
                args=[confirmation_code, total_price],
                start_to_close_timeout=timedelta(seconds=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )
            workflow.logger.info(f"✓ Payment processed: {payment_result.id}")
            workflow.logger.info(f"  Total charged: ${total_price:.2f}")

            workflow.logger.info(f"\n{'=' * 70}")
            workflow.logger.info("✓ TRAVEL PACKAGE BOOKED SUCCESSFULLY!")
            workflow.logger.info(f"Confirmation Code: {confirmation_code}")
            workflow.logger.info(f"Payment: {payment_result.id}")
            workflow.logger.info(f"Total: ${total_price:.2f}")
            workflow.logger.info(f"{'=' * 70}\n")

        except ActivityError as e:
            workflow.logger.error(f"Booking failed: {e.cause}")
            workflow.logger.info("Rolling back reservations...")

            if car_result:
                await workflow.execute_activity(
                    cancel_car,
                    args=[confirmation_code],
                    start_to_close_timeout=timedelta(seconds=5),
                    retry_policy=DEFAULT_RETRY_POLICY
                )

            if hotel_result:
                await workflow.execute_activity(
                    cancel_hotel,
                    args=[confirmation_code],
                    start_to_close_timeout=timedelta(seconds=5),
                    retry_policy=DEFAULT_RETRY_POLICY
                )

            if flight_result:
                await workflow.execute_activity(
                    cancel_flight,
                    args=[confirmation_code],
                    start_to_close_timeout=timedelta(seconds=5),
                    retry_policy=DEFAULT_RETRY_POLICY
                )

            raise