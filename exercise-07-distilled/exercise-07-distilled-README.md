# Exercise #7: Distilled - Resource Transformation & Parallel Execution

## 🎯 Learning Objectives

This exercise focuses on **resource transformation workflows** and **parallel execution patterns**:

1. **Resource transformation pipelines** - Ingredients → Spirits → Aged Spirits
2. **Parallel execution** - Multiple spirits aging simultaneously
3. **Recipe/crafting patterns** - Different recipes require different ingredients and processes
4. **Time-based workflows** - Spirits age over multiple rounds
5. **Capacity constraints** - Limited distilling and aging capacity per player
6. **Market simulation** - Dynamic buying and selling with price fluctuations

## 📋 Scenario

You're building a distillery management system for "Distilled" - a board game about crafting artisan spirits. Players buy ingredients, distill spirits using recipes, age them in barrels, and sell them for money and prestige.

**Game Flow (5 phases per round, 7 rounds total):**
1. **Market Phase** - Buy ingredients from the market
2. **Distill Phase** - Craft spirits using recipes
3. **Age Phase** - Advance aging on all spirits in barrels (parallel)
4. **Sell Phase** - Sell finished spirits for money and prestige
5. **Cleanup Phase** - Restock market, buy equipment

**Resource Transformation:**
```
Ingredients → (Distill) → Spirit → (Age) → Aged Spirit → (Sell) → Money + Prestige
```

**Parallel Execution Challenge:**
- Player can age 3-5 spirits simultaneously
- Each spirit has different aging time (0-3 rounds)
- All age in parallel, finishing at different times
- New spirits can't start aging if at capacity

## 🔧 What You'll Build

