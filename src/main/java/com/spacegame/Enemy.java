package com.spacegame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

public class Enemy {

    private double x, y;
    private double speedX;
    private final double width = 52;
    private final double height = 34;
    private boolean alive = true;
    private static final Random rand = new Random();

    private final double baseY;
    private final double waveAmplitude;
    private final double waveSpeed;
    private double phase;

    private final double bombCooldown;
    private double bombTimer = 0;

    public Enemy(double x, double y, double speedX, double bombCooldown) {
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.speedX = speedX;
        this.bombCooldown = bombCooldown;
        this.waveAmplitude = 8 + rand.nextDouble() * 12;
        this.waveSpeed = 1.2 + rand.nextDouble() * 1.8;
        this.phase = rand.nextDouble() * Math.PI * 2;
    }

    public void update(double delta) {
        x += speedX * delta;
        phase += waveSpeed * delta;
        y += Math.sin(phase) * waveAmplitude * delta;

        if (x <= 10 || x + width >= MainApp.WIDTH - 10) {
            speedX = -speedX;
            y += 24;
        }
        if (y < baseY - 25) y = baseY - 25;
        bombTimer += delta;
    }

    public boolean shouldDropBomb() {
        if (bombTimer >= bombCooldown) {
            bombTimer = rand.nextDouble() * bombCooldown * 0.45;
            return rand.nextDouble() < 0.78;
        }
        return false;
    }

    public void draw(GraphicsContext gc) {
        // Halo neon
        gc.setFill(Color.color(0.0, 0.8, 1.0, 0.18));
        gc.fillOval(x - 6, y + 3, width + 12, height + 10);

        // Corps ovale de l'OVNI
        gc.setFill(Color.rgb(34, 87, 122));
        gc.fillOval(x, y + 10, width, height - 8);

        // Dome
        gc.setFill(Color.LIGHTCYAN);
        gc.fillOval(x + 11, y, width - 22, 22);

        // Ligne neon
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(2);
        gc.strokeOval(x + 2, y + 11, width - 4, height - 10);

        // Lumieres
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + 8, y + 18, 8, 8);
        gc.fillOval(x + width / 2 - 4, y + 19, 8, 8);
        gc.fillOval(x + width - 16, y + 18, 8, 8);
    }

    public boolean intersects(double ox, double oy, double ow, double oh) {
        return x < ox + ow && x + width > ox && y < oy + oh && y + height > oy;
    }

    public void kill() { alive = false; }
    public boolean isAlive() { return alive; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getCenterX() { return x + width / 2; }
    public double getBottomY() { return y + height; }
}
