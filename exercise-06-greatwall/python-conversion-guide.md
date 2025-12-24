# The Great Wall - Temporal Conversion Guide

This directory is where you'll create your Temporal implementation of The Great Wall game.

## 🎯 Your Goal

Convert the procedural game engine (`original/great_wall_engine.py`) into a durable Temporal workflow that maintains game state through crashes, restarts, and failures.

## 📁 Suggested File Structure

```
python/
├── models.py              # Game state dataclasses (copy from original)
├── activities.py          # Non-deterministic operations
├── workflow.py            # Game orchestration with branching logic
├── worker.py              # Activity worker
├── client.py              # Start games
├── requirements.txt       # Dependencies
└── docker-compose.yml     # Temporal server (optional)
```

## 🗺️ Conversion Strategy

### Step 1: Identify the Workflow Boundary

**Question:** Should the entire game be one workflow, or should each round/phase be separate workflows?

**Recommendation:** Start with **one workflow for the entire game**. This makes state management simpler and matches the original design.

```python
@workflow.defn
class GreatWallGameWorkflow:
    @workflow.run
    async def run(self, game_id: str, player_names: List[str]) -> GameResult:
        # Entire game lives in this workflow
        pass
```

### Step 2: Separate Deterministic vs Non-Deterministic Logic

**Deterministic (keep in workflow):**
- Phase transitions: `Phase.COMMAND` → `Phase.WORKER_PLACEMENT`
- Turn order: Player 1 → Player 2 → Player 3
- Win condition checks: `if current_round > max_rounds`
- Combat calculations: `defense_strength = fortification * 2 + troops`
- Resource cost validation: `can_afford(cost)`

**Non-Deterministic (move to activities):**
- Random number generation: `random.randint()`, `random.choice()`
- Time operations: `time.time()`, `datetime.now()`
- Database operations: Resource updates, troop recruitment
- External I/O: Print statements, logging, notifications

### Step 3: Design Your Activities

**Suggested Activities:**

```python
@activity.defn
async def initialize_game(game_id: str, player_names: List[str]) -> GameState:
    """Set up database, create players, initialize rooms"""
    pass

@activity.defn
async def draw_command_card(player_id: str) -> str:
    """Random card selection - non-deterministic"""
    pass

@activity.defn
async def gather_resources(player_id: str, resource_type: str, amount: int):
    """Update database with resource change"""
    pass

@activity.defn
async def recruit_troops(player_id: str, troop_type: str, count: int):
    """Add troops to player in database"""
    pass

@activity.defn
async def fortify_wall(section_id: int, amount: int):
    """Increase wall fortification in database"""
    pass

@activity.defn
async def spawn_mongol_hordes(round_number: int) -> List[MongolHorde]:
    """Generate random hordes based on round"""
    pass

@activity.defn
async def resolve_battle(
    horde_strength: int,
    defense_strength: int
) -> BattleResult:
    """Dice rolls and combat resolution"""
    pass

@activity.defn
async def award_honor(player_id: str, points: int):
    """Update player honor in database"""
    pass

@activity.defn
async def get_game_state(game_id: str) -> GameState:
    """Fetch current state from database"""
    pass
```

### Step 4: Handle Branching Logic

**Phase Branching:**
```python
# In your workflow
for phase in self.phases:
    if phase == Phase.COMMAND:
        await self.execute_command_phase()
    elif phase == Phase.WORKER_PLACEMENT:
        await self.execute_worker_placement_phase()
    # etc...
```

**Worker Placement Location Branching:**
```python
for placement in worker_placements:
    if placement.location == LocationType.MARKET:
        await workflow.execute_activity(
            gather_resources,
            args=[placement.player_id, "gold", placement.advisors * 2]
        )
    elif placement.location == LocationType.BARRACKS:
        await workflow.execute_activity(
            recruit_troops,
            args=[placement.player_id, "infantry", placement.advisors]
        )
    # etc... 7+ location types
```

**Combat Outcome Branching:**
```python
battle_result = await workflow.execute_activity(
    resolve_battle,
    args=[horde.strength, defense_strength]
)

if battle_result.victory:
    # Award honor
    await workflow.execute_activity(
        award_honor,
        args=[defender_id, battle_result.honor_earned]
    )
else:
    # Apply damage and check for breach
    section.damage += battle_result.damage
    
    if section.is_breached():
        # Check game-ending condition
        breached_count = count_breached_sections()
        if breached_count >= len(wall_sections) // 2:
            self.game_over = True
            break
```

