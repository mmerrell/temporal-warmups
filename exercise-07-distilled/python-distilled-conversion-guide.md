# Distilled - Temporal Conversion Guide

This directory is where you'll create your Temporal implementation of the Distilled game.

## 🎯 Your Goal

Convert the procedural game engine (`original/distilled_engine.py`) into a durable Temporal workflow that handles resource transformation pipelines and parallel aging of multiple spirits.

## 📁 Suggested File Structure

```
python/
├── models.py              # Game state dataclasses (copy from original)
├── activities.py          # Non-deterministic operations
├── workflow.py            # Game orchestration with transformation pipeline
├── worker.py              # Activity worker
├── client.py              # Start games
├── database.py            # Game database (similar to previous exercises)
└── requirements.txt       # Dependencies
```

## 🗺️ Conversion Strategy

### Step 1: Identify Resource Transformation Stages

The game has a clear transformation pipeline:
1. **Ingredients** (bought from market)
2. **Spirits** (distilled from ingredients using recipes)
3. **Aged Spirits** (aged in barrels for rounds)
4. **Money + Prestige** (sold spirits)

Each transformation is an activity that modifies external state (database).

### Step 2: Parallel Aging Pattern

**The Key Challenge:** Multiple spirits age simultaneously with different completion times.

**Simple Approach** (recommended for first implementation):
```python
@dataclass
class AgingSpirit:
    spirit_id: str
    player_id: str
    rounds_remaining: int

@workflow.defn
class DistilleryWorkflow:
    def __init__(self):
        self.aging_spirits: List[AgingSpirit] = []
    
    async def execute_age_phase(self):
        """All spirits age by 1 round each phase"""
        newly_ready = []
        
        for spirit in self.aging_spirits:
            spirit.rounds_remaining -= 1
            
            if spirit.rounds_remaining == 0:
                # Spirit finished aging
                await workflow.execute_activity(
                    mark_spirit_as_aged,
                    args=[spirit.spirit_id]
                )
                newly_ready.append(spirit)
        
        # Remove finished spirits
        for spirit in newly_ready:
            self.aging_spirits.remove(spirit)
```

**Advanced Approach** (child workflows):
```python
# When distilling a spirit that needs aging
if recipe.aging_time > 0:
    # Start a child workflow for this spirit's aging
    await workflow.start_child_workflow(
        AgeSpiritWorkflow.run,
        args=[spirit_id, recipe.aging_time, player_id],
        id=f"age-{game_id}-{spirit_id}"
    )
```

### Step 3: Design Your Activities

**Suggested Activities:**

```python
@activity.defn
async def initialize_game(game_id: str, player_names: List[str]) -> GameSetup:
    """Set up database, create players, initialize market"""
    db = get_game_database()
    # Create players, market, etc.
    return GameSetup(...)

@activity.defn
async def buy_ingredient(
    player_id: str,
    ingredient_type: str,
    quantity: int
) -> BuyResult:
    """Purchase ingredients from market - non-deterministic pricing"""
    db = get_game_database()
    market = db.get_market()
    
    # Prices might fluctuate
    price = market.get_price(ingredient_type)  # Could include randomness
    total_cost = price * quantity
    
    # Validate and execute purchase
    # ...
    return BuyResult(success=True, cost_paid=total_cost)

@activity.defn
async def distill_spirit(
    player_id: str,
    recipe_id: str
) -> SpiritCreated:
    """Create a spirit from ingredients"""
    db = get_game_database()
    
    # Generate unique spirit ID (non-deterministic)
    spirit_id = generate_spirit_id()
    
    # Consume ingredients from player inventory
    player = db.get_player(player_id)
    recipe = db.get_recipe(recipe_id)
    
    for ingredient, amount in recipe.ingredients.items():
        player.ingredients[ingredient] -= amount
    
    # Create spirit record
    spirit = Spirit(
        spirit_id=spirit_id,
        recipe=recipe,
        quality=recipe.base_quality,
        created_at=time.time()
    )
    
    db.save_spirit(spirit)
    db.save_player(player)
    
    return SpiritCreated(spirit_id=spirit_id, aging_time=recipe.aging_time)

@activity.defn
async def mark_spirit_as_aged(spirit_id: str):
    """Mark a spirit as finished aging"""
    db = get_game_database()
    spirit = db.get_spirit(spirit_id)
    spirit.is_aged = True
    spirit.quality += 1  # Aging improves quality
    db.save_spirit(spirit)

@activity.defn
async def sell_spirit(player_id: str, spirit_id: str) -> SaleResult:
    """Sell a spirit for money and prestige"""
    db = get_game_database()
    
    spirit = db.get_spirit(spirit_id)
    player = db.get_player(player_id)
    
    # Calculate sale price (with randomness)
    base_value = spirit.calculate_value()
    sale_price = base_value + random.randint(-5, 10)
    
    # Award money and prestige
    player.money += sale_price
    player.prestige += spirit.quality
    
    # Remove spirit
    db.delete_spirit(spirit_id)
    db.save_player(player)
    
    return SaleResult(price=sale_price, prestige=spirit.quality)
```

