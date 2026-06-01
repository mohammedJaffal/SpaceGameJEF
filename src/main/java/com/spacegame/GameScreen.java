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

        initStars();
        initPlayer();
        initEnemies();
    }

    private void initStars() {
        for (int i = 0; i < 120; i++) stars.add(new Star());
    }

    private void initPlayer() {
        player = new Player(MainApp.WIDTH / 2.0 - 25, MainApp.HEIGHT - 90);
    }

    private void initEnemies() {
        int cols = 7;
        int rows = Math.min(5, 2 + level);
        double enemySpeed = 70 + level * 28;
        double bombCooldown = Math.max(0.85, 2.8 - level * 0.35);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double ex = 75 + c * 108;
                double ey = 58 + r * 62;
                double direction = ((r + c) % 2 == 0) ? 1 : -1;
                enemies.add(new Enemy(ex, ey, enemySpeed * direction, bombCooldown));
            }
        }
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

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
            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                double delta = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                if (delta > 0.05) delta = 0.05;
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

        updatePlayerMovement(delta);
        updateShooting(delta);
        updateEnemiesAndBombs(delta);
        updatePowerUps(delta);
        checkCollisions();
        updateExplosions(delta);
    }

    private void updatePlayerMovement(double delta) {
        double playerSpeed = 265;
        if (leftPressed) player.moveLeft(playerSpeed * delta);
        if (rightPressed) player.moveRight(playerSpeed * delta);
    }

    private void updateShooting(double delta) {
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

    private void updateEnemiesAndBombs(double delta) {
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            e.update(delta);
            if (gameTimer > 2.0 && e.shouldDropBomb()) {
                double bombSpeed = 125 + level * 38;
                bombs.add(new Bomb(e.getCenterX(), e.getBottomY(), bombSpeed));
            }
            if (e.getY() > MainApp.HEIGHT) {
                e.kill();
            }
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

    private void spawnPowerUp() {
        PowerUp.Type[] types = PowerUp.Type.values();
        PowerUp.Type type = types[random.nextInt(types.length)];
        double x = 45 + random.nextDouble() * (MainApp.WIDTH - 90);
        double speed = 70 + level * 10;
        powerUps.add(new PowerUp(x, -30, type, speed));
    }

    private void checkCollisions() {
        if (gameTimer <= 1.2) return;

        // Bullet vs Enemy collisions
        for (Bullet bullet : bullets) {
            if (!bullet.isActive()) continue;
            for (Enemy enemy : enemies) {
                if (!enemy.isAlive()) continue;
                if (bullet.intersects(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                    bullet.deactivate();
                    enemy.kill();
                    explosions.add(new Explosion(enemy.getX() + enemy.getWidth() / 2,
                            enemy.getY() + enemy.getHeight() / 2, 38));
                    score += 10 * level;
                    if (random.nextDouble() < 0.12) spawnPowerUpNear(enemy.getCenterX(), enemy.getBottomY());
                    break;
                }
            }
        }

        // Bullet vs Bomb collisions: bonus defense mechanic
        for (Bullet bullet : bullets) {
            if (!bullet.isActive()) continue;
            for (Bomb bomb : bombs) {
                if (!bomb.isActive()) continue;
                if (bullet.intersects(bomb.getX(), bomb.getY(), bomb.getWidth(), bomb.getHeight())) {
                    bullet.deactivate();
                    bomb.deactivate();
                    explosions.add(new Explosion(bomb.getX(), bomb.getY(), 18));
                    score += 2 * level;
                    break;
                }
            }
        }

        // Bomb vs Player collisions
        for (Bomb bomb : bombs) {
            if (!bomb.isActive()) continue;
            if (bomb.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                bomb.deactivate();
                boolean damaged = player.takeDamage(20);
                explosions.add(new Explosion(player.getCenterX(), player.getY() + 30, damaged ? 28 : 18));
                if (player.isDead()) gameOver = true;
            }
        }

        // PowerUp vs Player collisions
        for (PowerUp powerUp : powerUps) {
            if (!powerUp.isActive()) continue;
            if (powerUp.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                applyPowerUp(powerUp.getType());
                powerUp.deactivate();
                explosions.add(new Explosion(player.getCenterX(), player.getY() + 25, 22));
            }
        }

        // Victory check
        if (enemies.stream().noneMatch(Enemy::isAlive)) {
            victory = true;
        }
    }

    private void spawnPowerUpNear(double x, double y) {
        PowerUp.Type[] types = PowerUp.Type.values();
        PowerUp.Type type = types[random.nextInt(types.length)];
        powerUps.add(new PowerUp(Math.max(20, Math.min(MainApp.WIDTH - 50, x)), y, type, 85));
    }

    private void applyPowerUp(PowerUp.Type type) {
        switch (type) {
            case SHIELD -> player.activateShield(6.0);
            case DOUBLE_SHOT -> player.activateDoubleShot(7.0);
            case REPAIR -> player.heal(30);
        }
        score += 15;
    }

    private void updateExplosions(double delta) {
        for (Explosion ex : explosions) ex.update(delta);
        explosions.removeIf(Explosion::isFinished);
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
        gc.setLineWidth(1.2);
        for (int i = 0; i < 8; i++) {
            gc.strokeLine(0, MainApp.HEIGHT - 25 - i * 20, MainApp.WIDTH, MainApp.HEIGHT - 65 - i * 11);
        }
    }

    private void drawHUD() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.setFill(Color.WHITE);
        gc.fillText("Score : " + score, 15, 28);
        gc.fillText("Niveau : " + level, MainApp.WIDTH / 2.0 - 40, 28);
        gc.fillText("P : Pause", MainApp.WIDTH - 100, MainApp.HEIGHT - 18);

        drawEnergyBar();
        drawPowerTimers();
    }

    private void drawEnergyBar() {
        double x = MainApp.WIDTH - 250;
        double y = 15;
        double w = 160;
        double h = 16;

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
        if (player.hasShield()) {
            gc.setFill(Color.CYAN);
            gc.fillText("Shield " + formatTime(player.getShieldTime()) + "s", 15, y);
            y += 20;
        }
        if (player.hasDoubleShot()) {
            gc.setFill(Color.LIME);
            gc.fillText("Double shot " + formatTime(player.getDoubleShotTime()) + "s", 15, y);
        }
    }

    private String formatTime(double value) {
        return String.format("%.1f", value);
    }

    private void drawOverlay(String title, Color titleColor, String subtitle) {
        gc.setFill(Color.color(0, 0, 0, 0.68));
        gc.fillRect(0, 0, MainApp.WIDTH, MainApp.HEIGHT);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 56));
        gc.setFill(titleColor);
        double tw = title.length() * 30.0;
        gc.fillText(title, MainApp.WIDTH / 2.0 - tw / 2, MainApp.HEIGHT / 2.0 - 40);

        gc.setFont(Font.font("Arial", 22));
        gc.setFill(Color.WHITE);
        double sw = subtitle.length() * 11.0;
        gc.fillText(subtitle, MainApp.WIDTH / 2.0 - sw / 2, MainApp.HEIGHT / 2.0 + 10);

        if (paused && !gameOver && !victory) return;

        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.LIGHTGRAY);
        String hint = victory
                ? "[ ENTREE ] Niveau suivant  |  [ ECHAP ] Menu"
                : "[ ENTREE ] Rejouer  |  [ ECHAP ] Menu";
        gc.fillText(hint, MainApp.WIDTH / 2.0 - hint.length() * 4.2, MainApp.HEIGHT / 2.0 + 55);

        if (!overlayListenerSet) {
            overlayListenerSet = true;
            scene.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER -> {
                        timer.stop();
                        if (victory) {
                            new MenuScreen(stage).startGame(level + 1);
                        } else {
                            new MenuScreen(stage).startGame(level);
                        }
                    }
                    case ESCAPE -> {
                        timer.stop();
                        MenuScreen menu = new MenuScreen(stage);
                        stage.setScene(new Scene(menu.getRoot(), MainApp.WIDTH, MainApp.HEIGHT));
                    }
                }
            });
        }
    }

    public Parent getRoot() { return root; }
}
