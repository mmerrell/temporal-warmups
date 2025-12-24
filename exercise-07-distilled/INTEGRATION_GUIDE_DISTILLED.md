# Adding Exercise #7 (Distilled) to temporal-warmups Repository

## 📂 Directory Structure to Create

```
temporal-warmups/
├── exercise-01-registration/
├── exercise-02-[...]/
├── exercise-03-hotel-reservation/
├── exercise-04-[...]/
├── exercise-05-travel-booking/
├── exercise-06-great-wall/
└── exercise-07-distilled/                ← NEW
    ├── README.md                          ← Exercise description & objectives
    ├── original/                          ← Pre-Temporal code
    │   ├── distilled_game.py             ← Data models (Spirit, Recipe, Market, etc.)
    │   ├── distilled_engine.py           ← Game engine (to be temporalized)
    │   ├── distilled_main.py             ← Demo scripts
    │   └── requirements.txt              ← Python dependencies
    └── python/                            ← Your Temporal conversion (empty to start)
        └── README.md                      ← Conversion hints/guide
```

## 📋 Files to Copy

### From Claude's outputs:
1. **Main README**: `exercise-07-distilled-README.md` → `exercise-07-distilled/README.md`
2. **Conversion Guide**: `python-distilled-conversion-guide.md` → `exercise-07-distilled/python/README.md`
3. **Game Models**: `distilled_game.py` → `exercise-07-distilled/original/distilled_game.py`
4. **Game Engine**: `distilled_engine.py` → `exercise-07-distilled/original/distilled_engine.py`
5. **Demo Script**: `distilled_main.py` → `exercise-07-distilled/original/distilled_main.py`
6. **Requirements**: `distilled_requirements.txt` → `exercise-07-distilled/original/requirements.txt`

## 🚀 Quick Setup Commands

```bash
# Navigate to your temporal-warmups repo
cd /path/to/temporal-warmups

# Create directory structure
mkdir -p exercise-07-distilled/original
mkdir -p exercise-07-distilled/python

# Copy files from Claude outputs
cp ~/Downloads/exercise-07-distilled-README.md exercise-07-distilled/README.md
cp ~/Downloads/python-distilled-conversion-guide.md exercise-07-distilled/python/README.md
cp ~/Downloads/distilled_game.py exercise-07-distilled/original/
cp ~/Downloads/distilled_engine.py exercise-07-distilled/original/
cp ~/Downloads/distilled_main.py exercise-07-distilled/original/
cp ~/Downloads/distilled_requirements.txt exercise-07-distilled/original/requirements.txt

# Test that original code works
cd exercise-07-distilled/original
python distilled_main.py

# Commit to GitHub
cd ../..  # Back to repo root
git add exercise-07-distilled/
git commit -m "Add Exercise #7: Distilled - Resource Transformation & Parallel Execution"
git push origin main
```

## 📝 Update Main README

Add Exercise #7 to your main `temporal-warmups/README.md`:

```markdown
### Exercise #7: Distilled ⭐⭐⭐⭐
**Language:** Python  
**Time:** ~3-4 hours  
**Concepts:** Resource transformation, parallel execution, recipe/crafting workflows, capacity constraints

**Scenario:** Distillery management game where players buy ingredients, distill spirits 
using recipes, age them in barrels (parallel aging with different completion times), 
and sell them for money and prestige.

**Focus:**
- Resource transformation pipelines (ingredients → spirits → aged spirits → money)
- Parallel execution (3-5 spirits aging simultaneously)
- Different completion times (1-3 round aging per spirit)
- Capacity constraints (can't distill if aging capacity is full)
- Recipe-based crafting workflows

**Why This Exercise:**
Real-world systems often involve multi-stage resource transformations with parallel 
processing. This exercise teaches pipeline workflows, parallel execution with different 
completion times, and capacity-constrained orchestration. Perfect for manufacturing 
systems, batch processing, or any workflow where resources transform through stages.
```

## 🎯 Exercise Positioning in Curriculum

**Week 3-4: Realistic Scenarios**
- Exercise #3: Hotel Reservation (compensation intro)
- Exercise #5: Travel Booking (saga pattern)
- Exercise #6: The Great Wall (complex branching)
- **Exercise #7: Distilled (resource transformation & parallel execution)** ← NEW

**Learning Progression:**
- Exercises 1-2: Linear workflows
- Exercises 3-5: Compensations and multi-step orchestration
- Exercise 6: Branching and conditional logic
- **Exercise 7: Resource transformation and parallel execution**
- Future: Fan-out/fan-in, child workflows, signals

## 🔗 Exercise Dependencies

**Prerequisites:**
- Exercise #1: Basic workflow/activity pattern
- Exercise #3: External state management (database)

**Builds toward:**
- Exercise #8+: Child workflows for each aging spirit
- Exercise #9+: Full fan-out/fan-in patterns
- Exercise #10+: Complex parallel orchestration

## ✅ Verification Checklist

Before committing, verify:

- [ ] `exercise-07-distilled/README.md` exists and renders properly on GitHub
- [ ] `exercise-07-distilled/original/` contains all 4 files
- [ ] `exercise-07-distilled/python/README.md` provides conversion guidance
- [ ] `python distilled_main.py` runs from original/ directory
- [ ] Main repo README.md updated with Exercise #7 entry
- [ ] All files use consistent formatting and style

## 🎓 Teaching Notes

**Key Learning Moments:**
1. **Resource transformation pipeline**: Students see how data transforms through stages
2. **Parallel aging**: Multiple processes running with different completion times
3. **Capacity constraints**: Real-world resource limitations affect workflow design
4. **State management**: Lightweight tracking in workflow vs full state in database

**Common Struggles:**
1. Tracking multiple aging spirits with different timers
2. Enforcing capacity limits (can't distill if aging capacity full)
3. Deciding when spirits should be child workflows vs simple state
4. Handling spirits that finish aging at different rounds

**Instructor Tips:**
- Start with simple approach (aging timers in workflow state)
- Progress to child workflows once basic version works
- Emphasize the transformation pipeline concept
- Show how workflow state stays lightweight (just timers, not full spirit data)
- Use Temporal UI to visualize parallel aging

## 🚀 Next Steps

After adding Exercise #7:

1. **Test the original code** - Verify it runs and shows parallel aging
2. **Review the README** - Ensure learning objectives are clear
3. **Try the exercise yourself** - Validate difficulty and time estimate
4. **Consider child workflow variation** - Could be an advanced challenge
5. **Update curriculum** - Add insights about resource transformation patterns

---

**You're adding a crucial workflow pattern to the curriculum!** 🥃

Distilled teaches resource transformation and parallel execution - patterns that appear in:
- Manufacturing pipelines
- Batch processing systems
- Multi-stage data transformations
- Game crafting systems
- Any workflow where resources flow through transformation stages

This is an essential pattern for real-world systems! 🚀
