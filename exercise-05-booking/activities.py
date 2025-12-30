import random
import uuid
from datetime import datetime, timedelta

from temporalio.exceptions import ApplicationError
from temporalio import activity

from database import (
    get_booking_database,
    FlightReservationRequest,
    HotelReservationRequest,
    CarReservationRequest,
    CompletedFlightReservation,
    CompletedHotelReservation,
    CompletedCarReservation,
    CompletedPayment, TravelBookingRequest,
)


# BOOKING ACTIVITIES
@activity.defn
async def store_booking_request(booking: TravelBookingRequest):
    db = get_booking_database()
    db.add_booking_request(booking.confirmation_code, booking)

@activity.defn
async def book_flight(request: FlightReservationRequest) -> CompletedFlightReservation:
    db = get_booking_database()
    booking_request = db.get_booking_request(request.confirmation_code)

    # Search for available flight
    result = db.find_available_flight(
        departure_city=request.departure_city,
        destination_city=request.destination_city,
        date=request.departure_date,
        carriage_class=request.carriage_class
    )

    if not result:
        raise ApplicationError(f"No available flights from {request.departure_city} to {request.destination_city}")

    flight, instance, seat = result

    # Book the seat
    seat_success = db.book_seat(flight.flight_number, request.departure_date, seat.seat_number)
    if not seat_success:
        raise ApplicationError(f"Unable to book seats on flight {flight.flight_number}")

    # Get pricing
    price = flight.price if flight else 350.00

    # Create completed reservation
    completed_reservation = CompletedFlightReservation(
        confirmation_code=request.confirmation_code,
        customer_name=booking_request.customer_name,
        customer_email=booking_request.customer_email,
        flight_number=flight.flight_number,
        date=request.departure_date,
        seat_numbers=[seat.seat_number],
        carriage_class=request.carriage_class,
        price=price,
        booked_at=datetime.now().isoformat()
    )

    # Store it
    db.flight_reservations[request.confirmation_code] = completed_reservation
    activity.logger.info(f"✓ Flight booked: {seat.seat_number} on {flight.flight_number}")

    return completed_reservation


@activity.defn
async def book_hotel(request: HotelReservationRequest) -> CompletedHotelReservation:
    db = get_booking_database()
    booking_request = db.get_booking_request(request.confirmation_code)

    # Bypass random failures for forced test scenarios
    if "FORCE-PAYMENT-FAIL" not in request.confirmation_code:
        # Simulate intermittent failures
        if random.random() < 0.25:
            raise ApplicationError("Hotel booking service unavailable")

    # Get the destination city from booking request
    city = booking_request.destination_city

    # Search for available hotel room
    result = db.find_available_hotel_room(
        city=city,
        room_type=request.room_type,
        check_in=request.check_in,
        check_out=request.check_out
    )

    if not result:
        raise ApplicationError(f"No available {request.room_type} rooms in {city}")

    hotel, room = result

    # Generate dates list for booking
    in_date = datetime.fromisoformat(request.check_in)
    out_date = datetime.fromisoformat(request.check_out)
    dates = []
    current = in_date
    while current < out_date:
        dates.append(current.strftime('%Y-%m-%d'))
        current += timedelta(days=1)

    # Book the room
    room.book_dates(dates)

    # Calculate price
    nights = len(dates)
    room_rate = hotel.rates.get(request.room_type, 100.00)
    total_price = room_rate * nights

    # Create completed reservation
    completed_reservation = CompletedHotelReservation(
        confirmation_code=request.confirmation_code,
        customer_name=booking_request.customer_name,
        hotel_name=hotel.hotel_name,
        room_number=room.room_number,
        room_type=request.room_type,
        check_in=request.check_in,
        check_out=request.check_out,
        price=total_price,
        booked_at=datetime.now().isoformat()
    )

    # Store it
    db.hotel_reservations[request.confirmation_code] = completed_reservation
    activity.logger.info(f"✓ Hotel booked: Room {room.room_number} at {hotel.hotel_name}")

    return completed_reservation


@activity.defn
async def book_car(request: CarReservationRequest) -> CompletedCarReservation:
    db = get_booking_database()
    booking_request = db.get_booking_request(request.confirmation_code)

    # Bypass random failures for forced test scenarios
    if "FORCE-PAYMENT-FAIL" not in request.confirmation_code:
        # Simulate intermittent failures
        if random.random() < 0.40:
            raise ApplicationError("Car rental service unavailable")

    # Get the destination city from booking request
    city = booking_request.destination_city

    # Search for available car
    car = db.find_available_car(
        city=city,
        car_type=request.car_type,
        pickup_date=request.pickup_date,
        return_date=request.return_date
    )

    if not car:
        raise ApplicationError(f"No available {request.car_type} cars in {city}")

    # Generate dates list for booking
    pickup = datetime.fromisoformat(request.pickup_date)
    return_date = datetime.fromisoformat(request.return_date)
    dates = []
    current = pickup
    while current <= return_date:  # Include return date
        dates.append(current.strftime('%Y-%m-%d'))
        current += timedelta(days=1)

    # Book the car
    car.book_dates(dates)

    # Calculate price
    rental_days = len(dates)
    total_price = car.daily_rate * rental_days

    # Create completed reservation
    completed_reservation = CompletedCarReservation(
        confirmation_code=request.confirmation_code,
        customer_name=booking_request.customer_name,
        license_plate=car.license_plate,
        car_type=request.car_type,
        pickup_date=request.pickup_date,
        return_date=request.return_date,
        price=total_price,
        booked_at=datetime.now().isoformat()
    )

    # Store it
    db.car_reservations[request.confirmation_code] = completed_reservation
    activity.logger.info(f"✓ Car booked: {car.make} {car.model} ({car.license_plate})")

    return completed_reservation


