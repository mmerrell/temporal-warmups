"""
Distilled - Board Game Implementation
A game about crafting artisan spirits
"""

from dataclasses import dataclass, field
from typing import List, Dict, Optional
from enum import Enum
import random


class IngredientType(Enum):
    """Types of ingredients for distilling"""
    CORN = "corn"
    RYE = "rye"
    BARLEY = "barley"
    WHEAT = "wheat"
    SUGAR = "sugar"
    MOLASSES = "molasses"
    JUNIPER = "juniper"
    BOTANICALS = "botanicals"
    FRUIT = "fruit"
    YEAST = "yeast"
    WATER = "water"


class SpiritType(Enum):
    """Types of spirits that can be distilled"""
    WHISKEY = "whiskey"
    BOURBON = "bourbon"
    RUM = "rum"
    GIN = "gin"
    VODKA = "vodka"
    MOONSHINE = "moonshine"


class EquipmentType(Enum):
    """Distillery equipment"""
    COPPER_POT = "copper_pot"
    COLUMN_STILL = "column_still"
    PREMIUM_BARREL = "premium_barrel"
    AGING_WAREHOUSE = "aging_warehouse"
    BOTTLING_LINE = "bottling_line"
    FLAVOR_LABORATORY = "flavor_laboratory"


class Phase(Enum):
    """Game phases"""
    SETUP = "setup"
    MARKET = "market"
    DISTILL = "distill"
    AGE = "age"
    SELL = "sell"
    CLEANUP = "cleanup"
    GAME_OVER = "game_over"


@dataclass
class Recipe:
    """A recipe for distilling a spirit"""
    spirit_type: SpiritType
    name: str
    ingredients: Dict[IngredientType, int]  # ingredient -> quantity needed
    base_quality: int  # 1-5 stars
    base_price: int
    aging_time: int  # rounds required to age (0 = no aging needed)
    
    def get_quality_bonus(self, premium_ingredients: bool = False) -> int:
        """Calculate quality bonus"""
        bonus = 0
        if premium_ingredients:
            bonus += 1
        return bonus


@dataclass
class Ingredients:
    """Player's ingredient inventory"""
    corn: int = 0
    rye: int = 0
    barley: int = 0
    wheat: int = 0
    sugar: int = 0
    molasses: int = 0
    juniper: int = 0
    botanicals: int = 0
    fruit: int = 0
    yeast: int = 0
    water: int = 0
    
    def add(self, ingredient_type: IngredientType, amount: int):
        """Add ingredients"""
        setattr(self, ingredient_type.value, getattr(self, ingredient_type.value) + amount)
    
    def remove(self, ingredient_type: IngredientType, amount: int) -> bool:
        """Remove ingredients if available"""
        current = getattr(self, ingredient_type.value)
        if current >= amount:
            setattr(self, ingredient_type.value, current - amount)
            return True
        return False
    
    def has(self, ingredient_type: IngredientType, amount: int) -> bool:
        """Check if has enough ingredients"""
        return getattr(self, ingredient_type.value) >= amount
    
    def has_recipe_ingredients(self, recipe: Recipe) -> bool:
        """Check if has all ingredients for a recipe"""
        for ingredient, amount in recipe.ingredients.items():
            if not self.has(ingredient, amount):
                return False
        return True
    
    def use_recipe_ingredients(self, recipe: Recipe) -> bool:
        """Use ingredients for a recipe"""
        if not self.has_recipe_ingredients(recipe):
            return False
        for ingredient, amount in recipe.ingredients.items():
            self.remove(ingredient, amount)
        return True


@dataclass
class Spirit:
    """A distilled spirit"""
    spirit_id: str
    spirit_type: SpiritType
    name: str
    quality: int  # 1-5 stars
    base_price: int
    is_aged: bool = False
    aging_rounds_remaining: int = 0
    barrel_type: str = "standard"
    
    def current_value(self) -> int:
        """Calculate current market value"""
        value = self.base_price
        value += self.quality * 5  # Quality bonus
        if self.is_aged:
            value += 20  # Aging bonus
            if self.barrel_type == "premium":
                value += 15  # Premium barrel bonus
        return value


@dataclass
class Equipment:
    """Distillery equipment owned by player"""
    copper_pot: int = 1  # How many spirits can distill per round
    column_still: bool = False  # Improved quality
    premium_barrels: int = 0  # Better aging
    aging_warehouse: bool = False  # Age 2 extra spirits per round
    bottling_line: bool = False  # Sell 2 spirits per round instead of 1
    flavor_lab: bool = False  # +1 quality to all spirits
    
    def distill_capacity(self) -> int:
        """How many spirits can be distilled per round"""
        return self.copper_pot
    
    def aging_capacity(self) -> int:
        """How many spirits can age simultaneously"""
        base = 3
        if self.aging_warehouse:
            base += 2
        return base
    
    def quality_bonus(self) -> int:
        """Quality bonus from equipment"""
        bonus = 0
        if self.column_still:
            bonus += 1
        if self.flavor_lab:
            bonus += 1
        return bonus


