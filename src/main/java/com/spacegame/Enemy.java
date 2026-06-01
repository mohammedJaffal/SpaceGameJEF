package com.spacegame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Random;

public class Enemy {

    // Kenney Space Shooter Redux preview sheet, CC0.
    // Used only for sprite visuals. If the image cannot load, Canvas vector fallback is used.
    private static final String KENNEY_SHEET_URL =
            "https://kenney.nl/media/pages/assets/space-shooter-redux/770f8d32f7-1677669432/preview.png";
    private static Image kenneySheet;
    private static boolean triedLoadingSheet = false;

    public enum Type {
        SCOUT("Kenney Scout"),
        SHOOTER("Kenney Blaster"),
        TANK("Kenney Tank"),
        REAPER("Kenney Reaper"),
        BOSS("Kenney Mothership");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private double x, y;
    private double speedX;
    private double width;
    private double height;
    private boolean alive = true;
    private static final Random rand = new Random();

    private final Type type;
    private final int level;
    private int health;
    private int maxHealth;
    private int reward;
    private double bombChance;

    private final double baseY;
    private double waveAmplitude;
    private double waveSpeed;
    private double phase;

    private double bombCooldown;
    private double bombTimer = 0;

    public Enemy(double x, double y, double speedX, double bombCooldown) {
        this(x, y, speedX, bombCooldown, Type.SCOUT, 1);
    }

    public Enemy(double x, double y, double speedX, double bombCooldown, Type type, int level) {
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.speedX = speedX;
        this.bombCooldown = bombCooldown;
        this.type = type;
        this.level = level;
        this.phase = rand.nextDouble() * Math.PI * 2;
        configureStats();
    }

    private void configureStats() {
        switch (type) {
            case SCOUT -> {
                width = 54;
                height = 44;
                maxHealth = 1 + level / 5;
                reward = 10 * level;
                bombChance = 0.50;
                waveAmplitude = 13 + rand.nextDouble() * 8;
                waveSpeed = 2.0 + rand.nextDouble() * 1.4;
            }
            case SHOOTER -> {
                width = 58;
                height = 46;
                maxHealth = 2 + level / 4;
                reward = 18 * level;
                bombChance = 0.88;
                waveAmplitude = 7 + rand.nextDouble() * 8;
                waveSpeed = 1.6 + rand.nextDouble() * 1.2;
                bombCooldown *= 0.78;
            }
            case TANK -> {
                width = 70;
                height = 52;
                maxHealth = 4 + level / 2;
                reward = 30 * level;
                bombChance = 0.62;
                waveAmplitude = 4 + rand.nextDouble() * 4;
                waveSpeed = 0.8 + rand.nextDouble() * 0.7;
                speedX *= 0.62;
                bombCooldown *= 1.15;
            }
            case REAPER -> {
                width = 82;
                height = 60;
                maxHealth = 6 + level;
                reward = 48 * level;
                bombChance = 0.80;
                waveAmplitude = 22 + rand.nextDouble() * 10;
                waveSpeed = 2.3 + rand.nextDouble() * 1.0;
                speedX *= 1.15;
                bombCooldown *= 0.72;
            }
            case BOSS -> {
                width = 170;
                height = 90;
                maxHealth = 14 + level * 5;
                reward = 150 * level;
                bombChance = 0.96;
                waveAmplitude = 10;
                waveSpeed = 1.05;
                bombCooldown *= 0.55;
            }
        }
        health = maxHealth;
    }

    public void update(double delta) {
        phase += waveSpeed * delta;

        switch (type) {
            case SCOUT -> {
                x += speedX * delta;
                y += Math.sin(phase) * waveAmplitude * delta;
            }
            case SHOOTER -> {
                x += speedX * delta;
                y += Math.sin(phase) * waveAmplitude * delta * 0.75;
            }
            case TANK -> {
                x += speedX * delta;
                y += Math.sin(phase) * waveAmplitude * delta * 0.45;
            }
            case REAPER -> {
                x += speedX * delta;
                y += Math.sin(phase) * waveAmplitude * delta * 1.15;
            }
            case BOSS -> {
                x += speedX * delta;
                y = baseY + Math.sin(phase) * waveAmplitude;
            }
        }

        if (x <= 10 || x + width >= MainApp.WIDTH - 10) {
            speedX = -speedX;
            x = Math.max(10, Math.min(x, MainApp.WIDTH - width - 10));
            if (type != Type.BOSS) {
                y += type == Type.TANK ? 12 : 22;
            }
        }

        if (type != Type.BOSS && y < baseY - 28) y = baseY - 28;
        bombTimer += delta;
    }

