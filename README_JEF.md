# JEF updates

This update turns the project into a more modern JavaFX space shooter.

## What changed

- Energy bar instead of simple hearts.
- Power-ups: Shield, Double Shot, Repair.
- Random falling power-ups and bonus drops from enemies.
- Enemies move with a more dynamic wave motion.
- Bullets can destroy bombs for small bonus points.
- Pause with `P`.
- More neon-style menu and HUD.
- Multiple monster classes: Scout, Shooter, Tank, and Boss/Mothership.
- Health bars above monsters and a `MONSTER STATUS` panel showing the current monster type and HP.

## Open-source inspiration checked

- `kailielexx/Space-Shooter-Game` was reviewed. Its README describes dynamic enemies, power-ups, score, and difficulty-level features, and its MIT license allows reuse with attribution. The available `game.js` currently only implements a simple ship movement and bullet loop, so there were no ready monster classes or monster sprites to copy directly.
- Kenney `Space Shooter Redux` was also used as visual inspiration for arcade-style enemy variety. Kenney lists that asset pack under Creative Commons CC0.

The current enemy art in this project is drawn directly with JavaFX Canvas code, so the project stays simple: no external PNG/SVG assets are required to compile or run.

## Run

```bash
mvn clean compile
mvn javafx:run
```

## Controls

- `Q` / `D` or arrow keys: move the ship
- `Space`: shoot
- `P`: pause / resume
- `Enter`: replay or next level
- `Escape`: return to menu
