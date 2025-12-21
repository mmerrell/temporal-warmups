# hotel_reservation.py - Original messy implementation
import time
import random
from datetime import datetime, timedelta
from dataclasses import dataclass
from typing import List, Dict

# Room pricing
ROOM_PRICES = {
    'standard': 100.00,
    'deluxe': 150.00,
    'suite': 250.00,
}

@dataclass
class Room:
    room_number: str
    room_type: str
    is_available: bool

class HotelReservationSystem:
    def __init__(self):
        self.reservations = {}
        self.payments = {}
        self.emails_sent = []

        # Initialize some rooms
        self.rooms = [
            Room('101', 'standard', True),
            Room('102', 'standard', True),
            Room('103', 'standard', True),
            Room('201', 'deluxe', True),
            Room('202', 'deluxe', True),
            Room('301', 'suite', True),
        ]

    def process_reservation(self, guest_name, guest_email, room_type, check_in, check_out, payment_method):
        """
        Main reservation processing flow - MESSY!
        This is a monolithic function that does everything.
        """
        print(f"\n{'=' * 70}")
        print(f"Processing reservation for {guest_name}")
        print(f"Room type: {room_type}")
        print(f"Check-in: {check_in}, Check-out: {check_out}")
        print(f"{'=' * 70}\n")

        # Validation buried in the middle of everything
        if not guest_name or len(guest_name) < 2:
            raise ValueError("Invalid guest name")

        if not guest_email or '@' not in guest_email:
            raise ValueError("Invalid email address")

        # Calculate stay duration - should this be here?
        nights = (check_out - check_in).days
        if nights < 1:
            raise ValueError("Check-out must be after check-in")

        print(f"Calculating price for {nights} night(s)...")

        # Price calculation mixed into the flow
        base_price = ROOM_PRICES.get(room_type, 100.00)
        total_price = base_price * nights

        # Apply discount if staying 7+ nights
        if nights >= 7:
            total_price *= 0.9  # 10% discount
            print(f"Applied 7+ night discount: 10% off")

        print(f"Total price: ${total_price:.2f}")

        # Check room availability - NO RETRY LOGIC
        print(f"Checking {room_type} room availability...")
        time.sleep(0.5)

        available_rooms = [r for r in self.rooms
                           if r.room_type == room_type and r.is_available]

        if not available_rooms:
            raise Exception(f"No {room_type} rooms available for those dates")

        print(f"✓ Found {len(available_rooms)} available {room_type} room(s)")

        # Process payment with NAIVE RETRY LOGIC
        print(f"Processing payment of ${total_price:.2f}...")
        payment_id = None

        # Manual retry loop - should be handled by Temporal!
        for attempt in range(3):
            try:
                time.sleep(0.5)

                # Simulate payment gateway failures (20% failure rate)
                if random.random() < 0.2:
                    raise Exception("Payment gateway timeout")

                payment_id = f"PAY-{int(time.time())}-{random.randint(1000, 9999)}"
                self.payments[payment_id] = {
                    'amount': total_price,
                    'method': payment_method,
                    'timestamp': datetime.now(),
                    'guest': guest_name
                }
                print(f"✓ Payment processed: {payment_id}")
                break

            except Exception as e:
                print(f"✗ Payment attempt {attempt + 1} failed: {e}")
                if attempt < 2:
                    wait_time = 2 ** attempt  # Exponential backoff
                    print(f"  Retrying in {wait_time}s...")
                    time.sleep(wait_time)

        if not payment_id:
            raise Exception("Payment failed after 3 attempts")

        # Assign room - JUST FAILS, NO RETRY
        print(f"Assigning {room_type} room...")
        time.sleep(0.3)

        # Simulate occasional assignment failures (15% failure rate)
        if random.random() < 0.15:
            raise Exception("Room assignment system error")

        selected_room = available_rooms[0]
        selected_room.is_available = False
        room_number = selected_room.room_number

        print(f"✓ Room {room_number} assigned")

        # Create reservation record
        reservation_id = f"RES-{int(time.time())}-{random.randint(100, 999)}"
        self.reservations[reservation_id] = {
            'guest_name': guest_name,
            'guest_email': guest_email,
            'room_number': room_number,
            'room_type': room_type,
            'check_in': check_in,
            'check_out': check_out,
            'total_price': total_price,
            'payment_id': payment_id,
            'created_at': datetime.now()
        }

        # Send confirmation email - BASIC TRY/EXCEPT, NO RETRY
        print(f"Sending confirmation email to {guest_email}...")
        time.sleep(0.4)

        try:
            # Simulate email service failures (10% failure rate)
            if random.random() < 0.1:
                raise Exception("Email service unavailable")

            confirmation_message = f"""
Dear {guest_name},

Your reservation is confirmed!

Reservation ID: {reservation_id}
Room: {room_number} ({room_type})
Check-in: {check_in}
Check-out: {check_out}
Total: ${total_price:.2f}
Payment ID: {payment_id}

Thank you for choosing our hotel!
"""

            self.emails_sent.append({
                'to': guest_email,
                'subject': f'Reservation Confirmation - {reservation_id}',
                'body': confirmation_message,
                'sent_at': datetime.now()
            })

            print(f"✓ Confirmation email sent to {guest_email}")

        except Exception as e:
            # EMAIL FAILED BUT PAYMENT WENT THROUGH AND ROOM ASSIGNED!
            # This is the compensation problem we need to solve
            print(f"⚠ Warning: Email failed - {e}")
            print(f"⚠ Payment succeeded and room assigned, but customer wasn't notified!")
            # What do we do here? Nothing! Just continue... 😱

        print(f"\n{'=' * 70}")
        print(f"✓ Reservation {reservation_id} completed")
        print(f"  Guest: {guest_name}")
        print(f"  Room: {room_number} ({room_type})")
        print(f"  Nights: {nights}")
        print(f"  Total: ${total_price:.2f}")
        print(f"{'=' * 70}\n")

        return {
            'success': True,
            'reservation_id': reservation_id,
            'room_number': room_number,
            'total_price': total_price,
            'payment_id': payment_id,
            'nights': nights
        }