    public boolean shouldDropBomb() {
        if (bombTimer >= bombCooldown) {
            bombTimer = rand.nextDouble() * bombCooldown * 0.45;
            return rand.nextDouble() < bombChance;
        }
        return false;
    }

    public void draw(GraphicsContext gc) {
        if (!drawKenneySprite(gc)) {
            switch (type) {
                case SCOUT -> drawScout(gc);
                case SHOOTER -> drawShooter(gc);
                case TANK -> drawTank(gc);
                case REAPER -> drawReaper(gc);
                case BOSS -> drawBoss(gc);
            }
        }
        drawHealthBar(gc);
    }

    private boolean drawKenneySprite(GraphicsContext gc) {
        Image sheet = getKenneySheet();
        if (sheet == null || sheet.isError() || sheet.getProgress() < 1.0) return false;

        double[] source = switch (type) {
            // Coordinates are taken from the official Kenney Space Shooter Redux preview sheet.
            case SCOUT -> new double[]{70, 415, 55, 50};
            case SHOOTER -> new double[]{70, 365, 55, 48};
            case TANK -> new double[]{70, 470, 55, 50};
            case REAPER -> new double[]{180, 515, 58, 52};
            case BOSS -> new double[]{286, 198, 92, 58};
        };

        double glow = switch (type) {
            case BOSS -> 26;
            case REAPER -> 18;
            default -> 10;
        };
        Color glowColor = switch (type) {
            case SHOOTER -> Color.color(1.0, 0.1, 0.45, 0.18);
            case REAPER -> Color.color(0.8, 0.2, 1.0, 0.22);
            case BOSS -> Color.color(1.0, 0.0, 0.55, 0.20);
            default -> Color.color(0.0, 0.8, 1.0, 0.16);
        };
        gc.setFill(glowColor);
        gc.fillOval(x - glow / 2, y - glow / 3, width + glow, height + glow);
        gc.drawImage(sheet, source[0], source[1], source[2], source[3], x, y, width, height);
        return true;
    }

    private static Image getKenneySheet() {
        if (!triedLoadingSheet) {
            triedLoadingSheet = true;
            try {
                kenneySheet = new Image(KENNEY_SHEET_URL, false);
            } catch (Exception ignored) {
                kenneySheet = null;
            }
        }
        return kenneySheet;
    }

    private void drawScout(GraphicsContext gc) {
        gc.setFill(Color.color(0.0, 0.8, 1.0, 0.18));
        gc.fillOval(x - 6, y + 4, width + 12, height + 8);
        gc.setFill(Color.rgb(34, 87, 122));
        gc.fillOval(x, y + 9, width, height - 7);
        gc.setFill(Color.LIGHTCYAN);
        gc.fillOval(x + 9, y, width - 18, 20);
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(2);
        gc.strokeOval(x + 2, y + 10, width - 4, height - 8);
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + 8, y + 17, 7, 7);
        gc.fillOval(x + width / 2 - 3.5, y + 18, 7, 7);
        gc.fillOval(x + width - 15, y + 17, 7, 7);
    }

    private void drawShooter(GraphicsContext gc) {
        gc.setFill(Color.color(1.0, 0.18, 0.18, 0.22));
        gc.fillOval(x - 8, y + 1, width + 16, height + 12);
        gc.setFill(Color.rgb(85, 26, 64));
        gc.fillOval(x, y + 10, width, height - 8);
        gc.setFill(Color.HOTPINK);
        gc.fillOval(x + 12, y, width - 24, 22);
        gc.setFill(Color.ORANGERED);
        gc.fillRoundRect(x + width / 2 - 5, y + height - 2, 10, 18, 5, 5);
        gc.setStroke(Color.HOTPINK);
        gc.setLineWidth(2);
        gc.strokeOval(x + 2, y + 10, width - 4, height - 8);
    }

