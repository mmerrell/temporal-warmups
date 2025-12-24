# Exercise #6: The Great Wall - Complex Branching Workflows

## 🎯 Learning Objectives

This exercise focuses on **complex branching and conditional logic** in Temporal workflows:

1. **Phase-based workflow branching** - Execute different logic based on game phase
2. **Loop-based orchestration** - Manage rounds, phases, and player turns in nested loops
3. **Conditional activity execution** - Execute activities only when conditions are met
4. **State-driven branching** - Use workflow state to determine execution paths
5. **Complex decision trees** - Handle multiple conditional branches in a single workflow
6. **Signal-based interaction** (Advanced) - Allow external input to influence workflow branches

## 📋 Scenario

You're building a turn-based board game system for "The Great Wall" - a cooperative/competitive tower defense game where players defend the Great Wall of China against Mongol invasions.

**Game Flow (5 phases per round, 6 rounds total):**
1. **Command Phase** - Players draw cards
2. **Worker Placement** - Players place advisors at locations
3. **Resolution Phase** - Resolve worker placements (gather resources, recruit troops, fortify walls)
4. **Battle Phase** - Mongol hordes attack wall sections
5. **Cleanup Phase** - Reset for next round

**Branching Complexity:**
- Different logic per phase (5 distinct code paths)
- Different actions per location type (7+ worker placement locations)
- Combat has multiple outcomes (successful defense, failed defense, wall breach)
- Game can end early (wall completely breached) or after max rounds
- Multiple players taking turns (player order matters)

## 🔧 What You'll Build

Convert a traditional procedural board game implementation into a durable Temporal workflow that:
- Manages multi-round, multi-phase game state
- Handles complex conditional branching based on game phase
- Executes different activities based on player actions and game state
- Determines win/loss conditions through multiple conditional paths
- (Advanced) Accepts player input via signals to influence branching

## 📂 Files Provided

### `original/` - Before Temporal
- `great_wall_game.py` - Game state models (Player, Resources, WallSection, etc.)
- `great_wall_engine.py` - Game engine with all the branching logic
- `main.py` - Demo script showing full game flow
- `state_persistence_demo.py` - Demonstrates state snapshots (like Temporal history)
- `requirements.txt` - Dependencies

### `python/` - Your Temporal Conversion
You'll create the Temporal version here

## 🎓 Key Patterns to Learn

### 1. Phase-Based Branching
The game has 5 distinct phases per round. In Temporal:

```python
@workflow.defn
class GreatWallGameWorkflow:
    @workflow.run
    async def run(self, game_id: str, players: List[str]) -> GameResult:
        state = initialize_game(game_id, players)
        
        while not state.is_game_over():
            # Round loop
            for phase in [Phase.COMMAND, Phase.WORKER_PLACEMENT, 
                         Phase.RESOLUTION, Phase.BATTLE, Phase.CLEANUP]:
                
                # Branch based on phase
                if phase == Phase.COMMAND:
                    await self.execute_command_phase(state)
                elif phase == Phase.WORKER_PLACEMENT:
                    await self.execute_worker_placement_phase(state)
                elif phase == Phase.RESOLUTION:
                    await self.execute_resolution_phase(state)
                # ... etc
```

### 2. Conditional Activity Execution
Activities are only executed when certain conditions are met:

```python
async def execute_resolution_phase(self, state: GameState):
    """Resolve worker placements - different activities per location"""
    
    for placement in state.worker_placements:
        # Branch based on location type
        if placement.location == LocationType.MARKET:
            await workflow.execute_activity(
                gather_gold,
                args=[placement.player_id, placement.advisors_used * 2],
                start_to_close_timeout=timedelta(seconds=10)
            )
        elif placement.location == LocationType.BARRACKS:
            await workflow.execute_activity(
                recruit_troops,
                args=[placement.player_id, placement.advisors_used],
                start_to_close_timeout=timedelta(seconds=10)
            )
        # ... 7+ location types, each triggers different activity
```

### 3. State-Driven Branching
Game state determines which path to take:

```python
async def execute_battle_phase(self, state: GameState):
    """Battle resolution with multiple conditional outcomes"""
    
    for horde in state.mongol_hordes:
        section = state.get_wall_section(horde.target_section)
        
        # Calculate defense strength (deterministic in workflow)
        defense_strength = section.fortification_level * 2
        defense_strength += section.defenders.total()
        
        # Execute battle activity
        result = await workflow.execute_activity(
            resolve_battle,
            args=[horde, defense_strength],
            start_to_close_timeout=timedelta(seconds=10)
        )
        
        # Branch based on battle outcome
        if result.victory:
            # Award honor points
            if section.defender_id:
                state.award_honor(section.defender_id, result.honor_earned)
        else:
            # Apply damage
            section.take_damage(result.damage)
            
            # Check for breach (game-ending condition)
            if section.is_breached():
                breached_count = sum(1 for s in state.wall_sections if s.is_breached())
                
                # Early termination branch
                if breached_count >= len(state.wall_sections) // 2:
                    state.current_phase = Phase.GAME_OVER
                    break
```

### 4. Win Condition Branching
Multiple paths can end the game:

```python
while not state.is_game_over():
    # Play round...
    
    # Check win conditions (multiple branches)
    if state.current_round > state.max_rounds:
        # Time limit reached
        break
    
    breached = sum(1 for s in state.wall_sections if s.is_breached())
    if breached >= len(state.wall_sections) // 2:
        # Wall destroyed - game over
        break
```

## 🚀 Getting Started

### 1. Study the Original Implementation
```bash
cd original/
python main.py  # Watch a full game play out
python state_persistence_demo.py  # See state evolution
```

