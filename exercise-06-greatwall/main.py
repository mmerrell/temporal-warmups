"""
The Great Wall - Main Demo
Run a sample game
"""

from great_wall_engine import GreatWallGame
import json


def main():
    """Run a demo game"""
    print("="*60)
    print("THE GREAT WALL - BOARD GAME SIMULATION")
    print("="*60)
    
    # Create a game with 3 players
    game = GreatWallGame(
        game_id="game_001",
        player_names=["Zhang Wei", "Li Ming", "Wang Fang"]
    )
    
    # Play the complete game
    game.play_game()
    
    # Print final state summary
    print("\n" + "="*60)
    print("FINAL GAME STATE")
    print("="*60)
    state_summary = game.get_state_summary()
    print(json.dumps(state_summary, indent=2))


def demo_single_round():
    """Demo a single round with more detailed output"""
    print("="*60)
    print("SINGLE ROUND DEMO")
    print("="*60)
    
    game = GreatWallGame(
        game_id="demo_round",
        player_names=["Alice", "Bob"]
    )
    
    game.start_game()
    
    # Print initial state
    print("\nInitial State:")
    for player in game.state.players.values():
        print(f"  {player.name}:")
        print(f"    Resources: Gold={player.resources.gold}, Wood={player.resources.wood}, "
              f"Stone={player.resources.stone}, Chi={player.resources.chi}")
        print(f"    Troops: Infantry={player.troops.infantry}")
        print(f"    Advisors: {player.advisors_available}")
    
    # Play one round
    game.play_round()
    
    # Print final state
    print("\nState After Round:")
    for player in game.state.players.values():
        print(f"  {player.name}:")
        print(f"    Honor: {player.honor_points}")
        print(f"    Resources: Gold={player.resources.gold}, Wood={player.resources.wood}, "
              f"Stone={player.resources.stone}, Chi={player.resources.chi}")
        print(f"    Troops: Archers={player.troops.archers}, Infantry={player.troops.infantry}, "
              f"Cavalry={player.troops.cavalry}")


def interactive_demo():
    """Interactive demo showing state at each phase"""
    print("="*60)
    print("INTERACTIVE PHASE DEMO")
    print("="*60)
    
    game = GreatWallGame(
        game_id="interactive_demo",
        player_names=["Player 1", "Player 2", "Player 3"]
    )
    
    game.start_game()
    
    phases_to_run = [
        ("Command Phase", game.execute_command_phase),
        ("Worker Placement", game.execute_worker_placement_phase),
        ("Resolution Phase", game.execute_resolution_phase),
        ("Battle Phase", game.execute_battle_phase),
        ("Cleanup Phase", game.execute_cleanup_phase),
    ]
    
    for phase_name, phase_func in phases_to_run:
        input(f"\nPress Enter to execute {phase_name}...")
        phase_func()
        
        # Show current honor scores
        print("\nCurrent Honor Scores:")
        for player in game.state.players.values():
            print(f"  {player.name}: {player.honor_points} points")
    
    print("\n" + "="*60)
    print("Round 1 Complete!")
    print("="*60)


if __name__ == "__main__":
    # Run full game demo
    main()
    
    # Uncomment to run other demos:
    # demo_single_round()
    # interactive_demo()