@dataclass
class Player:
    """A player in the game"""
    player_id: str
    name: str
    money: int = 50
    ingredients: Ingredients = field(default_factory=Ingredients)
    equipment: Equipment = field(default_factory=Equipment)
    spirits_aging: List[Spirit] = field(default_factory=list)
    spirits_ready: List[Spirit] = field(default_factory=list)
    prestige: int = 0  # Victory points
    
    def can_afford_recipe(self, recipe: Recipe) -> bool:
        """Check if player has ingredients for recipe"""
        return self.ingredients.has_recipe_ingredients(recipe)
    
    def can_distill(self) -> bool:
        """Check if player can distill (has capacity)"""
        return len(self.spirits_aging) < self.equipment.aging_capacity()
    
    def add_prestige(self, points: int):
        """Gain prestige points"""
        self.prestige += points


@dataclass
class Market:
    """The ingredient market"""
    available_ingredients: Dict[IngredientType, int]  # ingredient -> quantity available
    prices: Dict[IngredientType, int]  # ingredient -> price per unit
    
    def buy_ingredient(self, ingredient_type: IngredientType, quantity: int) -> int:
        """Calculate cost to buy ingredients"""
        if self.available_ingredients.get(ingredient_type, 0) < quantity:
            return -1  # Not enough available
        return self.prices.get(ingredient_type, 5) * quantity


@dataclass
class GameState:
    """Complete game state"""
    game_id: str
    players: Dict[str, Player]
    market: Market
    available_recipes: List[Recipe]
    current_round: int = 1
    current_phase: Phase = Phase.SETUP
    current_player_turn: Optional[str] = None
    max_rounds: int = 7
    
    def get_player(self, player_id: str) -> Optional[Player]:
        """Get player by ID"""
        return self.players.get(player_id)
    
    def is_game_over(self) -> bool:
        """Check if game is over"""
        return self.current_round > self.max_rounds
    
    def next_phase(self):
        """Advance to next phase"""
        phase_order = [
            Phase.MARKET,
            Phase.DISTILL,
            Phase.AGE,
            Phase.SELL,
            Phase.CLEANUP
        ]
        
        if self.current_phase == Phase.SETUP:
            self.current_phase = Phase.MARKET
        elif self.current_phase in phase_order:
            current_idx = phase_order.index(self.current_phase)
            if current_idx < len(phase_order) - 1:
                self.current_phase = phase_order[current_idx + 1]
            else:
                # End of round
                self.current_round += 1
                self.current_phase = Phase.MARKET if not self.is_game_over() else Phase.GAME_OVER
    
    def get_winner(self) -> Optional[Player]:
        """Determine the winner by prestige points"""
        if not self.is_game_over():
            return None
        return max(self.players.values(), key=lambda p: p.prestige)


# Standard recipes
RECIPES = [
    Recipe(
        spirit_type=SpiritType.BOURBON,
        name="Classic Bourbon",
        ingredients={
            IngredientType.CORN: 3,
            IngredientType.RYE: 1,
            IngredientType.YEAST: 1,
            IngredientType.WATER: 2
        },
        base_quality=3,
        base_price=40,
        aging_time=2
    ),
    Recipe(
        spirit_type=SpiritType.RUM,
        name="Spiced Rum",
        ingredients={
            IngredientType.MOLASSES: 3,
            IngredientType.SUGAR: 1,
            IngredientType.YEAST: 1,
            IngredientType.WATER: 2
        },
        base_quality=2,
        base_price=30,
        aging_time=1
    ),
    Recipe(
        spirit_type=SpiritType.GIN,
        name="London Dry Gin",
        ingredients={
            IngredientType.JUNIPER: 2,
            IngredientType.BOTANICALS: 2,
            IngredientType.YEAST: 1,
            IngredientType.WATER: 2
        },
        base_quality=3,
        base_price=35,
        aging_time=0  # Gin doesn't need aging
    ),
    Recipe(
        spirit_type=SpiritType.VODKA,
        name="Premium Vodka",
        ingredients={
            IngredientType.WHEAT: 4,
            IngredientType.YEAST: 1,
            IngredientType.WATER: 3
        },
        base_quality=2,
        base_price=25,
        aging_time=0
    ),
    Recipe(
        spirit_type=SpiritType.WHISKEY,
        name="Single Malt Whiskey",
        ingredients={
            IngredientType.BARLEY: 4,
            IngredientType.YEAST: 1,
            IngredientType.WATER: 2
        },
        base_quality=4,
        base_price=50,
        aging_time=3
    ),
    Recipe(
        spirit_type=SpiritType.MOONSHINE,
        name="Corn Moonshine",
        ingredients={
            IngredientType.CORN: 4,
            IngredientType.SUGAR: 2,
            IngredientType.YEAST: 1
        },
        base_quality=1,
        base_price=20,
        aging_time=0
    ),
]
