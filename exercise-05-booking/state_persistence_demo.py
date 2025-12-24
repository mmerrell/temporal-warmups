"""
State Persistence Example

This shows how game state could be saved/loaded, which is analogous
to how Temporal persists workflow state.
"""

import json
import pickle
from dataclasses import asdict
from great_wall_engine import GreatWallGame
from great_wall_game import GameState


def save_game_state_json(game: GreatWallGame, filename: str):
    """
    Save game state to JSON file.
    
    This demonstrates serialization similar to how Temporal
    would persist workflow state.
    """
    # Get a dictionary representation of the state
    state_dict = {
        'game_id': game.state.game_id,
        'current_round': game.state.current_round,
        'current_phase': game.state.current_phase.value,
        'max_rounds': game.state.max_rounds,
        'players': {
            player_id: {
                'player_id': player.player_id,
                'name': player.name,
                'resources': {
                    'gold': player.resources.gold,
                    'wood': player.resources.wood,
                    'stone': player.resources.stone,
                    'chi': player.resources.chi,
                },
                'troops': {
                    'archers': player.troops.archers,
                    'infantry': player.troops.infantry,
                    'cavalry': player.troops.cavalry,
                },
                'honor_points': player.honor_points,
                'advisors_available': player.advisors_available,
                'command_cards': player.command_cards,
                'wall_sections_defended': player.wall_sections_defended,
            }
            for player_id, player in game.state.players.items()
        },
        'wall_sections': [
            {
                'section_id': section.section_id,
                'defender_id': section.defender_id,
                'fortification_level': section.fortification_level,
                'towers': section.towers,
                'defenders': {
                    'archers': section.defenders.archers,
                    'infantry': section.defenders.infantry,
                    'cavalry': section.defenders.cavalry,
                },
                'damage': section.damage,
            }
            for section in game.state.wall_sections
        ],
        'mongol_hordes': [
            {
                'strength': horde.strength,
                'target_section': horde.target_section,
                'defeated': horde.defeated,
            }
            for horde in game.state.mongol_hordes
        ],
    }
    
    with open(filename, 'w') as f:
        json.dump(state_dict, f, indent=2)
    
    print(f"Game state saved to {filename}")
    print(f"File size: {len(json.dumps(state_dict))} bytes")


def demonstrate_state_snapshots():
    """
    Demonstrate taking state snapshots at different points in the game.
    
    This is similar to how Temporal creates history events at each step.
    """
    print("="*60)
    print("STATE SNAPSHOT DEMONSTRATION")
    print("="*60)
    
    game = GreatWallGame(
        game_id="snapshot_demo",
        player_names=["Alice", "Bob", "Carol"]
    )
    
    snapshots = []
    
    # Snapshot 0: Initial state
    snapshots.append({
        'label': 'Initial State',
        'state': game.get_state_summary()
    })
    
    game.start_game()
    
    # Snapshot 1: After game start
    snapshots.append({
        'label': 'After Game Start',
        'state': game.get_state_summary()
    })
    
    # Play through phases and take snapshots
    game.execute_command_phase()
    snapshots.append({
        'label': 'After Command Phase',
        'state': game.get_state_summary()
    })
    
    game.execute_worker_placement_phase()
    snapshots.append({
        'label': 'After Worker Placement',
        'state': game.get_state_summary()
    })
    
    game.execute_resolution_phase()
    snapshots.append({
        'label': 'After Resolution',
        'state': game.get_state_summary()
    })
    
    game.execute_battle_phase()
    snapshots.append({
        'label': 'After Battle',
        'state': game.get_state_summary()
    })
    
    # Print summary of each snapshot
    print("\nState Evolution:")
    print("-" * 60)
    
    for i, snapshot in enumerate(snapshots):
        print(f"\n{i}. {snapshot['label']}")
        print(f"   Round: {snapshot['state']['round']}")
        print(f"   Phase: {snapshot['state']['phase']}")
        
        # Show honor changes
        print("   Honor Points:")
        for player_id, player_data in snapshot['state']['players'].items():
            print(f"     {player_data['name']}: {player_data['honor']}")
    
    # Save all snapshots to file
    with open('game_snapshots.json', 'w') as f:
        json.dump(snapshots, f, indent=2)
    
    print("\n" + "="*60)
    print(f"Saved {len(snapshots)} snapshots to game_snapshots.json")
    print("="*60)
    
    return snapshots


