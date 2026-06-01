# JEF updates

This update turns the project into a more modern JavaFX space shooter:

- Energy bar instead of simple hearts.
- Power-ups: Shield, Double Shot, Repair.
- Random falling power-ups and bonus drops from enemies.
- Enemies move with a more dynamic wave motion.
- Bullets can destroy bombs for small bonus points.
- Pause with `P`.
- More neon-style menu and HUD.

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