Pay attention to:
- How many branching points exist
- Which logic should be in workflow vs activities
- Where state determines the execution path

### 2. Plan Your Workflow Boundaries

**Questions to answer:**
- Should each phase be its own workflow or activities?
- Should each round be a separate workflow or loop in one workflow?
- What logic is deterministic (workflow) vs non-deterministic (activity)?
- Where do you need signals for player input?

### 3. Identify Activities

**Examples of activities:**
- `gather_resources(player_id, resource_type, amount)` - Non-deterministic external state change
- `recruit_troops(player_id, troop_type, count)` - Modifies external database
- `resolve_battle(horde, defense_strength)` - Random dice rolls (non-deterministic)
- `spawn_mongol_hordes(round_number)` - Random generation
- `send_game_notification(player_id, message)` - External I/O

**Examples of workflow logic (deterministic):**
- Phase transitions
- Turn order management
- Win condition checking
- Resource cost validation
- Combat strength calculations

## 📊 Workflow Branching Map

```
Start Game
    ↓
┌───────────────────────────┐
│   Round Loop (1-6)        │
│   ┌──────────────────┐    │
│   │ Command Phase    │───→ Draw cards for each player
│   └──────────────────┘    │
│   ┌──────────────────┐    │
│   │ Worker Placement │───→ For each player:
│   │                  │        ├─→ Market (gather gold)
│   │                  │        ├─→ Barracks (recruit troops)
│   │                  │        ├─→ Quarry (gather stone)
│   │                  │        └─→ 7+ more locations...
│   └──────────────────┘    │
│   ┌──────────────────┐    │
│   │ Resolution       │───→ For each placement:
│   │                  │        Execute appropriate activity
│   └──────────────────┘    │
│   ┌──────────────────┐    │
│   │ Battle Phase     │───→ For each horde:
│   │                  │        ├─→ Victory? Award honor
│   │                  │        └─→ Defeat? Apply damage
│   │                  │            └─→ Breached? Check game over
│   └──────────────────┘    │
│   ┌──────────────────┐    │
│   │ Cleanup Phase    │───→ Reset for next round
│   └──────────────────┘    │
│   ↓                        │
│   Game Over?               │
│   ├─→ Yes: Calculate winner
│   └─→ No: Continue to next round
└───────────────────────────┘
```

## ⏱️ Time Estimate

**Expected time: 3-4 hours**
- 30 min: Study original code and identify branching points
- 60 min: Design workflow structure and activity boundaries
- 90-120 min: Implement workflow with all conditional branches
- 30 min: Test different execution paths

## 🎯 Success Criteria

- [ ] Game workflow manages 5 phases per round across 6 rounds
- [ ] Different activities executed based on worker placement locations
- [ ] Combat resolution branches based on battle outcomes
- [ ] Game ends correctly via multiple conditions (time limit OR wall breach)
- [ ] State-driven branching works (phase transitions, player turns)
- [ ] All non-deterministic operations (random, time) moved to activities
- [ ] Workflow properly handles early termination (wall breached)
- [ ] Can replay workflow history and see all branching decisions

## 🏆 Advanced Challenges

If you finish early:

1. **Add Signals for Player Input**
   - `place_worker(player_id, location, advisors)` signal
   - `play_command_card(player_id, card)` signal
   - Turn timeouts using `workflow.wait_condition()` with timeout

2. **Child Workflows**
   - Each battle could be a child workflow
   - Each player's turn could be a child workflow

3. **Parallel Execution**
   - Resolve all worker placements in parallel (if independent)
   - Fight all battles simultaneously

4. **Continue-As-New**
   - If you extend to 20+ rounds, use continue-as-new to manage history

## 💡 Hints

<details>
<summary>Click for hints if you get stuck</summary>

**Hint 1: Workflow Structure**
Use a single workflow for the entire game. Nested loops:
- Outer loop: Rounds (1-6)
- Middle loop: Phases (5 per round)
- Inner loops: Players, placements, battles

**Hint 2: Activity Boundaries**
Move to activities:
- Random number generation (dice rolls, card draws)
- Database updates (resource changes, troop recruitment)
- Time-based operations (timestamps)
- External I/O (notifications, emails)

Keep in workflow:
- Phase transitions
- Turn order
- Conditional branching logic
- State calculations (totals, comparisons)

**Hint 3: State Management**
Pass entire `GameState` object between activities? NO!
Pass minimal data and reconstruct in activity from database? YES!

Example:
```python
# ❌ Don't do this
await workflow.execute_activity(
    resolve_battle,
    args=[state],  # Whole state object - too much!
)

# ✅ Do this
await workflow.execute_activity(
    resolve_battle,
    args=[horde.strength, horde.target_section, defense_strength],
)
```

**Hint 4: Testing Different Branches**
Use Temporal's replay testing to verify all branches:
- Test normal game completion (6 rounds)
- Test early termination (wall breach in round 3)
- Test different worker placement combinations
- Test battle victories and defeats

</details>

## 🔗 Related Exercises

- **Exercise #3**: Hotel Reservation (basic compensation)
- **Exercise #5**: Travel Booking Saga (multi-step compensation)
- **Exercise #7** (Future): Multi-player async game with signals

## 📚 Resources

- [Temporal Workflow Determinism](https://docs.temporal.io/workflows#deterministic-constraints)
- [Conditional Logic in Workflows](https://docs.temporal.io/dev-guide/python/features#workflow-logic-requirements)
- [Workflow Signals](https://docs.temporal.io/workflows#signal)
- [Testing Workflows](https://docs.temporal.io/dev-guide/python/testing)

---

**Ready to tackle complex branching workflows? Let's temporalize The Great Wall!** 🏯🐉
