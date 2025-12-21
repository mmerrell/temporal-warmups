import asyncio
import random
import uuid
from typing import List

from temporalio import activity
from temporalio.exceptions import ApplicationError

from database import HotelData, Room, ReservationRequest, ReservationResult


@activity.defn
async def check_room_availability(res: ReservationRequest) -> List[Room]:
    hotel = HotelData()
    await asyncio.sleep(0.5)

    # This is very naive--basically only checks for whether it's available "today", not the dates themselves.
    #   It works for now, but any iteration would require changing from a boolean to basically incorporating
    #   a mini-calendar into each Room
    return [r for r in hotel.rooms
        if r.room_type == res.room_type and r.is_available]

@activity.defn
async def collect_payment(total_price: float) -> str:
    # Process payment with NAIVE RETRY LOGIC
    activity.logger.info(f"Processing payment of ${total_price:.2f}...")

    await asyncio.sleep(0.5)

    # Simulate payment gateway failures (20% failure rate)
    if random.random() < 0.2:
        raise ApplicationError("Payment gateway timeout")

    return f"PAY-{uuid.uuid4().hex[:12]}"

@activity.defn
async def assign_room(available_rooms: List[Room]) -> str:
    await asyncio.sleep(0.3)

    # Simulate occasional assignment failures (15% failure rate)
    if random.random() < 0.15:
        raise Exception("Room assignment system error")

    selected_room = available_rooms[0]
    selected_room.is_available = False

    return selected_room.room_number

@activity.defn
async def send_email_notification(res: ReservationRequest, res_result: ReservationResult):
        # Simulate email service failures (10% failure rate)
        if random.random() < 0.1:
            raise Exception("Email service unavailable")

        confirmation_message = await build_confirmation_message()
        await asyncio.sleep(2) # here's where we pretend we're sending the message
        activity.logger.info(f"✓ Confirmation email sent to {res.guest_email}")

@activity.defn
async def send_sms_notification(res: ReservationRequest, res_result: ReservationResult):
        # Simulate email service failures (10% failure rate)
        if random.random() < 0.1:
            raise Exception("SMS service unavailable")

        confirmation_message = await build_confirmation_message(res, res_result)
        await asyncio.sleep(2) # here's where we pretend we're sending the message
        activity.logger.info(f"✓ Confirmation text sent to {res.guest_mobile}")

@activity.defn
async def front_desk_confirmation(res: ReservationRequest, res_result: ReservationResult):
        # Simulate email service failures (10% failure rate)

        confirmation_message = build_confirmation_message(res, res_result)
        await asyncio.sleep(2) # here's where we pretend to assign the confirmation to the front desk
        activity.logger.info(f"✓ Front desk will call to notify {res.guest_mobile}")

async def build_confirmation_message(res: ReservationRequest, res_result: ReservationResult) -> str:
    return f"""
Dear {res.guest_name},

Your reservation is confirmed!

Reservation ID: {res_result.reservation_id}
Room: {res_result.room_number} ({res.room_type})
Check-in: {res.check_in}
Check-out: {res.check_out}
Total: ${res_result.total_price:.2f}
Payment ID: {res_result.payment_id}

Thank you for choosing our hotel!
"""