"""
Distilled - Main Demo
Run a sample game
"""

from distilled_engine import DistilledGame
import json


def main():
    """Run a demo game"""
    print("="*70)
    print("DISTILLED - CRAFT SPIRITS BOARD GAME SIMULATION")
    print("="*70)
    
    # Create a game with 3 players
    game = DistilledGame(
        game_id="game_001",
        player_names=["Master Distiller Chen", "Brewmaster Jones", "Artisan Smith"]
    )
    
    # Play the complete game
    game.play_game()
    
    # Print final state summary
    print("\n" + "="*70)
    print("FINAL GAME STATE")
    print("="*70)
    state_summary = game.get_state_summary()
    print(json.dumps(state_summary, indent=2))


def demo_single_round():
    """Demo a single round with more detailed output"""
    print("="*70)
    print("SINGLE ROUND DEMO")
    print("="*70)
    
    game = DistilledGame(
        game_id="demo_round",
        player_names=["Alice", "Bob"]
    )
    
    game.start_game()
    
    # Print initial state
    print("\nInitial State:")
    for player in game.state.players.values():
        print(f"  {player.name}:")
        print(f"    Money: ${player.money}")
        print(f"    Ingredients: Corn={player.ingredients.corn}, "
              f"Yeast={player.ingredients.yeast}, Water={player.ingredients.water}")
        print(f"    Spirits: {len(player.spirits_aging)} aging, "
              f"{len(player.spirits_ready)} ready")
    
    # Play one round
    game.play_round()
    
    # Print final state
    print("\nState After Round:")
    for player in game.state.players.values():
        print(f"  {player.name}:")
        print(f"    Prestige: {player.prestige} points")
        print(f"    Money: ${player.money}")
        print(f"    Spirits: {len(player.spirits_aging)} aging, "
              f"{len(player.spirits_ready)} ready")


def demo_recipe_crafting():
    """Demonstrate recipe crafting mechanics"""
    print("="*70)
    print("RECIPE CRAFTING DEMO")
    print("="*70)
    
    from distilled_game import RECIPES
    
    print("\nAvailable Recipes:")
    for recipe in RECIPES:
        print(f"\n  {recipe.name} ({recipe.spirit_type.value})")
        print(f"    Quality: {recipe.base_quality}⭐")
        print(f"    Base Price: ${recipe.base_price}")
        print(f"    Aging Time: {recipe.aging_time} rounds")
        print(f"    Ingredients needed:")
        for ingredient, amount in recipe.ingredients.items():
            print(f"      - {ingredient.value}: {amount}")


def demo_parallel_aging():
    """Demonstrate parallel aging of multiple spirits"""
    print("="*70)
    print("PARALLEL AGING DEMO")
    print("="*70)
    
    game = DistilledGame(
        game_id="aging_demo",
        player_names=["Master Distiller"]
    )
    
    game.start_game()
    
    print("\nThis demonstrates how multiple spirits age in parallel:")
    print("Round 1: Distill 3 different spirits with different aging times")
    print("Rounds 2-4: Watch them age simultaneously")
    print("Each spirit finishes aging at different times\n")
    
    # Play a few rounds to show parallel aging
    for i in range(4):
        print(f"\n--- Playing Round {i+1} ---")
        if not game.play_round():
            break


if __name__ == "__main__":
    # Run full game demo
    main()
    
    # Uncomment to run other demos:
    # demo_single_round()
    # demo_recipe_crafting()
    # demo_parallel_aging()