### Step 4: Workflow Structure

**Main Game Workflow:**
```python
@workflow.defn
class DistilleryGameWorkflow:
    def __init__(self):
        self.current_round = 1
        self.max_rounds = 7
        self.aging_spirits: List[AgingSpirit] = []
        self.player_ids: List[str] = []
    
    @workflow.run
    async def run(self, game_id: str, player_names: List[str]) -> GameResult:
        # Initialize
        setup = await workflow.execute_activity(
            initialize_game,
            args=[game_id, player_names],
            start_to_close_timeout=timedelta(seconds=30)
        )
        self.player_ids = setup.player_ids
        
        # Play rounds
        while self.current_round <= self.max_rounds:
            await self.execute_market_phase()
            await self.execute_distill_phase()
            await self.execute_age_phase()
            await self.execute_sell_phase()
            await self.execute_cleanup_phase()
            
            self.current_round += 1
        
        # Determine winner
        winner = await workflow.execute_activity(
            determine_winner,
            args=[self.player_ids],
            start_to_close_timeout=timedelta(seconds=10)
        )
        
        return GameResult(winner=winner)
    
    async def execute_distill_phase(self):
        """Distill spirits from ingredients"""
        for player_id in self.player_ids:
            # Check capacity
            player_aging_count = sum(
                1 for s in self.aging_spirits if s.player_id == player_id
            )
            
            # Query what recipes player can afford
            affordable = await workflow.execute_activity(
                get_affordable_recipes,
                args=[player_id],
                start_to_close_timeout=timedelta(seconds=10)
            )
            
            for recipe_id in affordable:
                if player_aging_count >= 3:  # Max capacity
                    break
                
                # Distill the spirit
                result = await workflow.execute_activity(
                    distill_spirit,
                    args=[player_id, recipe_id],
                    start_to_close_timeout=timedelta(seconds=10)
                )
                
                # Track aging if needed
                if result.aging_time > 0:
                    self.aging_spirits.append(AgingSpirit(
                        spirit_id=result.spirit_id,
                        player_id=player_id,
                        rounds_remaining=result.aging_time
                    ))
                    player_aging_count += 1
    
    async def execute_age_phase(self):
        """Age all spirits in parallel"""
        newly_ready = []
        
        for spirit in self.aging_spirits:
            spirit.rounds_remaining -= 1
            
            if spirit.rounds_remaining == 0:
                await workflow.execute_activity(
                    mark_spirit_as_aged,
                    args=[spirit.spirit_id],
                    start_to_close_timeout=timedelta(seconds=10)
                )
                newly_ready.append(spirit)
        
        # Remove aged spirits from tracking
        for spirit in newly_ready:
            self.aging_spirits.remove(spirit)
```

### Step 5: State Management

