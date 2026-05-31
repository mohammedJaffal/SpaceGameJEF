package com.spacegame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Player {

    private double x, y;
    private final double width = 50;
    private final double height = 60;
    private int lives;
    private static final int MAX_LIVES = 3;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.lives = MAX_LIVES;
    }

    public void draw(GraphicsContext gc) {
        // Corps de la fusée
        gc.setFill(Color.WHITE);
        double[] xPoints = {x + width / 2, x, x + width};
        double[] yPoints = {y, y + height, y + height};
        gc.fillPolygon(xPoints, yPoints, 3);

        // Hublot
        gc.setFill(Color.CYAN);
        gc.fillOval(x + width / 2 - 8, y + height / 2 - 5, 16, 16);

        // Flamme moteur
        gc.setFill(Color.ORANGE);
        gc.fillOval(x + width / 2 - 10, y + height - 5, 20, 15);
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + width / 2 - 6, y + height - 2, 12, 10);
    }

    public void moveLeft(double speed) {
        x = Math.max(0, x - speed);
    }

    public void moveRight(double speed) {
        x = Math.min(MainApp.WIDTH - width, x + speed);
    }

    public boolean intersects(double ox, double oy, double ow, double oh) {
        return x < ox + ow && x + width > ox && y < oy + oh && y + height > oy;
    }

    public void loseLife() {
        lives--;
    }

    public boolean isDead() {
        return lives <= 0;
    }

    public int getLives() { return lives; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getCenterX() { return x + width / 2; }
}
