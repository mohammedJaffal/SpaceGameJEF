package com.spacegame;

import javafx.animation.AnimationTimer;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GameScreen {
    private final Stage stage;
    private final int level;
    private Scene scene;
    private final Pane root;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private Player player;
    private Enemy selectedEnemy;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Bomb> bombs = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final Random random = new Random();
    private int score = 0;
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean paused = false;
    private boolean overlayListenerSet = false;
    private double gameTimer = 0;
    private double powerUpTimer = 5.5;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean spacePressed = false;
    private double shootCooldown = 0;
    private AnimationTimer timer;
    private long lastTime = 0;

    public GameScreen(Stage stage, int level) {
        this.stage = stage;
        this.level = level;
        canvas = new Canvas(MainApp.WIDTH, MainApp.HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root = new Pane(canvas);
        for (int i = 0; i < 120; i++) stars.add(new Star());
        player = new Player(MainApp.WIDTH / 2.0 - 25, MainApp.HEIGHT - 90);
        initEnemies();
    }

    private void initEnemies() {
        int cols = 7;
        int rows = Math.min(4, 2 + level);
        double speed = 70 + level * 28;
        double cooldown = Math.max(0.85, 2.8 - level * 0.35);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Enemy.Type type = chooseType(r, c);
                double direction = ((r + c) % 2 == 0) ? 1 : -1;
                enemies.add(new Enemy(75 + c * 108, 62 + r * 68, speed * direction, cooldown, type, level));
            }
        }
        if (level >= 2) {
            enemies.add(new Enemy(MainApp.WIDTH / 2.0 - 75, 44, 48 + level * 12,
                    Math.max(0.9, 2.4 - level * 0.22), Enemy.Type.BOSS, level));
        }
        if (level >= 1) {
            enemies.add(new Enemy(MainApp.WIDTH / 2.0 + 190, 130, -(95 + level * 18),
                    Math.max(0.9, 2.3 - level * 0.20), Enemy.Type.REAPER, level));
        }
    }

    private Enemy.Type chooseType(int row, int col) {
        if (level >= 2 && row == 0 && col == 3) return Enemy.Type.REAPER;
        if (level >= 3 && row == 0 && col % 3 == 1) return Enemy.Type.SHOOTER;
        if (level >= 2 && row >= 2 && col % 2 == 0) return Enemy.Type.TANK;
        if (row == 1 && col % 3 == 0) return Enemy.Type.SHOOTER;
        return Enemy.Type.SCOUT;
    }

    public void setScene(Scene scene) { this.scene = scene; }

    public void start() {
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.Q) leftPressed = true;
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) rightPressed = true;
            if (e.getCode() == KeyCode.SPACE) spacePressed = true;
            if (e.getCode() == KeyCode.P) paused = !paused;
        });
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.Q) leftPressed = false;
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) rightPressed = false;
            if (e.getCode() == KeyCode.SPACE) spacePressed = false;
        });
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                double delta = Math.min(0.05, (now - lastTime) / 1_000_000_000.0);
                lastTime = now;
                update(delta);
                render();
            }
        };
        timer.start();
    }

    private void update(double delta) {
        for (Star s : stars) s.update(delta);
        if (paused || gameOver || victory) return;
        gameTimer += delta;
        player.update(delta);
        if (leftPressed) player.moveLeft(265 * delta);
        if (rightPressed) player.moveRight(265 * delta);
        updateShots(delta);
        updateEnemies(delta);
        updatePowerUps(delta);
        checkCollisions();
        for (Explosion ex : explosions) ex.update(delta);
        explosions.removeIf(Explosion::isFinished);
        refreshSelectedEnemy();
    }

    private void updateShots(double delta) {
        shootCooldown -= delta;
        if (spacePressed && shootCooldown <= 0) {
            if (player.hasDoubleShot()) {
                bullets.add(new Bullet(player.getCenterX() - 12, player.getY() + 4));
                bullets.add(new Bullet(player.getCenterX() + 12, player.getY() + 4));
                shootCooldown = 0.18;
            } else {
                bullets.add(new Bullet(player.getCenterX(), player.getY()));
                shootCooldown = 0.28;
            }
        }
        for (Bullet b : bullets) b.update(delta);
        bullets.removeIf(b -> !b.isActive());
    }

    private void updateEnemies(double delta) {
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            e.update(delta);
            if (gameTimer > 2.0 && e.shouldDropBomb()) {
                double speed = switch (e.getType()) {
                    case BOSS -> 170 + level * 42;
                    case REAPER -> 155 + level * 45;
                    default -> 125 + level * 38;
                };
                bombs.add(new Bomb(e.getCenterX(), e.getBottomY(), speed));
            }
            if (e.getY() > MainApp.HEIGHT) e.kill();
        }
        for (Bomb b : bombs) b.update(delta);
        bombs.removeIf(b -> !b.isActive());
    }

    private void updatePowerUps(double delta) {
        powerUpTimer -= delta;
        if (powerUpTimer <= 0) {
            spawnPowerUp();
            powerUpTimer = Math.max(4.5, 8.5 - level * 0.35) + random.nextDouble() * 2.5;
        }
        for (PowerUp p : powerUps) p.update(delta);
        powerUps.removeIf(p -> !p.isActive());
    }

    private void checkCollisions() {
        if (gameTimer <= 1.2) return;
        for (Bullet bullet : bullets) {
            if (!bullet.isActive()) continue;
            for (Enemy enemy : enemies) {
                if (!enemy.isAlive()) continue;
                if (bullet.intersects(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                    bullet.deactivate();
                    selectedEnemy = enemy;
                    boolean defeated = enemy.takeDamage(player.hasDoubleShot() ? 2 : 1);
                    explosions.add(new Explosion(enemy.getX() + enemy.getWidth() / 2, enemy.getY() + enemy.getHeight() / 2, defeated ? 38 : 18));
                    if (defeated) {
                        score += enemy.getReward();
                        if (random.nextDouble() < dropChance(enemy)) spawnPowerUpNear(enemy.getCenterX(), enemy.getBottomY());
                    } else {
                        score += level;
                    }
                    break;
                }
            }
        }
        for (Bullet bullet : bullets) {
            if (!bullet.isActive()) continue;
            for (Bomb bomb : bombs) {
                if (!bomb.isActive()) continue;
                if (bullet.intersects(bomb.getX(), bomb.getY(), bomb.getWidth(), bomb.getHeight())) {
                    bullet.deactivate(); bomb.deactivate(); score += 2 * level;
                    explosions.add(new Explosion(bomb.getX(), bomb.getY(), 18));
                    break;
                }
            }
        }
        for (Bomb bomb : bombs) {
            if (bomb.isActive() && bomb.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                bomb.deactivate();
                boolean damaged = player.takeDamage(20);
                explosions.add(new Explosion(player.getCenterX(), player.getY() + 30, damaged ? 28 : 18));
                if (player.isDead()) gameOver = true;
            }
        }
        for (PowerUp powerUp : powerUps) {
            if (powerUp.isActive() && powerUp.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                applyPowerUp(powerUp.getType());
                powerUp.deactivate();
                explosions.add(new Explosion(player.getCenterX(), player.getY() + 25, 22));
            }
        }
        if (enemies.stream().noneMatch(Enemy::isAlive)) victory = true;
    }

    private double dropChance(Enemy enemy) {
        return switch (enemy.getType()) {
            case SCOUT -> 0.10;
            case SHOOTER -> 0.18;
            case TANK -> 0.28;
            case REAPER -> 0.45;
            case BOSS -> 1.0;
        };
    }

    private void spawnPowerUp() {
        PowerUp.Type[] types = PowerUp.Type.values();
        powerUps.add(new PowerUp(45 + random.nextDouble() * (MainApp.WIDTH - 90), -30, types[random.nextInt(types.length)], 70 + level * 10));
    }

    private void spawnPowerUpNear(double x, double y) {
        PowerUp.Type[] types = PowerUp.Type.values();
        powerUps.add(new PowerUp(Math.max(20, Math.min(MainApp.WIDTH - 50, x)), y, types[random.nextInt(types.length)], 85));
    }

    private void applyPowerUp(PowerUp.Type type) {
        switch (type) {
            case SHIELD -> player.activateShield(6.0);
            case DOUBLE_SHOT -> player.activateDoubleShot(7.0);
            case REPAIR -> player.heal(30);
        }
        score += 15;
    }

    private void refreshSelectedEnemy() {
        if (selectedEnemy != null && selectedEnemy.isAlive()) return;
        selectedEnemy = enemies.stream().filter(Enemy::isAlive).max(Comparator.comparingInt(Enemy::getMaxHealth)).orElse(null);
    }

    private void render() {
        drawBackground();
        for (Star s : stars) s.draw(gc);
        if (!gameOver && !victory) {
            for (PowerUp p : powerUps) if (p.isActive()) p.draw(gc);
            for (Enemy e : enemies) if (e.isAlive()) e.draw(gc);
            for (Bullet b : bullets) if (b.isActive()) b.draw(gc);
            for (Bomb b : bombs) if (b.isActive()) b.draw(gc);
            player.draw(gc);
        }
        for (Explosion ex : explosions) ex.draw(gc);
        drawHUD();
        if (paused && !gameOver && !victory) drawOverlay("PAUSE", Color.CYAN, "Appuie sur P pour continuer");
        if (gameOver) drawOverlay("GAME OVER", Color.RED, "Score final : " + score);
        if (victory) drawOverlay("VICTOIRE !", Color.LIME, "Score : " + score + " | Niveau " + level + " termine !");
    }

    private void drawBackground() {
        gc.setFill(Color.color(0.015, 0.015, 0.055));
        gc.fillRect(0, 0, MainApp.WIDTH, MainApp.HEIGHT);
        gc.setFill(Color.color(0.0, 0.55, 0.9, 0.10));
        gc.fillOval(-140, 90, 360, 260);
        gc.setFill(Color.color(0.75, 0.0, 0.9, 0.09));
        gc.fillOval(MainApp.WIDTH - 260, 130, 420, 320);
        gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.14));
        for (int i = 0; i < 8; i++) gc.strokeLine(0, MainApp.HEIGHT - 25 - i * 20, MainApp.WIDTH, MainApp.HEIGHT - 65 - i * 11);
    }

    private void drawHUD() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.setFill(Color.WHITE);
        gc.fillText("Score : " + score, 15, 28);
        gc.fillText("Niveau : " + level, MainApp.WIDTH / 2.0 - 40, 28);
        gc.fillText("P : Pause", MainApp.WIDTH - 100, MainApp.HEIGHT - 18);
        drawEnergyBar();
        drawPowerTimers();
        drawMonsterPanel();
    }

    private void drawEnergyBar() {
        double x = MainApp.WIDTH - 250, y = 15, w = 160, h = 16;
        gc.setFill(Color.color(1, 1, 1, 0.12));
        gc.fillRoundRect(x, y, w, h, 10, 10);
        gc.setFill(player.getEnergy() > 35 ? Color.LIME : Color.ORANGERED);
        gc.fillRoundRect(x, y, w * player.getEnergyPercent(), h, 10, 10);
        gc.setStroke(Color.WHITE);
        gc.strokeRoundRect(x, y, w, h, 10, 10);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setFill(Color.WHITE);
        gc.fillText("ENERGIE " + player.getEnergy() + "%", x + 34, y + 12);
    }

    private void drawPowerTimers() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        double y = 52;
        if (player.hasShield()) { gc.setFill(Color.CYAN); gc.fillText("Shield " + formatTime(player.getShieldTime()) + "s", 15, y); y += 20; }
        if (player.hasDoubleShot()) { gc.setFill(Color.LIME); gc.fillText("Double shot " + formatTime(player.getDoubleShotTime()) + "s", 15, y); }
    }

    private void drawMonsterPanel() {
        Enemy e = selectedEnemy != null && selectedEnemy.isAlive() ? selectedEnemy : enemies.stream().filter(Enemy::isAlive).max(Comparator.comparingInt(Enemy::getMaxHealth)).orElse(null);
        double x = MainApp.WIDTH - 270, y = 42, w = 255, h = 78;
        gc.setFill(Color.color(0.02, 0.02, 0.08, 0.70));
        gc.fillRoundRect(x, y, w, h, 14, 14);
        gc.setStroke(Color.color(0.0, 0.9, 1.0, 0.45));
        gc.strokeRoundRect(x, y, w, h, 14, 14);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        gc.setFill(Color.CYAN);
        gc.fillText("MONSTER STATUS", x + 12, y + 20);
        if (e == null) { gc.setFill(Color.WHITE); gc.fillText("Aucun monstre restant", x + 12, y + 46); return; }
        gc.setFill(Color.WHITE);
        gc.fillText(e.getDisplayName(), x + 12, y + 41);
        double hpX = x + 98, hpY = y + 32, hpW = 140, hpH = 12, pct = e.getHealth() / (double) e.getMaxHealth();
        gc.setFill(Color.color(1, 1, 1, 0.12));
        gc.fillRoundRect(hpX, hpY, hpW, hpH, 8, 8);
        gc.setFill(pct > 0.45 ? Color.LIME : Color.ORANGERED);
        gc.fillRoundRect(hpX, hpY, hpW * pct, hpH, 8, 8);
        gc.setStroke(Color.WHITE);
        gc.strokeRoundRect(hpX, hpY, hpW, hpH, 8, 8);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setFill(Color.WHITE);
        gc.fillText("HP " + e.getHealth() + "/" + e.getMaxHealth(), hpX + 38, hpY + 10);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("Scout | Blaster | Tank | Reaper | Boss", x + 12, y + 65);
    }

    private String formatTime(double value) { return String.format("%.1f", value); }

    private void drawOverlay(String title, Color titleColor, String subtitle) {
        gc.setFill(Color.color(0, 0, 0, 0.68));
        gc.fillRect(0, 0, MainApp.WIDTH, MainApp.HEIGHT);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 56));
        gc.setFill(titleColor);
        gc.fillText(title, MainApp.WIDTH / 2.0 - title.length() * 15.0, MainApp.HEIGHT / 2.0 - 40);
        gc.setFont(Font.font("Arial", 22));
        gc.setFill(Color.WHITE);
        gc.fillText(subtitle, MainApp.WIDTH / 2.0 - subtitle.length() * 5.5, MainApp.HEIGHT / 2.0 + 10);
        if (paused && !gameOver && !victory) return;
        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.LIGHTGRAY);
        String hint = victory ? "[ ENTREE ] Niveau suivant  |  [ ECHAP ] Menu" : "[ ENTREE ] Rejouer  |  [ ECHAP ] Menu";
        gc.fillText(hint, MainApp.WIDTH / 2.0 - hint.length() * 4.2, MainApp.HEIGHT / 2.0 + 55);
        if (!overlayListenerSet) {
            overlayListenerSet = true;
            scene.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER -> { timer.stop(); if (victory) new MenuScreen(stage).startGame(level + 1); else new MenuScreen(stage).startGame(level); }
                    case ESCAPE -> { timer.stop(); MenuScreen menu = new MenuScreen(stage); stage.setScene(new Scene(menu.getRoot(), MainApp.WIDTH, MainApp.HEIGHT)); }
                }
            });
        }
    }

    public Parent getRoot() { return root; }
}
