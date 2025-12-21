from dataclasses import dataclass
from datetime import datetime
from typing import Optional


@dataclass
class Room:
    room_number: str
    room_type: str
    is_available: bool

@dataclass
class ReservationRequest:
    guest_name: str
    guest_email: str
    guest_mobile: str
    room_type: str
    check_in: str
    check_out: str
    payment_method: str

@dataclass
class ReservationResult:
    success: bool
    reservation_id: str
    room_number: str
    total_price: float
    payment_id: str
    nights: int
    error: Optional[str] = None

class HotelData:
    def __init__(self):
        self.reservations = {}

        # Initialize some rooms
        self.rooms = [
            Room('101', 'standard', True),
            Room('102', 'standard', True),
            Room('103', 'standard', True),
            Room('201', 'deluxe', True),
            Room('202', 'deluxe', True),
            Room('301', 'suites', True),
        ]

        self.rates = {
            'standard': 100.00,
            'deluxe': 150.00,
            'suite': 250.00,
        }