@activity.defn
async def accept_payment(confirmation_code: str, total_price: float) -> CompletedPayment:
    """Process payment for the total booking amount"""

    # Force failure for testing full compensation chain
    if "FORCE-PAYMENT-FAIL" in confirmation_code:
        raise ApplicationError("Forced payment failure for testing")

    # Simulate payment processing failures
    if random.random() < 0.10:
        raise ApplicationError("Payment processing failed")

    # Simulate payment service unavailability
    if random.random() < 0.15:
        raise ApplicationError("Payment service unavailable")

    # Generate payment ID
    payment_id = f"PAY-{uuid.uuid4().hex[:8]}"

    payment = CompletedPayment(
        id=payment_id,
        confirmation_code=confirmation_code
    )

    activity.logger.info(f"✓ Payment processed: {payment_id} for ${total_price:.2f}")

    return payment

# ========================================
# COMPENSATION ACTIVITIES
# ========================================

@activity.defn
async def cancel_flight(confirmation_code: str) -> None:
    """Cancel a flight reservation and release the seat"""
    db = get_booking_database()

    # Look up the reservation
    reservation = db.flight_reservations.get(confirmation_code)
    if not reservation:
        activity.logger.warning(f"No flight reservation found for {confirmation_code}")
        return

    # Release the seat(s)
    for seat_number in reservation.seat_numbers:
        db.release_seat(
            flight_number=reservation.flight_number,
            date=reservation.date,
            seat_number=seat_number
        )

    # Remove from database
    del db.flight_reservations[confirmation_code]

    activity.logger.info(f"✓ Flight cancelled: {reservation.flight_number} seats {reservation.seat_numbers}")

@activity.defn
async def cancel_hotel(confirmation_code: str) -> None:
    """Cancel a hotel reservation and release the room"""
    db = get_booking_database()

    # Look up the reservation
    reservation = db.hotel_reservations.get(confirmation_code)
    if not reservation:
        activity.logger.warning(f"No hotel reservation found for {confirmation_code}")
        return

    # Find the hotel and room
    hotel = next((h for h in db.hotels if h.hotel_name == reservation.hotel_name), None)
    if not hotel:
        activity.logger.warning(f"Hotel {reservation.hotel_name} not found")
        return

    room = next((r for r in hotel.rooms if r.room_number == reservation.room_number), None)
    if not room:
        activity.logger.warning(f"Room {reservation.room_number} not found")
        return

    # Generate dates to release
    in_date = datetime.fromisoformat(reservation.check_in)
    out_date = datetime.fromisoformat(reservation.check_out)
    dates = []
    current = in_date
    while current < out_date:
        dates.append(current.strftime('%Y-%m-%d'))
        current += timedelta(days=1)

    # Release the dates
    for date in dates:
        if date in room.unavailable_dates:
            room.unavailable_dates.remove(date)

    # Remove from database
    del db.hotel_reservations[confirmation_code]

    activity.logger.info(f"✓ Hotel cancelled: Room {reservation.room_number} at {reservation.hotel_name}")


@activity.defn
async def cancel_car(confirmation_code: str) -> None:
    """Cancel a car rental and release the vehicle"""
    db = get_booking_database()

    # Look up the reservation
    reservation = db.car_reservations.get(confirmation_code)
    if not reservation:
        activity.logger.warning(f"No car reservation found for {confirmation_code}")
        return

    # Find the car
    car = next((c for c in db.cars if c.license_plate == reservation.license_plate), None)
    if not car:
        activity.logger.warning(f"Car {reservation.license_plate} not found")
        return

    # Generate dates to release
    pickup = datetime.fromisoformat(reservation.pickup_date)
    return_date = datetime.fromisoformat(reservation.return_date)
    dates = []
    current = pickup
    while current <= return_date:
        dates.append(current.strftime('%Y-%m-%d'))
        current += timedelta(days=1)

    # Release the dates
    for date in dates:
        if date in car.unavailable_dates:
            car.unavailable_dates.remove(date)

    # Remove from database
    del db.car_reservations[confirmation_code]

    activity.logger.info(f"✓ Car rental cancelled: {car.make} {car.model} ({reservation.license_plate})")


@activity.defn
async def refund_payment(confirmation_code: str) -> None:
    """Process refund for a cancelled booking"""

    # Simulate refund processing
    if random.random() < 0.05:
        raise ApplicationError("Refund processing failed")

    refund_id = f"REF-{uuid.uuid4().hex[:8]}"

    activity.logger.info(f"✓ Payment refunded: {refund_id} for booking {confirmation_code}")