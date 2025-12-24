"""
The Great Wall - Game Engine
Core game logic and phase management
"""

from typing import Dict, List, Optional, Tuple
import random
from great_wall_game import (
    GameState, Player, WallSection, MongolHorde, WorkerPlacement,
    Phase, LocationType, ResourceType, TroopType, Resources, Troops
)


class GreatWallGame:
    """Main game engine"""
    
    def __init__(self, game_id: str, player_names: List[str]):
        """Initialize a new game"""
        self.state = self._create_initial_state(game_id, player_names)
    
    def _create_initial_state(self, game_id: str, player_names: List[str]) -> GameState:
        """Create initial game state"""
        players = {}
        for i, name in enumerate(player_names):
            player_id = f"player_{i+1}"
            players[player_id] = Player(
                player_id=player_id,
                name=name,
                resources=Resources(gold=3, wood=2, stone=1, chi=3),
                troops=Troops(infantry=2),
                advisors_available=5,
                command_cards=self._deal_initial_cards()
            )
        
        # Create wall sections (one per player plus extras)
        wall_sections = [
            WallSection(section_id=i, fortification_level=1)
            for i in range(len(players) + 2)
        ]
        
        # Assign initial defenders
        for i, (player_id, player) in enumerate(players.items()):
            if i < len(wall_sections):
                wall_sections[i].defender_id = player_id
                player.wall_sections_defended.append(i)
        
        state = GameState(
            game_id=game_id,
            players=players,
            wall_sections=wall_sections,
            mongol_hordes=[],
            current_phase=Phase.SETUP
        )
        
        return state
    
    def _deal_initial_cards(self) -> List[str]:
        """Deal initial command cards"""
        all_cards = [
            "fortify", "rally_troops", "counterattack", "strategic_retreat",
            "reinforcements", "inspired_defense", "tactical_strike"
        ]
        return random.sample(all_cards, 3)
    
    def start_game(self):
        """Start the game"""
        self.state.current_phase = Phase.COMMAND
        self.state.current_player_turn = list(self.state.players.keys())[0]
        print(f"Game {self.state.game_id} started!")
        print(f"Players: {', '.join(p.name for p in self.state.players.values())}")
    
    def execute_command_phase(self):
        """Execute command phase - players draw and play command cards"""
        print(f"\n=== Round {self.state.current_round} - Command Phase ===")
        for player in self.state.players.values():
            # Draw a new card
            new_card = random.choice([
                "fortify", "rally_troops", "counterattack", "strategic_retreat",
                "reinforcements", "inspired_defense", "tactical_strike"
            ])
            player.command_cards.append(new_card)
            print(f"{player.name} draws card: {new_card}")
        
        self.state.next_phase()
    
    def place_worker(self, player_id: str, location: LocationType, advisors: int = 1) -> bool:
        """Place worker at a location"""
        player = self.state.get_player(player_id)
        if not player:
            return False
        
        if player.advisors_available < advisors:
            print(f"Not enough advisors available")
            return False
        
        placement = WorkerPlacement(
            player_id=player_id,
            location=location,
            advisors_used=advisors
        )
        
        self.state.worker_placements.append(placement)
        player.advisors_available -= advisors
        
        print(f"{player.name} places {advisors} advisor(s) at {location.value}")
        return True
    
    def execute_worker_placement_phase(self):
        """Execute worker placement phase"""
        print(f"\n=== Round {self.state.current_round} - Worker Placement Phase ===")
        
        # In a real game, this would be interactive turn-by-turn
        # For this implementation, we'll simulate random placements
        for player in self.state.players.values():
            while player.advisors_available > 0:
                location = random.choice(list(LocationType))
                advisors_to_use = min(random.randint(1, 2), player.advisors_available)
                self.place_worker(player.player_id, location, advisors_to_use)
        
        self.state.next_phase()
    
    def execute_resolution_phase(self):
        """Resolve all worker placements"""
        print(f"\n=== Round {self.state.current_round} - Resolution Phase ===")
        
        for placement in self.state.worker_placements:
            self._resolve_placement(placement)
        
        self.state.next_phase()
    
    def _resolve_placement(self, placement: WorkerPlacement):
        """Resolve a single worker placement"""
        if placement.resolved:
            return
        
        player = self.state.get_player(placement.player_id)
        if not player:
            return
        
        location = placement.location
        power = placement.advisors_used
        
        if location == LocationType.MARKET:
            amount = 2 * power
            player.resources.add(ResourceType.GOLD, amount)
            print(f"  {player.name} gains {amount} gold")
        
        elif location == LocationType.QUARRY:
            amount = 2 * power
            player.resources.add(ResourceType.STONE, amount)
            print(f"  {player.name} gains {amount} stone")
        
        elif location == LocationType.LUMBER_MILL:
            amount = 2 * power
            player.resources.add(ResourceType.WOOD, amount)
            print(f"  {player.name} gains {amount} wood")
        
        elif location == LocationType.CHI_TEMPLE:
            amount = 2 * power
            player.resources.add(ResourceType.CHI, amount)
            print(f"  {player.name} gains {amount} chi")
        
        elif location == LocationType.BARRACKS:
            # Recruit troops
            troop_type = random.choice([TroopType.ARCHER, TroopType.INFANTRY, TroopType.CAVALRY])
            count = power
            player.troops.add(troop_type, count)
            print(f"  {player.name} recruits {count} {troop_type.value}(s)")
        
        elif location == LocationType.COMMAND_CENTER:
            # Draw extra cards
            for _ in range(power):
                card = random.choice(["fortify", "rally_troops", "counterattack"])
                player.command_cards.append(card)
            print(f"  {player.name} draws {power} command card(s)")
        
        elif location == LocationType.WALL_SECTION:
            # Fortify wall
            if player.wall_sections_defended:
                section_id = player.wall_sections_defended[0]
                section = self.state.get_wall_section(section_id)
                if section:
                    section.fortification_level += power
                    print(f"  {player.name} fortifies wall section {section_id} (+{power})")
        
        placement.resolved = True
    
    def spawn_mongol_hordes(self):
        """Spawn Mongol hordes for battle phase"""
        num_hordes = min(self.state.current_round + 1, len(self.state.wall_sections))
        
        for i in range(num_hordes):
            strength = random.randint(3, 8) + self.state.current_round
            target = random.randint(0, len(self.state.wall_sections) - 1)
            
            horde = MongolHorde(
                strength=strength,
                target_section=target
            )
            self.state.mongol_hordes.append(horde)
            print(f"  Mongol horde (strength {strength}) targets section {target}")
    
    def execute_battle_phase(self):
        """Execute battle phase"""
        print(f"\n=== Round {self.state.current_round} - Battle Phase ===")
        
        # Spawn hordes
        self.spawn_mongol_hordes()
        
        # Resolve battles
        for horde in self.state.mongol_hordes:
            if not horde.defeated:
                self._resolve_battle(horde)
        
        self.state.next_phase()
    
    def _resolve_battle(self, horde: MongolHorde):
        """Resolve a single battle"""
        section = self.state.get_wall_section(horde.target_section)
        if not section:
            return
        
        print(f"\n  Battle at section {section.section_id}:")
        
        # Calculate defense strength
        defense_strength = section.fortification_level * 2
        defense_strength += section.defenders.total()
        defense_strength += section.towers * 3
        
        # Add defender player's additional troops
        if section.defender_id:
            defender = self.state.get_player(section.defender_id)
            if defender:
                defense_strength += defender.troops.total()
        
        # Add some randomness
        defense_roll = defense_strength + random.randint(0, 6)
        attack_roll = horde.attack_strength()
        
        print(f"    Defense: {defense_roll} vs Attack: {attack_roll}")
        
        if defense_roll >= attack_roll:
            # Successful defense
            horde.defeated = True
            damage = max(0, attack_roll - defense_strength)
            section.take_damage(damage)
            
            if section.defender_id:
                defender = self.state.get_player(section.defender_id)
                if defender:
                    honor = 3 + (defense_roll - attack_roll) // 2
                    defender.gain_honor(honor)
                    print(f"    {defender.name} successfully defends! (+{honor} honor)")
        else:
            # Failed defense
            damage = attack_roll - defense_roll
            section.take_damage(damage)
            
            # Lose some troops
            if section.defender_id:
                defender = self.state.get_player(section.defender_id)
                if defender and defender.troops.total() > 0:
                    loss = min(damage // 2, defender.troops.infantry)
                    defender.troops.remove(TroopType.INFANTRY, loss)
                    print(f"    {defender.name} loses {loss} troops")
            
            print(f"    Defense failed! Section takes {damage} damage")
            
            if section.is_breached():
                print(f"    ⚠️  Section {section.section_id} is BREACHED!")
    
    def execute_cleanup_phase(self):
        """Execute cleanup phase"""
        print(f"\n=== Round {self.state.current_round} - Cleanup Phase ===")
        
        # Reset advisors
        for player in self.state.players.values():
            player.advisors_available = 5
        
        # Clear worker placements
        self.state.worker_placements.clear()
        
        # Clear defeated hordes
        self.state.mongol_hordes = [h for h in self.state.mongol_hordes if not h.defeated]
        
        # Give maintenance resources
        for player in self.state.players.values():
            player.resources.add(ResourceType.CHI, 1)
        
        print("  Players reset for next round")
        
        self.state.next_phase()
    
    def play_round(self):
        """Play a complete round"""
        if self.state.is_game_over():
            return False
        
        self.execute_command_phase()
        self.execute_worker_placement_phase()
        self.execute_resolution_phase()
        self.execute_battle_phase()
        self.execute_cleanup_phase()
        
        return not self.state.is_game_over()
    
    def play_game(self):
        """Play the complete game"""
        self.start_game()
        
        while not self.state.is_game_over():
            self.play_round()
        
        self.print_final_results()
    
    def print_final_results(self):
        """Print final game results"""
        print("\n" + "="*50)
        print("GAME OVER")
        print("="*50)
        
        print("\nFinal Scores:")
        sorted_players = sorted(
            self.state.players.values(),
            key=lambda p: p.honor_points,
            reverse=True
        )
        
        for i, player in enumerate(sorted_players, 1):
            print(f"{i}. {player.name}: {player.honor_points} honor points")
        
        winner = self.state.get_winner()
        if winner:
            print(f"\n🏆 {winner.name} wins the game! 🏆")
        
        print("\nWall Status:")
        for section in self.state.wall_sections:
            status = "BREACHED" if section.is_breached() else f"Standing (dmg: {section.damage})"
            print(f"  Section {section.section_id}: {status}")
    
    def get_state_summary(self) -> Dict:
        """Get a summary of current game state"""
        return {
            "game_id": self.state.game_id,
            "round": self.state.current_round,
            "phase": self.state.current_phase.value,
            "players": {
                p.player_id: {
                    "name": p.name,
                    "honor": p.honor_points,
                    "resources": {
                        "gold": p.resources.gold,
                        "wood": p.resources.wood,
                        "stone": p.resources.stone,
                        "chi": p.resources.chi
                    },
                    "troops": {
                        "archers": p.troops.archers,
                        "infantry": p.troops.infantry,
                        "cavalry": p.troops.cavalry
                    }
                }
                for p in self.state.players.values()
            },
            "wall_sections": [
                {
                    "id": s.section_id,
                    "defender": s.defender_id,
                    "fortification": s.fortification_level,
                    "damage": s.damage,
                    "breached": s.is_breached()
                }
                for s in self.state.wall_sections
            ]
        }
