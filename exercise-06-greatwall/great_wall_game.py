"""
The Great Wall - Board Game Implementation
A simplified Python implementation of the core game mechanics
"""

from dataclasses import dataclass, field
from typing import List, Dict, Optional
from enum import Enum
import random


class ResourceType(Enum):
    """Types of resources in the game"""
    GOLD = "gold"
    WOOD = "wood"
    STONE = "stone"
    CHI = "chi"


class TroopType(Enum):
    """Types of military units"""
    ARCHER = "archer"
    INFANTRY = "infantry"
    CAVALRY = "cavalry"
    GENERAL = "general"


class LocationType(Enum):
    """Worker placement locations"""
    MARKET = "market"
    QUARRY = "quarry"
    LUMBER_MILL = "lumber_mill"
    CHI_TEMPLE = "chi_temple"
    BARRACKS = "barracks"
    COMMAND_CENTER = "command_center"
    WALL_SECTION = "wall_section"


class Phase(Enum):
    """Game phases"""
    SETUP = "setup"
    COMMAND = "command"
    WORKER_PLACEMENT = "worker_placement"
    RESOLUTION = "resolution"
    BATTLE = "battle"
    CLEANUP = "cleanup"
    GAME_OVER = "game_over"


@dataclass
class Resources:
    """Player resources"""
    gold: int = 0
    wood: int = 0
    stone: int = 0
    chi: int = 0
    
    def add(self, resource_type: ResourceType, amount: int):
        """Add resources"""
        setattr(self, resource_type.value, getattr(self, resource_type.value) + amount)
    
    def remove(self, resource_type: ResourceType, amount: int) -> bool:
        """Remove resources if available"""
        current = getattr(self, resource_type.value)
        if current >= amount:
            setattr(self, resource_type.value, current - amount)
            return True
        return False
    
    def has(self, resource_type: ResourceType, amount: int) -> bool:
        """Check if has enough resources"""
        return getattr(self, resource_type.value) >= amount


@dataclass
class Troops:
    """Military units"""
    archers: int = 0
    infantry: int = 0
    cavalry: int = 0
    
    def add(self, troop_type: TroopType, count: int):
        """Add troops"""
        if troop_type == TroopType.ARCHER:
            self.archers += count
        elif troop_type == TroopType.INFANTRY:
            self.infantry += count
        elif troop_type == TroopType.CAVALRY:
            self.cavalry += count
    
    def remove(self, troop_type: TroopType, count: int) -> bool:
        """Remove troops if available"""
        if troop_type == TroopType.ARCHER:
            if self.archers >= count:
                self.archers -= count
                return True
        elif troop_type == TroopType.INFANTRY:
            if self.infantry >= count:
                self.infantry -= count
                return True
        elif troop_type == TroopType.CAVALRY:
            if self.cavalry >= count:
                self.cavalry -= count
                return True
        return False
    
    def total(self) -> int:
        """Total troop count"""
        return self.archers + self.infantry + self.cavalry


@dataclass
class WallSection:
    """A section of the Great Wall"""
    section_id: int
    defender_id: Optional[str] = None  # Player ID defending this section
    fortification_level: int = 0
    towers: int = 0
    defenders: Troops = field(default_factory=Troops)
    damage: int = 0  # Accumulated damage
    
    def is_breached(self) -> bool:
        """Check if wall section is breached"""
        return self.damage >= (self.fortification_level * 3 + 5)
    
    def repair(self, amount: int):
        """Repair damage"""
        self.damage = max(0, self.damage - amount)
    
    def take_damage(self, amount: int):
        """Take damage"""
        self.damage += amount


@dataclass
class MongolHorde:
    """Enemy horde attacking the wall"""
    strength: int
    target_section: int
    defeated: bool = False
    
    def attack_strength(self) -> int:
        """Calculate attack strength with some randomness"""
        return self.strength + random.randint(0, 3)


@dataclass
class Player:
    """A player in the game"""
    player_id: str
    name: str
    resources: Resources = field(default_factory=Resources)
    troops: Troops = field(default_factory=Troops)
    honor_points: int = 0
    advisors_available: int = 5
    command_cards: List[str] = field(default_factory=list)
    wall_sections_defended: List[int] = field(default_factory=list)
    
    def can_afford(self, cost: Dict[ResourceType, int]) -> bool:
        """Check if player can afford a cost"""
        for resource_type, amount in cost.items():
            if not self.resources.has(resource_type, amount):
                return False
        return True
    
    def pay_cost(self, cost: Dict[ResourceType, int]) -> bool:
        """Pay a resource cost"""
        if not self.can_afford(cost):
            return False
        for resource_type, amount in cost.items():
            self.resources.remove(resource_type, amount)
        return True
    
    def gain_honor(self, points: int):
        """Gain honor points"""
        self.honor_points += points


@dataclass
class WorkerPlacement:
    """A worker placement action"""
    player_id: str
    location: LocationType
    advisors_used: int
    resolved: bool = False


@dataclass
class GameState:
    """Complete game state"""
    game_id: str
    players: Dict[str, Player]
    wall_sections: List[WallSection]
    mongol_hordes: List[MongolHorde]
    current_round: int = 1
    current_phase: Phase = Phase.SETUP
    current_player_turn: Optional[str] = None
    worker_placements: List[WorkerPlacement] = field(default_factory=list)
    max_rounds: int = 6
    
    def get_player(self, player_id: str) -> Optional[Player]:
        """Get player by ID"""
        return self.players.get(player_id)
    
    def get_wall_section(self, section_id: int) -> Optional[WallSection]:
        """Get wall section by ID"""
        for section in self.wall_sections:
            if section.section_id == section_id:
                return section
        return None
    
    def is_game_over(self) -> bool:
        """Check if game is over"""
        if self.current_round > self.max_rounds:
            return True
        # Game also ends if wall is completely breached
        breached_count = sum(1 for section in self.wall_sections if section.is_breached())
        return breached_count >= len(self.wall_sections) // 2
    
    def next_phase(self):
        """Advance to next phase"""
        phase_order = [
            Phase.COMMAND,
            Phase.WORKER_PLACEMENT,
            Phase.RESOLUTION,
            Phase.BATTLE,
            Phase.CLEANUP
        ]
        
        if self.current_phase == Phase.SETUP:
            self.current_phase = Phase.COMMAND
        elif self.current_phase in phase_order:
            current_idx = phase_order.index(self.current_phase)
            if current_idx < len(phase_order) - 1:
                self.current_phase = phase_order[current_idx + 1]
            else:
                # End of round, start new round
                self.current_round += 1
                self.current_phase = Phase.COMMAND if not self.is_game_over() else Phase.GAME_OVER
        
    def get_winner(self) -> Optional[Player]:
        """Determine the winner"""
        if not self.is_game_over():
            return None
        return max(self.players.values(), key=lambda p: p.honor_points)
