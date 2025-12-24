# The Great Wall - Board Game Implementation

A Python implementation of the board game "The Great Wall" with cooperative/competitive tower defense mechanics.

## Overview

This is a simplified implementation of The Great Wall board game that includes:
- Worker placement mechanics
- Resource management (gold, wood, stone, chi)
- Troop recruitment and deployment
- Wall defense and fortification
- Mongol horde attacks
- Honor point scoring
- Multi-round gameplay

## Files

- `great_wall_game.py` - Data models and game state
- `great_wall_engine.py` - Game engine and core logic
- `main.py` - Demo scripts to run the game

## Running the Game

```bash
python main.py
```

This will run a full 6-round game with 3 AI players.

## Game Flow

Each round consists of 5 phases:

1. **Command Phase** - Players draw command cards
2. **Worker Placement** - Players place advisors at locations to gather resources, recruit troops, etc.
3. **Resolution Phase** - Worker placements are resolved
4. **Battle Phase** - Mongol hordes attack wall sections
5. **Cleanup Phase** - Reset for next round

The game ends after 6 rounds or if the wall is breached. The player with the most honor points wins.

## Converting to Temporal Workflows

Here are some suggestions for converting this implementation to use Temporal:

### Workflows

The main game flow would make a great workflow:

```python
@workflow.defn
class GreatWallGameWorkflow:
    @workflow.run
    async def run(self, game_id: str, player_names: List[str]) -> GameResult:
        # Initialize game state
        state = await workflow.execute_activity(
            initialize_game,
            args=[game_id, player_names]
        )
        
        # Play rounds until game over
        while not state.is_game_over:
            state = await workflow.execute_activity(
                play_round,
                args=[state]
            )
        
        return calculate_winner(state)
```

### Activities

Each phase could be an activity:

- `initialize_game()` - Set up initial game state
- `execute_command_phase()` - Handle command card phase
- `execute_worker_placement_phase()` - Process worker placements
- `execute_resolution_phase()` - Resolve placements
- `execute_battle_phase()` - Run battles
- `execute_cleanup_phase()` - Clean up round
- `calculate_winner()` - Determine final winner

### Signals

For player interactions in a real multiplayer game:

```python
@workflow.signal
def place_worker(self, player_id: str, location: LocationType, advisors: int):
    # Handle worker placement signal
    pass

@workflow.signal
def play_command_card(self, player_id: str, card_name: str):
    # Handle command card play
    pass
```

### Queries

For checking game state:

```python
@workflow.query
def get_game_state(self) -> GameState:
    return self.state

@workflow.query
def get_player_info(self, player_id: str) -> Player:
    return self.state.players[player_id]
```

### State Management

The `GameState` dataclass is already designed to be serializable, which works well with Temporal's state persistence. Consider:

1. Store the complete game state in workflow state
2. Use Temporal's durable execution to handle long-running games
3. Use timers for turn timeouts
4. Use search attributes for querying active games

### Benefits of Temporal Version

- **Durability**: Game state survives crashes/restarts
- **Observability**: View game history and state at any point
- **Scalability**: Handle thousands of concurrent games
- **Time travel**: Replay games for debugging
- **Player disconnections**: Games continue even if players disconnect
- **Fair play**: Enforce turn timers and game rules
- **Audit trail**: Complete history of all actions

### Example Temporal Structure

```
temporal-great-wall/
├── activities/
│   ├── game_setup.py
│   ├── phase_execution.py
│   ├── combat_resolution.py
│   └── scoring.py
├── workflows/
│   ├── game_workflow.py
│   └── matchmaking_workflow.py
├── models/
│   └── game_state.py (from great_wall_game.py)
└── worker.py
```

### Key Considerations

1. **Activity Timeouts**: Set appropriate timeouts for each phase
2. **Retry Policies**: Define retry behavior for activities
3. **Versioning**: Use workflow versioning for game rule updates
4. **Signals vs Activities**: Use signals for player actions, activities for game logic
5. **Child Workflows**: Consider separate workflows for complex sub-systems (e.g., combat resolution)
6. **Continue-As-New**: For very long games, use continue-as-new to manage workflow history size

### Multiplayer Considerations

For a real multiplayer game with human players:

1. Use signals to receive player actions
2. Use timers to enforce turn timeouts
3. Use queries to let players check game state
4. Consider a separate workflow per game
5. Use workflow search to find active games by player ID
6. Implement a matchmaking workflow to pair players

### Testing

Temporal makes testing easier:

```python
async def test_game_workflow():
    async with await WorkflowEnvironment.start_time_skipping() as env:
        async with Worker(env.client, task_queue="test", workflows=[GreatWallGameWorkflow]):
            result = await env.client.execute_workflow(
                GreatWallGameWorkflow.run,
                args=["test_game", ["Alice", "Bob", "Carol"]],
                id="test-game-1",
                task_queue="test"
            )
            assert result.winner_name == "Alice"
```

## Gameplay Notes

This is a simplified version of the full board game. It includes:

✅ Core worker placement mechanics
✅ Resource gathering
✅ Troop recruitment  
✅ Wall fortification
✅ Combat resolution
✅ Honor point scoring
✅ Multi-round gameplay

Not implemented (but could be added):

- Command card special abilities
- Trading between players
- Wall section selection strategies
- More complex combat modifiers
- Special general abilities
- Achievement bonuses

## License

MIT License - feel free to use this as a learning example for Temporal!