**Workflow State** (lightweight, deterministic):
```python
class DistilleryGameWorkflow:
    def __init__(self):
        # Control flow state
        self.current_round = 1
        self.current_phase = Phase.MARKET
        
        # Aging tracking (lightweight)
        self.aging_spirits: List[AgingSpirit] = []
        
        # Player IDs only (not full player data)
        self.player_ids: List[str] = []
```

**Database State** (full game state):
```python
# database.py
class GameDatabase:
    def __init__(self):
        self.players = {}  # player_id -> Player
        self.spirits = {}  # spirit_id -> Spirit
        self.market = Market()
    
    def get_player(self, player_id: str) -> Player:
        return self.players[player_id]
    
    def save_player(self, player: Player):
        self.players[player.player_id] = player
    
    # etc...
```

## ⚠️ Common Pitfalls

1. **Passing full Player objects to activities**
   - ❌ `activity(player)` - mutable workflow state
   - ✅ `activity(player_id)` - activity loads from database

2. **Using random in workflow for aging**
   - ❌ `rounds = random.randint(1, 3)` in workflow
   - ✅ Recipe defines aging time (deterministic)

3. **Forgetting capacity limits**
   - Workflow must track how many spirits each player is aging
   - Can't start new aging if at capacity

4. **Not handling spirits finishing at different times**
   - Spirit A: 1 round → finishes round 2
   - Spirit B: 3 rounds → finishes round 4
   - Must track individually

## 🧪 Testing Scenarios

**Scenario 1: Basic Transformation**
```python
# Round 1: Buy corn, yeast, water
# Round 2: Distill moonshine (no aging needed)
# Round 3: Sell moonshine immediately
```

**Scenario 2: Parallel Aging**
```python
# Round 1: Distill 3 spirits
#   - Spirit A: 1 round aging
#   - Spirit B: 2 rounds aging
#   - Spirit C: 3 rounds aging
# Round 2: Spirit A finishes, sell it
# Round 3: Spirit B finishes, sell it
# Round 4: Spirit C finishes, sell it
```

**Scenario 3: Capacity Limits**
```python
# Round 1: Distill 3 spirits (all need 2 rounds aging)
# Round 2: Try to distill another spirit
#   → Fails! At capacity (3/3 aging)
# Round 3: First batch finishes aging, now can distill again
```

## 🎯 Success Checklist

- [ ] Market phase purchases ingredients via activities
- [ ] Distill phase consumes ingredients and creates spirits
- [ ] Capacity limits prevent over-distilling
- [ ] Multiple spirits age in parallel
- [ ] Spirits with different aging times finish correctly
- [ ] Age phase decrements all timers simultaneously
- [ ] Sell phase converts spirits to money + prestige
- [ ] Game runs for 7 rounds without errors
- [ ] Workflow survives worker crashes during aging
- [ ] Can query current aging spirits mid-game

## 💡 Debugging Tips

**View aging spirits:**
```python
# Add a query to your workflow
@workflow.query
def get_aging_spirits(self) -> List[Dict]:
    return [
        {
            "spirit_id": s.spirit_id,
            "player_id": s.player_id,
            "rounds_left": s.rounds_remaining
        }
        for s in self.aging_spirits
    ]
```

**Test aging logic:**
```python
# Create test with known aging times
# Verify spirits finish at correct rounds
async def test_parallel_aging():
    # Round 1: Distill spirits with aging 1, 2, 3
    # Round 2: Verify only aging-1 spirit finished
    # Round 3: Verify only aging-2 spirit finished
    # Round 4: Verify only aging-3 spirit finished
```

## 🚀 Ready to Start?

1. Copy `models.py` from original
2. Create `database.py` - similar to previous exercises
3. Create `activities.py` - start with core 5-6 activities
4. Create `workflow.py` - focus on resource transformation flow
5. Test incrementally - get one phase working before adding others

**The key insight:** This is a pipeline workflow where resources transform through stages, with parallel processing during the aging stage!

Good luck crafting artisan spirits with Temporal! 🥃✨
