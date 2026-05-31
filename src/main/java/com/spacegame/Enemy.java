package com.spacegame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

public class Enemy {

    private double x, y;
    private double speedX;
    private final double width = 50;
    private final double height = 30;
    private boolean alive = true;
    private static final Random rand = new Random();

    private double bombCooldown;
    private double bombTimer = 0;

    public Enemy(double x, double y, double speedX, double bombCooldown) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.bombCooldown = bombCooldown;
    }

    public void update(double delta) {
        x += speedX * delta;
        if (x <= 0 || x + width >= MainApp.WIDTH) {
            speedX = -speedX;
            y += 20;
        }
        bombTimer += delta;
    }

    public boolean shouldDropBomb() {
        if (bombTimer >= bombCooldown) {
            bombTimer = rand.nextDouble() * bombCooldown * 0.5;
            return true;
        }
        return false;
    }

    public void draw(GraphicsContext gc) {
        // Corps ovale de l'OVNI
        gc.setFill(Color.STEELBLUE);
        gc.fillOval(x, y + 8, width, height - 8);

        // Dôme
        gc.setFill(Color.LIGHTCYAN);
        gc.fillOval(x + 10, y, width - 20, 20);

        // Lumières
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + 8, y + 14, 8, 8);
        gc.fillOval(x + width / 2 - 4, y + 14, 8, 8);
        gc.fillOval(x + width - 16, y + 14, 8, 8);
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
