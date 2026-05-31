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

import java.util.*;

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

    private int score = 0;
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean overlayListenerSet = false;
    private double gameTimer = 0;

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
        for (int i = 0; i < 80; i++) stars.add(new Star());
    }

    private void initPlayer() {
        player = new Player(MainApp.WIDTH / 2.0 - 25, MainApp.HEIGHT - 90);
    }

    private void initEnemies() {
        int cols = 8;
        int rows = 2 + level;
        double enemySpeed = 60 + level * 30;
        double bombCooldown = Math.max(1.0, 3.0 - level * 0.5);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double ex = 60 + c * 95;
                double ey = 60 + r * 60;
                enemies.add(new Enemy(ex, ey, enemySpeed * (((r + c) % 2 == 0) ? 1 : -1), bombCooldown));
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
        if (gameOver || victory) return;

        gameTimer += delta;

        // Stars
        for (Star s : stars) s.update(delta);

        // Player movement
        double playerSpeed = 250;
        if (leftPressed) player.moveLeft(playerSpeed * delta);
        if (rightPressed) player.moveRight(playerSpeed * delta);

        // Shooting
        shootCooldown -= delta;
        if (spacePressed && shootCooldown <= 0) {
            bullets.add(new Bullet(player.getCenterX(), player.getY()));
            shootCooldown = 0.3;
        }

        // Update bullets
        for (Bullet b : bullets) b.update(delta);
        bullets.removeIf(b -> !b.isActive());

        // Update enemies and bombs
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            e.update(delta);
            if (gameTimer > 3.0 && e.shouldDropBomb()) {
                double bombSpeed = 120 + level * 40;
                bombs.add(new Bomb(e.getCenterX(), e.getBottomY(), bombSpeed));
            }
            // Si ennemi sort de l'ecran par le bas, on le tue sans game over
            if (e.getY() > MainApp.HEIGHT) {
                e.kill();
            }
        }

        for (Bomb b : bombs) b.update(delta);
        bombs.removeIf(b -> !b.isActive());

        if (gameTimer > 3.0) {
            // Bullet vs Enemy collisions
            for (Bullet bullet : bullets) {
                if (!bullet.isActive()) continue;
                for (Enemy enemy : enemies) {
                    if (!enemy.isAlive()) continue;
                    if (bullet.intersects(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                        bullet.deactivate();
                        enemy.kill();
                        explosions.add(new Explosion(enemy.getX() + enemy.getWidth() / 2,
                                enemy.getY() + enemy.getHeight() / 2, 35));
                        score += 10 * level;
                    }
                }
            }

            // Bomb vs Player collisions
            for (Bomb bomb : bombs) {
                if (!bomb.isActive()) continue;
                if (bomb.intersects(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                    bomb.deactivate();
                    player.loseLife();
                    explosions.add(new Explosion(player.getCenterX(), player.getY() + 30, 25));
                    if (player.isDead()) gameOver = true;
                }
            }

            // Victory check
            if (enemies.stream().noneMatch(Enemy::isAlive)) {
                victory = true;
            }
        }

        // Explosions
        for (Explosion ex : explosions) ex.update(delta);
        explosions.removeIf(Explosion::isFinished);
    }

    private void render() {
        // Background
        gc.setFill(Color.color(0.04, 0.04, 0.12));
        gc.fillRect(0, 0, MainApp.WIDTH, MainApp.HEIGHT);

        // Stars
        for (Star s : stars) s.draw(gc);

        if (!gameOver && !victory) {
            // Game elements
            for (Enemy e : enemies) if (e.isAlive()) e.draw(gc);
            for (Bullet b : bullets) if (b.isActive()) b.draw(gc);
            for (Bomb b : bombs) if (b.isActive()) b.draw(gc);
            player.draw(gc);
        }

        // Explosions
        for (Explosion ex : explosions) ex.draw(gc);

        // HUD
        drawHUD();

        if (gameOver) drawOverlay("GAME OVER", Color.RED, "Score final : " + score);
        if (victory) drawOverlay("VICTOIRE !", Color.LIME, "Score : " + score + " | Niveau " + level + " termine !");
    }

    private void drawHUD() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.setFill(Color.WHITE);
        gc.fillText("Score : " + score, 15, 28);
        gc.fillText("Niveau : " + level, MainApp.WIDTH / 2.0 - 40, 28);

        // Lives (hearts)
        gc.setFill(Color.RED);
        for (int i = 0; i < player.getLives(); i++) {
            gc.fillText("<3", MainApp.WIDTH - 38 - i * 32, 28);
        }
    }

    private void drawOverlay(String title, Color titleColor, String subtitle) {
        gc.setFill(Color.color(0, 0, 0, 0.65));
        gc.fillRect(0, 0, MainApp.WIDTH, MainApp.HEIGHT);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 56));
        gc.setFill(titleColor);
        double tw = title.length() * 30.0;
        gc.fillText(title, MainApp.WIDTH / 2.0 - tw / 2, MainApp.HEIGHT / 2.0 - 40);

        gc.setFont(Font.font("Arial", 22));
        gc.setFill(Color.WHITE);
        double sw = subtitle.length() * 11.0;
        gc.fillText(subtitle, MainApp.WIDTH / 2.0 - sw / 2, MainApp.HEIGHT / 2.0 + 10);

        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.LIGHTGRAY);
        String hint = victory
                ? "[ ENTREE ] Niveau suivant  |  [ ECHAP ] Quitter"
                : "[ ENTREE ] Rejouer  |  [ ECHAP ] Quitter";
        gc.fillText(hint, MainApp.WIDTH / 2.0 - hint.length() * 4.2, MainApp.HEIGHT / 2.0 + 55);

        // Key listeners for restart/quit (set only once)
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