### Step 5: State Management Pattern

**Don't pass entire GameState to activities:**
```python
# ❌ Bad - passing mutable workflow state
await workflow.execute_activity(
    some_activity,
    args=[self.state]  # Don't do this!
)
```

**Do pass specific values and use database:**
```python
# ✅ Good - specific immutable values
await workflow.execute_activity(
    recruit_troops,
    args=[player_id, troop_type, count]
)

# Activity reads/writes from database
@activity.defn
async def recruit_troops(player_id: str, troop_type: str, count: int):
    db = get_game_database()
    player = db.get_player(player_id)
    player.troops.add(troop_type, count)
    db.save_player(player)
```

**Workflow maintains lightweight state for branching decisions:**
```python
@workflow.defn
class GreatWallGameWorkflow:
    def __init__(self):
        self.current_round = 1
        self.current_phase = Phase.COMMAND
        self.game_over = False
        self.wall_sections_breached = 0
        # Minimal state for control flow only
```

### Step 6: Test Different Execution Paths

Make sure your workflow handles:

1. **Normal completion:** 6 rounds, all phases complete
2. **Early termination:** Wall breached in round 3
3. **Different worker placements:** All 7+ location types
4. **Battle outcomes:** Victories, defeats, breaches
5. **Activity failures:** Retries work correctly

## 🔧 Database Design

You'll need a game database (similar to Exercise #3):

```python
# database.py
class GameDatabase:
    def __init__(self):
        self.games = {}  # game_id -> GameState
        self.players = {}  # player_id -> Player
        self.wall_sections = {}  # section_id -> WallSection
    
    def get_game(self, game_id: str) -> GameState:
        pass
    
    def save_game(self, game: GameState):
        pass
    
    def update_resources(self, player_id: str, resource_type: str, amount: int):
        pass
    
    # etc...
```

## ⚠️ Common Pitfalls

1. **Using `random` in workflow**
   - ❌ `random.randint()` in workflow code
   - ✅ `random.randint()` in activity, return result to workflow

2. **Using `time` in workflow**
   - ❌ `time.time()` or `datetime.now()` in workflow
   - ✅ Use `workflow.now()` or move to activity

3. **Printing from workflow**
   - ❌ `print(f"Player {name} won!")` in workflow
   - ✅ Use `workflow.logger` or move to activity

4. **Passing mutable state to activities**
   - ❌ `activity(self.game_state)`
   - ✅ `activity(player_id, specific_values)`

5. **Forgetting retry policies**
   - Activities should have appropriate timeout and retry settings

## 🧪 Testing Tips

**Manual Testing:**
```python
# client.py
async def main():
    game_result = await client.execute_workflow(
        GreatWallGameWorkflow.run,
        args=["game_001", ["Alice", "Bob", "Carol"]],
        id="game-workflow-001",
        task_queue="great-wall-queue"
    )
    print(f"Winner: {game_result.winner_name}")
```

**Replay Testing:**
```python
# Test that workflow is deterministic
async def test_workflow_replay():
    # Run workflow once
    result1 = await run_workflow(...)
    
    # Replay from history
    result2 = await replay_workflow(...)
    
    # Should be identical
    assert result1 == result2
```

## 🎯 Success Checklist

- [ ] Workflow completes 6 rounds successfully
- [ ] All 5 phases execute in order each round
- [ ] Worker placements trigger correct activities
- [ ] Battles resolve with proper outcomes
- [ ] Game ends early if wall breaches
- [ ] Game ends after 6 rounds if wall holds
- [ ] Winner is determined correctly
- [ ] Workflow survives worker crashes (test by stopping worker mid-game)
- [ ] No non-deterministic operations in workflow code
- [ ] All random/time operations moved to activities

## 💡 Stuck? Check These

1. Review `original/great_wall_engine.py` - which methods have random/time?
2. Look at `state_persistence_demo.py` - see how state snapshots work
3. Study Exercise #3 (Hotel Reservation) - similar state management pattern
4. Review Exercise #5 (Travel Booking) - similar multi-step orchestration

## 🚀 Ready to Start?

1. Copy `models.py` from original (or adapt as needed)
2. Create `activities.py` - start with 3-4 core activities
3. Create `workflow.py` - start with just the round loop
4. Create `worker.py` and `client.py` (similar to previous exercises)
5. Test incrementally - get one phase working before adding others
6. Add branching complexity gradually

**Good luck! The branching logic is challenging but rewarding when it works!** 🏯