    private void drawTank(GraphicsContext gc) {
        gc.setFill(Color.color(0.55, 0.25, 1.0, 0.22));
        gc.fillRoundRect(x - 7, y + 2, width + 14, height + 10, 18, 18);
        gc.setFill(Color.rgb(56, 42, 98));
        gc.fillRoundRect(x, y + 8, width, height - 4, 14, 14);
        gc.setFill(Color.MEDIUMPURPLE);
        gc.fillRoundRect(x + 8, y, width - 16, 22, 12, 12);
        gc.setFill(Color.LIGHTBLUE);
        gc.fillOval(x + 11, y + 19, 8, 8);
        gc.fillOval(x + width / 2 - 4, y + 20, 8, 8);
        gc.fillOval(x + width - 19, y + 19, 8, 8);
        gc.setStroke(Color.MEDIUMPURPLE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 2, y + 9, width - 4, height - 6, 14, 14);
    }

    private void drawReaper(GraphicsContext gc) {
        gc.setFill(Color.color(0.65, 0.0, 1.0, 0.23));
        gc.fillOval(x - 10, y - 2, width + 20, height + 16);

        gc.setFill(Color.rgb(42, 28, 76));
        double[] bodyX = {x + width / 2, x + 4, x + width * 0.25, x + width * 0.75, x + width - 4};
        double[] bodyY = {y, y + height * 0.55, y + height, y + height, y + height * 0.55};
        gc.fillPolygon(bodyX, bodyY, 5);

        gc.setFill(Color.GHOSTWHITE);
        gc.fillOval(x + width / 2 - 15, y + 15, 30, 24);
        gc.setFill(Color.HOTPINK);
        gc.fillOval(x + width / 2 - 7, y + 24, 5, 5);
        gc.fillOval(x + width / 2 + 2, y + 24, 5, 5);

        gc.setStroke(Color.MEDIUMPURPLE);
        gc.setLineWidth(3);
        gc.strokePolygon(bodyX, bodyY, 5);
    }

    private void drawBoss(GraphicsContext gc) {
        gc.setFill(Color.color(1.0, 0.0, 0.55, 0.20));
        gc.fillOval(x - 22, y - 10, width + 44, height + 30);
        gc.setFill(Color.rgb(36, 20, 64));
        gc.fillRoundRect(x + 18, y + 18, width - 36, height - 12, 24, 24);
        gc.setFill(Color.rgb(80, 20, 100));
        gc.fillOval(x, y + 10, 58, 48);
        gc.fillOval(x + width - 58, y + 10, 58, 48);
        gc.setFill(Color.HOTPINK);
        gc.fillOval(x + width / 2 - 32, y, 64, 38);
        gc.setFill(Color.ORANGERED);
        gc.fillRoundRect(x + 25, y + height - 7, 14, 24, 8, 8);
        gc.fillRoundRect(x + width - 39, y + height - 7, 14, 24, 8, 8);
        gc.setStroke(Color.HOTPINK);
        gc.setLineWidth(3);
        gc.strokeRoundRect(x + 16, y + 17, width - 32, height - 10, 24, 24);
        gc.setStroke(Color.CYAN);
        gc.strokeOval(x + width / 2 - 33, y, 66, 39);
    }

    private void drawHealthBar(GraphicsContext gc) {
        double barWidth = width;
        double barHeight = type == Type.BOSS ? 7 : 5;
        double barX = x;
        double barY = y - (type == Type.BOSS ? 18 : 10);
        double percent = Math.max(0, health / (double) maxHealth);
        gc.setFill(Color.color(0, 0, 0, 0.58));
        gc.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        gc.setFill(percent > 0.45 ? Color.LIME : Color.ORANGERED);
        gc.fillRoundRect(barX, barY, barWidth * percent, barHeight, 4, 4);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.7);
        gc.strokeRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        if (type == Type.BOSS || type == Type.REAPER || maxHealth > 2) {
            gc.setFont(Font.font("Arial", FontWeight.BOLD, type == Type.BOSS ? 12 : 9));
            gc.setFill(Color.WHITE);
            gc.fillText(health + "/" + maxHealth, barX + barWidth / 2 - 14, barY - 2);
        }
    }

    public boolean intersects(double ox, double oy, double ow, double oh) {
        return x < ox + ow && x + width > ox && y < oy + oh && y + height > oy;
    }

    public boolean takeDamage(int amount) {
        if (!alive) return false;
        health = Math.max(0, health - amount);
        if (health <= 0) {
            kill();
            return true;
        }
        return false;
    }

    public void kill() { alive = false; }
    public boolean isAlive() { return alive; }
    public Type getType() { return type; }
    public String getDisplayName() { return type.getDisplayName(); }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getReward() { return reward; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getCenterX() { return x + width / 2; }
    public double getBottomY() { return y + height; }
}