Convert a traditional game engine into a Temporal workflow that:
- Manages multi-round, multi-phase resource transformation
- Handles parallel aging of multiple spirits with different completion times
- Implements recipe-based crafting workflows
- Manages capacity constraints (can't distill if aging capacity full)
- Simulates market dynamics for buying/selling

## 📂 Files Provided

### `original/` - Before Temporal
- `distilled_game.py` - Game state models (Player, Spirit, Recipe, Market, etc.)
- `distilled_engine.py` - Game engine with resource transformation logic
- `distilled_main.py` - Demo scripts showing game flow
- `requirements.txt` - Dependencies

### `python/` - Your Temporal Conversion
You'll create the Temporal version here

## 🎓 Key Patterns to Learn

### 1. Resource Transformation Pipeline

The core pattern: ingredients transform through multiple stages

```python
@workflow.defn
class DistilleryWorkflow:
    @workflow.run
    async def run(self, game_id: str, player_names: List[str]) -> GameResult:
        state = initialize_game(game_id, player_names)
        
        while not state.is_game_over():
            # Buy ingredients (external activity)
            await self.execute_market_phase(state)
            
            # Transform: ingredients → spirits
            await self.execute_distill_phase(state)
            
            # Transform: spirits → aged spirits (parallel)
            await self.execute_age_phase(state)
            
            # Transform: aged spirits → money + prestige
            await self.execute_sell_phase(state)
            
            await self.execute_cleanup_phase(state)
```

### 2. Parallel Aging with Different Completion Times

**The Challenge:** Multiple spirits aging simultaneously, each with different timers

```python
async def execute_age_phase(self, state: GameState):
    """Age all spirits in parallel"""
    
    for player in state.players:
        # All spirits age simultaneously
        aging_tasks = []
        
        for spirit in player.spirits_aging:
            # Each spirit might finish at a different round
            spirit.aging_rounds_remaining -= 1
            
            if spirit.aging_rounds_remaining <= 0:
                # This spirit finished aging
                result = await workflow.execute_activity(
                    finish_aging,
                    args=[spirit.spirit_id],
                    start_to_close_timeout=timedelta(seconds=10)
                )
                
                # Move to ready list
                state.move_spirit_to_ready(player.player_id, spirit.spirit_id)
```

**Alternative Pattern: Child Workflows for Each Spirit**
```python
# When distilling a spirit that needs aging
if recipe.aging_time > 0:
    # Start a child workflow to handle aging
    await workflow.start_child_workflow(
        AgeSpiritWorkflow.run,
        args=[spirit.spirit_id, recipe.aging_time],
        id=f"age-{spirit.spirit_id}"
    )
```

### 3. Recipe-Based Crafting

Different recipes require different resources and produce different results:

```python
async def execute_distill_phase(self, state: GameState):
    for player in state.players:
        for recipe in available_recipes:
            # Check if player has ingredients
            if player.has_ingredients(recipe.ingredients):
                # Validate in workflow (deterministic)
                can_distill = (
                    len(player.spirits_aging) < player.aging_capacity and
                    player.distill_count < player.distill_capacity
                )
                
                if can_distill:
                    # Consume ingredients (activity - modifies external state)
                    await workflow.execute_activity(
                        consume_ingredients,
                        args=[player.player_id, recipe.ingredients]
                    )
                    
                    # Create spirit (activity - generates ID, adds to DB)
                    spirit_id = await workflow.execute_activity(
                        create_spirit,
                        args=[recipe, player.equipment_bonus]
                    )
                    
                    # Track in workflow state
                    state.add_spirit_to_aging(player.player_id, spirit_id, recipe.aging_time)
```

### 4. Capacity Constraints

Players can't distill unlimited spirits - aging capacity is limited:

```python
@workflow.defn
class DistilleryWorkflow:
    def __init__(self):
        self.player_aging_counts: Dict[str, int] = {}
        self.player_aging_capacity: Dict[str, int] = {}
    
    async def can_distill_spirit(self, player_id: str) -> bool:
        """Check if player has aging capacity available"""
        current = self.player_aging_counts.get(player_id, 0)
        capacity = self.player_aging_capacity.get(player_id, 3)
        return current < capacity
    
    async def execute_distill_phase(self, state: GameState):
        for player in state.players:
            # Only distill if under capacity
            if not self.can_distill_spirit(player.player_id):
                workflow.logger.info(f"{player.name} at aging capacity")
                continue
            
            # Distill spirit...
            self.player_aging_counts[player.player_id] += 1
```

### 5. Market Simulation

Dynamic market with changing prices and availability:

```python
@activity.defn
async def buy_ingredients(
    player_id: str,
    ingredient_type: str,
    quantity: int
) -> BuyResult:
    """Buy ingredients from market (non-deterministic prices)"""
    db = get_game_database()
    market = db.get_market()
    
    # Prices might fluctuate
    base_price = market.prices[ingredient_type]
    price = base_price + random.randint(-1, 2)
    
    total_cost = price * quantity
    
    # Check player money and market availability
    player = db.get_player(player_id)
    if player.money < total_cost:
        return BuyResult(success=False, reason="Insufficient funds")
    
    if market.available[ingredient_type] < quantity:
        return BuyResult(success=False, reason="Not enough in stock")
    
    # Execute purchase
    player.money -= total_cost
    player.ingredients[ingredient_type] += quantity
    market.available[ingredient_type] -= quantity
    
    db.save_player(player)
    db.save_market(market)
    
    return BuyResult(success=True, cost_paid=total_cost)
```

## 📊 Workflow Execution Map

```
Start Game
    ↓
┌─────────────────────────────────────┐
│   Round Loop (1-7)                  │
│   ┌───────────────────────────┐     │
│   │ Market Phase              │────→ Players buy ingredients
│   └───────────────────────────┘     │
│   ┌───────────────────────────┐     │
│   │ Distill Phase             │────→ For each player:
│   │                           │         For each recipe:
│   │                           │           Check ingredients ✓
│   │                           │           Check capacity ✓
│   │                           │           Consume ingredients
│   │                           │           Create spirit
│   │                           │           ├─→ Age 0: Ready immediately
│   │                           │           ├─→ Age 1-3: To aging barrel
│   └───────────────────────────┘     │
│   ┌───────────────────────────┐     │
│   │ Age Phase (PARALLEL)      │────→ For each aging spirit:
│   │                           │         Decrement timer
│   │  Spirit A: 2 → 1          │         If timer = 0:
│   │  Spirit B: 1 → 0 ✓        │           Mark as aged
│   │  Spirit C: 3 → 2          │           Move to ready
│   └───────────────────────────┘     │
│   ┌───────────────────────────┐     │
│   │ Sell Phase                │────→ For each ready spirit:
│   │                           │         Calculate value
│   │                           │         Sell for $ + prestige
│   └───────────────────────────┘     │
│   ┌───────────────────────────┐     │
│   │ Cleanup Phase             │────→ Restock market
│   │                           │      Buy equipment
│   └───────────────────────────┘     │
│   ↓                                  │
│   Next round or game over?           │
└─────────────────────────────────────┘
```

## ⏱️ Time Estimate

**Expected time: 3-4 hours**
- 30 min: Study original code and identify transformation stages
- 60 min: Design workflow structure and activity boundaries
- 90-120 min: Implement resource transformation and parallel aging
- 30 min: Test different recipes and aging scenarios

## 🎯 Success Criteria

- [ ] Game workflow manages 7 rounds with 5 phases each
- [ ] Market phase buys ingredients via activities
- [ ] Distill phase transforms ingredients into spirits using recipes
- [ ] Age phase handles parallel aging (3-5 spirits at once)
- [ ] Spirits with different aging times finish at correct rounds
- [ ] Capacity constraints prevent over-distilling
- [ ] Sell phase converts spirits to money and prestige
- [ ] All non-deterministic operations (random, time) in activities
- [ ] Workflow survives worker restarts during aging
- [ ] Can query current aging status of all spirits

## 🏆 Advanced Challenges

1. **Child Workflows for Aging**
   - Each spirit's aging process is a child workflow
   - Parent workflow coordinates multiple child workflows
   - Child workflows complete at different times

2. **Parallel Distillation**
   - Distill multiple spirits in parallel using activities
   - Use `workflow.execute_activities()` with parallel execution

3. **Dynamic Capacity Upgrades**
   - Players can buy equipment to increase capacity mid-game
   - Workflow must adapt capacity limits dynamically

4. **Continue-As-New for Long Games**
   - If extending to 20+ rounds, use continue-as-new

5. **Signals for Player Actions**
   - `buy_ingredient(player_id, ingredient, qty)` signal
   - `select_recipe(player_id, recipe_id)` signal
   - `sell_spirit(player_id, spirit_id)` signal

## 💡 Hints

<details>
<summary>Click for hints if you get stuck</summary>

**Hint 1: Tracking Aging Spirits**
Keep lightweight state in workflow about what's aging:
```python
@dataclass
class AgingSpirit:
    spirit_id: str
    player_id: str
    rounds_remaining: int

class DistilleryWorkflow:
    def __init__(self):
        self.aging_spirits: List[AgingSpirit] = []
```

**Hint 2: Activity Boundaries**
Move to activities:
- Random number generation (market prices, sale prices)
- Database updates (inventory, money, spirits)
- Spirit ID generation
- Time-based operations

Keep in workflow:
- Recipe validation logic
- Capacity checks
- Phase transitions
- Aging countdown (deterministic)

**Hint 3: Parallel Aging**
You don't need complex parallelism - all spirits age by 1 each round:
```python
# Each round, decrement ALL aging timers by 1
for spirit in self.aging_spirits:
    spirit.rounds_remaining -= 1
    if spirit.rounds_remaining == 0:
        # Spirit finished - move to ready
```

**Hint 4: Child Workflows (Advanced)**
If you want each spirit to be a child workflow:
```python
# When distilling
spirit_workflow_id = await workflow.start_child_workflow(
    AgeSpiritWorkflow.run,
    args=[spirit_id, aging_time],
    id=f"age-spirit-{spirit_id}"
)

# Check completion later
status = await workflow.get_child_workflow_handle(spirit_workflow_id).query(...)
```

</details>

## 🔗 Related Exercises

- **Exercise #3**: Hotel Reservation (external state management)
- **Exercise #6**: The Great Wall (complex branching)
- **Future exercises**: Fan-out/Fan-in patterns, Child workflows

## 📚 Resources

- [Temporal Child Workflows](https://docs.temporal.io/workflows#child-workflows)
- [Parallel Execution Patterns](https://docs.temporal.io/dev-guide/python/features#activity-execution)
- [Continue-As-New](https://docs.temporal.io/workflows#continue-as-new)
- [Workflow Queries for Live Data](https://docs.temporal.io/workflows#query)

---

**Ready to master resource transformation and parallel execution?** 🥃✨

This exercise teaches crucial patterns:
- Multi-stage transformation pipelines
- Parallel execution with different completion times
- Capacity-constrained workflows
- Recipe/template-based processing

These patterns apply to:
- Manufacturing pipelines
- Batch processing systems
- Game systems with crafting
- Any workflow where resources transform through stages
