Commands:
- WASD: Movement
- Mouse Move: Rotate player
- Left Click:
    - Melee Weapon: Light attack
    - Ranged Weapon: Fire
- Right Click:
    - Melee Weapon: Heavy attack
    - Ranged Weapon: Null
- 1: Swap to melee weapon
- 2: Swap to ranged weapon (if you have one)
- Space Bar: Dash (must be moving)
- Left Shift: Blocking
- E: pickup Item/open door when prompted
- T: toggle lock cursor to screen

Cheat Codes:
- god: Gives the player invincibility.
- musket: Gives the player a musket as their ranged weapon. +10 ammo
- crossbow: Gives the player a crossbow as their ranged weapon. +10 ammo
- fps: Shows fps.
- killall: Kills all enemies in the dungeon. Triggers boss fight.
- stats: Shows basic render information. How many sprites, walls, etc.
- debug: Shows rays in minimap.

Low Bar Goals:
- Raycasted 2.5D rendering in first person (completed)
- “Doors” that can be opened/closed by players(completed)

- Player
    - Can hold 1 melee weapon that they choose at the start (completed)
    - 1 ranged weapon if found and can freely swap between (completed)
    - Limited health:
        - Health regens to “checkpoints” example: every 25% it can regenerate to (completed)
    - Stamina for dodging/blocking/heavy attacks (completed)
    - Dashing gives small amount of immunity at the cost of stamina (completed)

- Melee Attacks
    - Light attack (completed)
    - Heavy attack - slow harder hitting, uses stamina (completed)
    - Block - blocks damage at the cost of stamina when hit (completed)

- Melee weapons (start with one of these):
    - Sword (completed)
    - Halberd (completed)
    - Mace (completed)
    - Brass Knuckles (not completed)

- Ranged weapons (pickup only, limited ammo):
    - Crossbow (completed)
    - Musket (completed)

- Enemy AI
    - Path finding (completed)
    - Behavior based on type of enemy (completed)
    - Aggression:
        - Enemies can spot you and attack and also trigger aggression on nearby enemies (completed)
        - Enemies can lose sight of you and lose aggression (completed)

- Enemy Types
    - Goblin (melee, machete) (completed)
    - Skeleton (ranged, bow) (Changed to Ranged Goblin: completed)

- Powerups
    - Items dropped when enemy dies, player picks up (completed)

    Powerup Types:
        - Ranged weapons (completed)
        - Health packs (completed)
        - Weapon Buff (completed)

- Multiplayer/Networking
    - Realtime CO-OP gameplay (completed)
    - Players respawn in set amount of time if at least 1 player is remaining (completed)
    - Level resets if all players die within the respawn time (completed)

- Custom Art/Animations
    - Assets built in Blender then rendered as animated sprites (completed)
    - Weapons (completed)
    - Items/Power ups (completed)
    - Enemies (completed)
    - Remote Player (multiplayer viewed) (completed)

Extra Features Added:
- Enemies:
    - Slime (Melee)
    - Werewolf (Melee)

- Torches

- Boss Fight:
    - 3D model with A LOT more detailed animations
    - Extra map

- Minimap

--------LICENSE--------
Apache 2.0

full license under LICENSE.txt