# Usage example
if __name__ == "__main__":
    hotel = HotelReservationSystem()

    # Test reservations
    reservations_to_process = [
        {
            'guest_name': 'Alice Johnson',
            'guest_email': 'alice@example.com',
            'room_type': 'deluxe',
            'check_in': datetime(2024, 12, 20),
            'check_out': datetime(2024, 12, 23),
            'payment_method': 'credit_card'
        },
        {
            'guest_name': 'Bob Smith',
            'guest_email': 'bob@example.com',
            'room_type': 'suite',
            'check_in': datetime(2024, 12, 25),
            'check_out': datetime(2025, 1, 5),  # 11 nights - gets discount!
            'payment_method': 'credit_card'
        },
        {
            'guest_name': 'Charlie Brown',
            'guest_email': 'charlie@example.com',
            'room_type': 'standard',
            'check_in': datetime(2024, 12, 22),
            'check_out': datetime(2024, 12, 24),
            'payment_method': 'debit_card'
        },
    ]

    for reservation in reservations_to_process:
        try:
            result = hotel.process_reservation(**reservation)
            print(f"Success: {result['reservation_id']}")
        except Exception as e:
            print(f"Failed: {e}")

        time.sleep(1)  # Pause between reservations

    print(f"\n\nFinal Summary:")
    print(f"Total reservations: {len(hotel.reservations)}")
    print(f"Total payments: {len(hotel.payments)}")
    print(f"Emails sent: {len(hotel.emails_sent)}")
    print(f"\nAvailable rooms remaining:")
    for room in hotel.rooms:
        if room.is_available:
            print(f"  {room.room_number} ({room.room_type})")