def demonstrate_state_replay(snapshots):
    """
    Demonstrate replaying through saved state snapshots.
    
    This is analogous to Temporal's workflow history replay.
    """
    print("\n" + "="*60)
    print("STATE REPLAY DEMONSTRATION")
    print("="*60)
    
    print("\nReplaying game history...\n")
    
    for i, snapshot in enumerate(snapshots):
        print(f"Step {i}: {snapshot['label']}")
        print(f"  Round {snapshot['state']['round']} - {snapshot['state']['phase']}")
        
        # Show changes from previous snapshot
        if i > 0:
            prev_snapshot = snapshots[i-1]
            print("  Changes:")
            
            # Compare honor points
            for player_id in snapshot['state']['players']:
                current_honor = snapshot['state']['players'][player_id]['honor']
                prev_honor = prev_snapshot['state']['players'][player_id]['honor']
                
                if current_honor != prev_honor:
                    player_name = snapshot['state']['players'][player_id]['name']
                    change = current_honor - prev_honor
                    print(f"    {player_name}: {prev_honor} → {current_honor} ({'+' if change > 0 else ''}{change})")
        
        print()


def demonstrate_crash_recovery():
    """
    Demonstrate how a game could be recovered after a crash.
    
    This shows the value of durable execution that Temporal provides.
    """
    print("="*60)
    print("CRASH RECOVERY DEMONSTRATION")
    print("="*60)
    
    # Start a game and play partway through
    print("\n1. Starting a game...")
    game = GreatWallGame(
        game_id="crash_recovery_demo",
        player_names=["Alice", "Bob"]
    )
    game.start_game()
    game.execute_command_phase()
    game.execute_worker_placement_phase()
    
    # Save state
    print("\n2. Saving game state...")
    save_game_state_json(game, "crash_save.json")
    
    # Simulate crash
    print("\n3. ⚠️  SIMULATED CRASH ⚠️")
    print("   (Game object destroyed)")
    del game
    
    # Recovery
    print("\n4. Recovering from saved state...")
    print("   In a Temporal workflow, this recovery happens automatically!")
    print("   The workflow would resume from the last completed activity.")
    
    print("\n5. With Temporal:")
    print("   - State is automatically persisted after each activity")
    print("   - Workflow can resume exactly where it left off")
    print("   - No manual save/load needed")
    print("   - Complete audit trail of all state changes")


if __name__ == "__main__":
    # Demonstrate state snapshots
    snapshots = demonstrate_state_snapshots()
    
    # Demonstrate replay
    demonstrate_state_replay(snapshots)
    
    # Demonstrate crash recovery
    demonstrate_crash_recovery()
    
    print("\n" + "="*60)
    print("KEY TAKEAWAYS FOR TEMPORAL CONVERSION")
    print("="*60)
    print("""
In this demo, we manually:
- Captured state snapshots at each phase
- Serialized state to JSON
- Replayed through history
- Simulated crash recovery

Temporal provides all of this automatically:
✅ State persistence after each activity
✅ Complete history/event log
✅ Automatic replay on recovery
✅ Deterministic execution
✅ Time-travel debugging
✅ No manual save/load code needed

When converting to Temporal:
1. Each phase becomes an activity
2. GameState becomes workflow state
3. History events = automatic snapshots
4. Recovery = automatic replay
5. Add signals for player interactions
6. Add queries to inspect live state
""")
