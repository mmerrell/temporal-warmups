# inventory.py
# This simulates an external database that activities can access
import time

class UserDatabase:
    def __init__(self):
        self._users = {}

    def create_user(self, email: str, username: str, password: str) -> str:
        user_id = f"user_{len(self._users) + 1}"
        self._users[user_id] = {
            'email': email,
            'username': username,
            'password': password,
            'verified': False,
            'created_at': time.time()
        }
        return user_id

    def user_exists(self, username: str) -> bool:
        return any(u['username'] == username for u in self._users.values())


# Global instance (in real life, this would be a real database)
user_db = UserDatabase()