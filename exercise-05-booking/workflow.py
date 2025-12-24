from temporalio import workflow
from temporalio.common import RetryPolicy

from database import FlightReservationRequest, HotelReservationRequest, CarReservationRequest, TravelBookingRequest
import random
from database import get_booking_database
from datetime import timedelta

DEFAULT_RETRY_POLICY = RetryPolicy(
    maximum_attempts=3,
    initial_interval=timedelta(seconds=1),
    maximum_interval=timedelta(seconds=10),
    backoff_coefficient=2.0,
)

with workflow.unsafe.imports_passed_through():
    from activities import (
        book_flight,
        book_car,
        book_hotel,
        accept_payment,
    )

@workflow.defn
class BookingWorkflow:
    @workflow.run
    async def run(self, booking: TravelBookingRequest):
        workflow.logger.info("\n{'=' * 70}")
        workflow.logger.info(f"Processing travel booking for {booking.customer_name}")
        workflow.logger.info(f"Carriage Class:  {booking.carriage_class}")
        workflow.logger.info(f"Route: {booking.departure_city} → {booking.destination_city}")
        workflow.logger.info(f"Dates: {booking.departure_date} to {booking.return_date}")
        workflow.logger.info(f"Passengers: {booking.num_passengers}")
        workflow.logger.info(f"{'=' * 70}\n")

        confirmation_code = str(workflow.uuid4().hex[:6])

        try:
            # ========================================
            # Step 1: Book Flight
            # ========================================
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
                start_to_close_timeout=timedelta(seconds=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )
            workflow.logger.info(f"✓ Flight booked: {confirmation_code}")
            workflow.logger.info(f"  Price: ${flight_result.flight_price:.2f}")

            # ========================================
            # Step 2: Book Hotel
            # ========================================
            workflow.logger.info("\nStep 2: Booking hotel...")

            hotel_request = HotelReservationRequest(
                confirmation_code=confirmation_code,
                hotel_name="Grand Plaza Hotel",
                room_type="Standard Double",
                check_in=booking.departure_date,
                check_out=booking.return_date
            )

            hotel_result = await workflow.execute_activity(
                book_hotel,
                args=[hotel_request],
                start_to_close_timeout=timedelta(seconds=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )
            workflow.logger.info(f"✓ Hotel booked: {confirmation_code}")
            workflow.logger.info(f"  {len(hotel_result.nights)} nights at ${hotel_result.price:.2f}")

            # ========================================
            # Step 3: Book Rental Car
            # ========================================
            workflow.logger.info("\nStep 3: Booking rental car...")
            car_request = CarReservationRequest(
                confirmation_code=confirmation_code,
                rental_company="Enterprise",
                car_type="Economy",
                pickup_date=booking.departure_date,
                return_date=booking.return_date
            )

            car_result = await workflow.execute_activity(
                book_car,
                args=[car_request],
                start_to_close_timeout=timedelta(seconds=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )

            workflow.logger.info(f"✓ Car rental booked: {confirmation_code}")
            workflow.logger.info(f"  ${car_result.price:.2f}")

            # ========================================
            # Step 4: Process Payment
            # ========================================
            workflow.logger.info("\nStep 4: Processing payment...")

            total_price = flight_result.price + hotel_result.price + car_result.price

            payment_result = await workflow.execute_activity(
                accept_payment,
                total_price,
                start_to_close_timeout=timedelta(seconds=5),
                retry_policy=DEFAULT_RETRY_POLICY
            )
            workflow.logger.info(f"✓ Payment processed: {payment_result.payment_id}")
            workflow.logger.info(f"  Total charged: ${total_price:.2f}")

            # ========================================
            # Step 5: Send confirmation email
            # ========================================
            workflow.logger.info("\nStep 5: Sending confirmation email...")

            # Email can fail but we don't care - booking is complete
            if random.random() < 0.10:
                workflow.logger.info("⚠ Warning: Email service failed, but booking is complete")
            else:
                workflow.logger.info(f"✓ Confirmation email sent to {booking.customer_email}")

            workflow.logger.info(f"\n{'=' * 70}")
            workflow.logger.info("✓ TRAVEL PACKAGE BOOKED SUCCESSFULLY!")
            workflow.logger.info(f"Confirmation Code: {confirmation_code}")
            workflow.logger.info(f"Payment: {payment_result.id}")
            workflow.logger.info(f"Total: ${total_price:.2f}")
            workflow.logger.info(f"{'=' * 70}\n")

        except Exception as e:
            # ========================================
            # CRITICAL PROBLEM: INCOMPLETE ROLLBACK!
            # ========================================
            workflow.logger.info(f"\n{'=' * 70}")
            workflow.logger.info(f"✗ BOOKING FAILED: {e}")
            workflow.logger.info(f"{'=' * 70}")

            # This is the WRONG way to handle it!
            # We just log warnings but don't actually cancel anything!
            # Need to compensate for these as they happen, not just at the end
            # TODO bookings needs to look at the results here, not the reservation request
            if self.bookings.flight_requests[confirmation_code].success:
                workflow.logger.info(f"⚠ WARNING: Flight {confirmation_code} was booked but not cancelled!")
                workflow.logger.info("  Customer will be charged for unused flight!")

            if self.bookings.hotel_requests[confirmation_code].success:
                workflow.logger.info(f"⚠ WARNING: Hotel {confirmation_code} was booked but not cancelled!")
                workflow.logger.info("  Hotel will charge no-show fee!")

            if self.bookings.car_requests[confirmation_code].success:
                workflow.logger.info(f"⚠ WARNING: Car {confirmation_code} was booked!")

            # We should be cancelling these reservations, but we're not!
            # This is what SAGA pattern / compensations solve!

            workflow.logger.info(f"{'=' * 70}\n")

    @workflow.query
    def get_reservation_summary(self) -> str:
        """Query to get current reservation status and details"""
        db = get_booking_database()
        details = db.log_full_reservation_details(self.confirmation_code)

        if not details:
            return f"❌ No reservation found for {self.confirmation_code}"

        lines = [
            f"\n{'=' * 70}", f"RESERVATION: {self.confirmation_code}",
            f"Status: {'✅ Complete' if self.flight_booked and self.hotel_booked and self.car_booked else '🔄 In Progress'}",
            f"{'=' * 70}\n"
        ]

        # Flight details
        if details['flight']:
            f = details['flight']
            lines.append("✈️  FLIGHT:")
            lines.append(f"   {f['airline']} {f['reservation'].flight_number}")
            lines.append(f"   {f['departure_airport']} → {f['arrival_airport']}")
            lines.append(f"   Date: {f['reservation'].departure_date}")
            lines.append(f"   Departure: {f['departure_time']}")
            lines.append(f"   Arrival: {f['arrival_time']}")
            lines.append(f"   Class: {f['reservation'].carriage_class}")
            lines.append(f"   Price: ${f['reservation'].price:.2f}")
            lines.append(f"   Status: {'✅ Booked' if self.flight_booked else '⏳ Pending'}")

        # Hotel details
        if details['hotel']:
            h = details['hotel']
            lines.append(f"\n🏨 HOTEL:")
            lines.append(f"   {h['reservation'].hotel_name}")
            lines.append(f"   Room Type: {h['reservation'].room_type}")
            lines.append(f"   Check-in: {h['reservation'].check_in}")
            lines.append(f"   Check-out: {h['reservation'].check_out}")
            lines.append(f"   Price: ${h['reservation'].price:.2f}")
            lines.append(f"   Status: {'✅ Booked' if self.hotel_booked else '⏳ Pending'}")

        # Car details
        if details['car']:
            c = details['car']
            car = c['car']
            lines.append(f"\n🚗 RENTAL CAR:")
            lines.append(f"   {car.make} {car.model} ({car.car_type})")
            lines.append(f"   License: {c['reservation'].license_plate}")
            lines.append(f"   Pickup: {c['reservation'].pickup_date}")
            lines.append(f"   Return: {c['reservation'].return_date}")
            lines.append(f"   Price: ${c['reservation'].price:.2f}")
            lines.append(f"   Status: {'✅ Booked' if self.car_booked else '⏳ Pending'}")

        lines.append(f"\n{'=' * 70}")
        lines.append(f"TOTAL: ${self.total_price:.2f}")
        lines.append(f"{'=' * 70}\n")

        return "\n".join(lines)