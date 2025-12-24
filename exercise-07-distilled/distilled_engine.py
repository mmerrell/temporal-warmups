"""
Distilled - Game Engine
Core game logic and phase management
"""

from typing import Dict, List, Optional
import random
import time
from distilled_game import (
    GameState, Player, Spirit, Market, Recipe, Equipment,
    Phase, IngredientType, SpiritType, EquipmentType,
    Ingredients, RECIPES
)


class DistilledGame:
    """Main game engine for Distilled"""
    
    def __init__(self, game_id: str, player_names: List[str]):
        """Initialize a new game"""
        self.state = self._create_initial_state(game_id, player_names)
        self.spirit_counter = 0
    
    def _create_initial_state(self, game_id: str, player_names: List[str]) -> GameState:
        """Create initial game state"""
        players = {}
        for i, name in enumerate(player_names):
            player_id = f"player_{i+1}"
            players[player_id] = Player(
                player_id=player_id,
                name=name,
                money=50,
                ingredients=Ingredients(
                    corn=2, yeast=2, water=3, sugar=1
                ),
                equipment=Equipment()
            )
        
        # Initialize market
        market = Market(
            available_ingredients={
                IngredientType.CORN: 20,
                IngredientType.RYE: 15,
                IngredientType.BARLEY: 15,
                IngredientType.WHEAT: 15,
                IngredientType.SUGAR: 20,
                IngredientType.MOLASSES: 15,
                IngredientType.JUNIPER: 10,
                IngredientType.BOTANICALS: 10,
                IngredientType.FRUIT: 10,
                IngredientType.YEAST: 25,
                IngredientType.WATER: 30,
            },
            prices={
                IngredientType.CORN: 3,
                IngredientType.RYE: 4,
                IngredientType.BARLEY: 4,
                IngredientType.WHEAT: 3,
                IngredientType.SUGAR: 2,
                IngredientType.MOLASSES: 3,
                IngredientType.JUNIPER: 5,
                IngredientType.BOTANICALS: 4,
                IngredientType.FRUIT: 3,
                IngredientType.YEAST: 2,
                IngredientType.WATER: 1,
            }
        )
        
        state = GameState(
            game_id=game_id,
            players=players,
            market=market,
            available_recipes=RECIPES.copy(),
            current_phase=Phase.SETUP
        )
        
        return state
    
    def start_game(self):
        """Start the game"""
        self.state.current_phase = Phase.MARKET
        self.state.current_player_turn = list(self.state.players.keys())[0]
        print(f"🥃 Distilled - Game {self.state.game_id} started!")
        print(f"Players: {', '.join(p.name for p in self.state.players.values())}")
        print(f"Goal: Craft the finest spirits and earn the most prestige!\n")
    
    def execute_market_phase(self):
        """Execute market phase - players buy ingredients"""
        print(f"\n{'='*70}")
        print(f"Round {self.state.current_round} - MARKET PHASE 🛒")
        print(f"{'='*70}")
        
        for player in self.state.players.values():
            # Simple AI: buy random ingredients if can afford
            if player.money >= 10:
                purchases = random.randint(1, 3)
                for _ in range(purchases):
                    ingredient = random.choice(list(IngredientType))
                    cost = self.state.market.buy_ingredient(ingredient, 1)
                    
                    if cost > 0 and player.money >= cost:
                        player.money -= cost
                        player.ingredients.add(ingredient, 1)
                        self.state.market.available_ingredients[ingredient] -= 1
                        print(f"  {player.name} buys 1 {ingredient.value} for ${cost}")
        
        self.state.next_phase()
    
    def execute_distill_phase(self):
        """Execute distill phase - players craft spirits"""
        print(f"\n{'='*70}")
        print(f"Round {self.state.current_round} - DISTILL PHASE 🧪")
        print(f"{'='*70}")
        
        for player in self.state.players.values():
            # Try to distill spirits up to capacity
            distilled_count = 0
            capacity = player.equipment.distill_capacity()
            
            # Shuffle recipes to add variety
            available_recipes = random.sample(self.state.available_recipes, 
                                            len(self.state.available_recipes))
            
            for recipe in available_recipes:
                if distilled_count >= capacity:
                    break
                
                if not player.can_distill():
                    print(f"  {player.name} is at aging capacity!")
                    break
                
                if player.can_afford_recipe(recipe):
                    # Use ingredients
                    player.ingredients.use_recipe_ingredients(recipe)
                    
                    # Create spirit
                    self.spirit_counter += 1
                    quality = recipe.base_quality + player.equipment.quality_bonus()
                    quality = min(quality, 5)  # Max 5 stars
                    
                    spirit = Spirit(
                        spirit_id=f"spirit_{self.spirit_counter}",
                        spirit_type=recipe.spirit_type,
                        name=recipe.name,
                        quality=quality,
                        base_price=recipe.base_price,
                        is_aged=False,
                        aging_rounds_remaining=recipe.aging_time
                    )
                    
                    # Determine if needs aging
                    if spirit.aging_rounds_remaining > 0:
                        player.spirits_aging.append(spirit)
                        print(f"  {player.name} distills {recipe.name} ({quality}⭐) - aging for {recipe.aging_time} rounds")
                    else:
                        player.spirits_ready.append(spirit)
                        print(f"  {player.name} distills {recipe.name} ({quality}⭐) - ready to sell!")
                    
                    distilled_count += 1
        
        self.state.next_phase()
    
    def execute_age_phase(self):
        """Execute age phase - advance aging spirits"""
        print(f"\n{'='*70}")
        print(f"Round {self.state.current_round} - AGE PHASE 🛢️")
        print(f"{'='*70}")
        
        for player in self.state.players.values():
            if not player.spirits_aging:
                continue
            
            print(f"\n  {player.name}'s aging spirits:")
            newly_ready = []
            
            for spirit in player.spirits_aging:
                spirit.aging_rounds_remaining -= 1
                
                if spirit.aging_rounds_remaining <= 0:
                    spirit.is_aged = True
                    newly_ready.append(spirit)
                    print(f"    ✓ {spirit.name} finished aging! ({spirit.quality}⭐, ${spirit.current_value()})")
                else:
                    print(f"    ⏳ {spirit.name} - {spirit.aging_rounds_remaining} rounds remaining")
            
            # Move ready spirits
            for spirit in newly_ready:
                player.spirits_aging.remove(spirit)
                player.spirits_ready.append(spirit)
        
        self.state.next_phase()
    
    def execute_sell_phase(self):
        """Execute sell phase - players sell spirits"""
        print(f"\n{'='*70}")
        print(f"Round {self.state.current_round} - SELL PHASE 💰")
        print(f"{'='*70}")
        
        for player in self.state.players.values():
            if not player.spirits_ready:
                continue
            
            # Sell capacity (1 or 2 with bottling line)
            sell_capacity = 2 if player.equipment.bottling_line else 1
            
            # Sort by value (sell most valuable first)
            player.spirits_ready.sort(key=lambda s: s.current_value(), reverse=True)
            
            sold_count = 0
            for spirit in list(player.spirits_ready):
                if sold_count >= sell_capacity:
                    break
                
                value = spirit.current_value()
                # Add randomness to sale price
                sale_price = value + random.randint(-5, 10)
                sale_price = max(sale_price, value // 2)  # At least half value
                
                player.money += sale_price
                
                # Prestige based on quality
                prestige_gain = spirit.quality
                if spirit.is_aged:
                    prestige_gain += 2
                
                player.add_prestige(prestige_gain)
                player.spirits_ready.remove(spirit)
                
                print(f"  {player.name} sells {spirit.name} ({spirit.quality}⭐) for ${sale_price} (+{prestige_gain} prestige)")
                sold_count += 1
        
        self.state.next_phase()
    
    def execute_cleanup_phase(self):
        """Execute cleanup phase - prepare for next round"""
        print(f"\n{'='*70}")
        print(f"Round {self.state.current_round} - CLEANUP PHASE 🧹")
        print(f"{'='*70}")
        
        # Restock market (small amount)
        for ingredient in IngredientType:
            self.state.market.available_ingredients[ingredient] += random.randint(2, 5)
        
        # Players might buy equipment
        for player in self.state.players.values():
            if player.money >= 40 and random.random() < 0.3:
                # Random equipment purchase
                if not player.equipment.column_still:
                    player.equipment.column_still = True
                    player.money -= 40
                    print(f"  {player.name} buys Column Still (+1 quality)")
                elif not player.equipment.aging_warehouse:
                    player.equipment.aging_warehouse = True
                    player.money -= 35
                    print(f"  {player.name} buys Aging Warehouse (+2 aging capacity)")
        
        print(f"\n  Round {self.state.current_round} complete. Preparing round {self.state.current_round + 1}...")
        
        self.state.next_phase()
    
    def play_round(self):
        """Play a complete round"""
        if self.state.is_game_over():
            return False
        
        self.execute_market_phase()
        self.execute_distill_phase()
        self.execute_age_phase()
        self.execute_sell_phase()
        self.execute_cleanup_phase()
        
        return not self.state.is_game_over()
    
    def play_game(self):
        """Play the complete game"""
        self.start_game()
        
        while not self.state.is_game_over():
            self.play_round()
            time.sleep(0.5)  # Pause between rounds
        
        self.print_final_results()
    
    def print_final_results(self):
        """Print final game results"""
        print("\n" + "="*70)
        print("🏆 GAME OVER - FINAL RESULTS 🏆")
        print("="*70)
        
        print("\nFinal Scores:")
        sorted_players = sorted(
            self.state.players.values(),
            key=lambda p: p.prestige,
            reverse=True
        )
        
        for i, player in enumerate(sorted_players, 1):
            print(f"\n{i}. {player.name}:")
            print(f"   Prestige: {player.prestige} points")
            print(f"   Money: ${player.money}")
            print(f"   Spirits aging: {len(player.spirits_aging)}")
            print(f"   Spirits ready: {len(player.spirits_ready)}")
        
        winner = self.state.get_winner()
        if winner:
            print(f"\n🥇 {winner.name} wins with {winner.prestige} prestige points! 🥇")
    
    def get_state_summary(self) -> Dict:
        """Get a summary of current game state"""
        return {
            "game_id": self.state.game_id,
            "round": self.state.current_round,
            "phase": self.state.current_phase.value,
            "players": {
                p.player_id: {
                    "name": p.name,
                    "prestige": p.prestige,
                    "money": p.money,
                    "spirits_aging": len(p.spirits_aging),
                    "spirits_ready": len(p.spirits_ready),
                    "ingredients": {
                        "corn": p.ingredients.corn,
                        "yeast": p.ingredients.yeast,
                        "water": p.ingredients.water,
                        "sugar": p.ingredients.sugar,
                    }
                }
                for p in self.state.players.values()
            }
        }
