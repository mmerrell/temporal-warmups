# Adding Exercise #6 to temporal-warmups Repository

## 📂 Directory Structure to Create

```
temporal-warmups/
├── exercise-01-registration/
├── exercise-02-[...]/
├── exercise-03-hotel-reservation/
├── exercise-04-[...]/
├── exercise-05-travel-booking/
└── exercise-06-great-wall/              ← NEW
    ├── README.md                         ← Exercise description & objectives
    ├── original/                         ← Pre-Temporal code
    │   ├── great_wall_game.py           ← Data models
    │   ├── great_wall_engine.py         ← Game engine (to be temporalized)
    │   ├── main.py                       ← Demo script
    │   ├── state_persistence_demo.py    ← State snapshot concepts
    │   └── requirements.txt             ← Python dependencies
    └── python/                           ← Your Temporal conversion (empty to start)
        └── README.md                     ← Conversion hints/guide
```

## 📋 Files to Copy

### From Claude's outputs:
1. **Main README**: `exercise-06-README.md` → `exercise-06-great-wall/README.md`
2. **Conversion Guide**: `python-conversion-guide.md` → `exercise-06-great-wall/python/README.md`
3. **Game Models**: `great_wall_game.py` → `exercise-06-great-wall/original/great_wall_game.py`
4. **Game Engine**: `great_wall_engine.py` → `exercise-06-great-wall/original/great_wall_engine.py`
5. **Demo Script**: `main.py` → `exercise-06-great-wall/original/main.py`
6. **State Demo**: `state_persistence_demo.py` → `exercise-06-great-wall/original/state_persistence_demo.py`
7. **Requirements**: `requirements.txt` → `exercise-06-great-wall/original/requirements.txt`

## 🚀 Quick Setup Commands

```bash
# Navigate to your temporal-warmups repo
cd /path/to/temporal-warmups

# Create directory structure
mkdir -p exercise-06-great-wall/original
mkdir -p exercise-06-great-wall/python

# Copy files from Claude outputs
cp ~/Downloads/exercise-06-README.md exercise-06-great-wall/README.md
cp ~/Downloads/python-conversion-guide.md exercise-06-great-wall/python/README.md
cp ~/Downloads/great_wall_game.py exercise-06-great-wall/original/
cp ~/Downloads/great_wall_engine.py exercise-06-great-wall/original/
cp ~/Downloads/main.py exercise-06-great-wall/original/
cp ~/Downloads/state_persistence_demo.py exercise-06-great-wall/original/
cp ~/Downloads/requirements.txt exercise-06-great-wall/original/

# Test that original code works
cd exercise-06-great-wall/original
python main.py

# Commit to GitHub
cd ../..  # Back to repo root
git add exercise-06-great-wall/
git commit -m "Add Exercise #6: The Great Wall - Complex Branching Workflows"
git push origin main
```

## 📝 Update Main README

Add Exercise #6 to your main `temporal-warmups/README.md`:

```markdown
### Exercise #6: The Great Wall ⭐⭐⭐⭐
**Language:** Python  
**Time:** ~3-4 hours  
**Concepts:** Complex branching, phase-based workflows, state-driven execution, conditional logic

**Scenario:** Turn-based board game with 5 distinct phases per round across 6 rounds. 
Players defend the Great Wall against Mongol invasions through worker placement, 
resource management, and combat.

**Focus:**
- Phase-based workflow branching (5 different execution paths)
- Nested loops (rounds → phases → players → actions)
- Conditional activity execution based on game state
- Multiple win/loss conditions
- State-driven decision trees

**Why This Exercise:**
Most real-world workflows have complex branching logic. This exercise teaches you 
to manage conditional execution, nested loops, and state-driven decisions in 
Temporal workflows. Perfect preparation for game systems, approval workflows, 
or any multi-stage processes with conditional paths.
```

## 🎯 Exercise Positioning in Curriculum

**Week 3-4: Realistic Scenarios**
- Exercise #3: Hotel Reservation (compensation intro)
- Exercise #5: Travel Booking (saga pattern)
- **Exercise #6: The Great Wall (complex branching)** ← NEW

**Learning Progression:**
- Exercises 1-2: Linear workflows (A → B → C)
- Exercises 3-5: Compensations and multi-step orchestration
- **Exercise 6: Branching and conditional logic**
- Future exercises: Parallel execution, signals, child workflows

## 🔗 Exercise Dependencies

**Prerequisites:**
- Exercise #1: Basic workflow/activity pattern
- Exercise #3: External state management (database)

**Builds toward:**
- Exercise #7+: Signals for player input
- Exercise #8+: Child workflows for complex sub-systems
- Exercise #9+: Parallel execution (fan-out/fan-in)

## ✅ Verification Checklist

Before committing, verify:

- [ ] `exercise-06-great-wall/README.md` exists and renders properly on GitHub
- [ ] `exercise-06-great-wall/original/` contains all 5 Python files
- [ ] `exercise-06-great-wall/python/README.md` provides conversion guidance
- [ ] `python main.py` runs from original/ directory
- [ ] Main repo README.md updated with Exercise #6 entry
- [ ] All files use consistent formatting and style
- [ ] No sensitive data or API keys in any files

## 📚 Documentation Updates

Consider updating these additional files:

1. **Main README.md**: Add Exercise #6 to the exercise list
2. **.gitignore**: Ensure it covers Python cache files
3. **Curriculum doc** (if you have one): Add to Week 3-4 section
4. **Progress tracker**: Mark Exercise #6 as "Available, Not Yet Attempted"

## 🎓 Teaching Notes

**Key Learning Moments:**
1. **"Aha" on branching**: Students realize workflow can handle complex decision trees
2. **State management**: Lightweight state in workflow vs full state in database
3. **Determinism**: All random/time operations must be in activities
4. **Testing branches**: How to verify all conditional paths work correctly

**Common Struggles:**
1. Trying to pass entire GameState to activities (mutable state issue)
2. Using `random` or `time` in workflow (non-deterministic)
3. Over-complicating the workflow structure (child workflows when not needed)
4. Forgetting to handle early termination (wall breach before round 6)

**Instructor Tips:**
- Start with just the round loop, add phases incrementally
- Test each phase independently before combining
- Use Temporal UI to visualize the branching execution
- Show workflow replay to demonstrate determinism

## 🚀 Next Steps

After adding Exercise #6:

1. **Test the original code** - Make sure it runs on your machine
2. **Review the README** - Ensure learning objectives are clear
3. **Commit to GitHub** - Use descriptive commit message
4. **Try the exercise yourself** - Validate it's achievable in 3-4 hours
5. **Gather feedback** - If others try it, collect their experiences
6. **Update curriculum** - Reflect any insights back into the main curriculum doc

---

**You're adding a challenging but valuable exercise to the curriculum!** 🎉

The Great Wall demonstrates complex branching workflows that students will encounter 
in real-world systems: approval workflows, game systems, multi-stage processes with 
conditional logic. This is a key skill that's hard to learn without hands-on practice.

Good luck with the conversion! 🏯
