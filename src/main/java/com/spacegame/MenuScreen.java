package com.spacegame;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MenuScreen {

    private final VBox root;
    private final Stage stage;

    public MenuScreen(Stage stage) {
        this.stage = stage;

        root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0a0a1a;");

        Label title = new Label("SPACE DEFENDER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setTextFill(Color.CYAN);

        Label subtitle = new Label("Defends la Terre contre les extraterrestres !");
        subtitle.setFont(Font.font("Arial", 16));
        subtitle.setTextFill(Color.LIGHTGRAY);

        Button btnPlay = createButton("JOUER", "#00cc44");
        Button btnQuit = createButton("QUITTER", "#cc2200");

        btnPlay.setOnAction(e -> startGame(1));
        btnQuit.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, subtitle, btnPlay, btnQuit);
    }

    private Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 12 40; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    public void startGame(int level) {
        GameScreen game = new GameScreen(stage, level);
        Scene scene = new Scene(game.getRoot(), MainApp.WIDTH, MainApp.HEIGHT);
        game.setScene(scene);
        stage.setScene(scene);
        game.start();
    }

    public Parent getRoot() {
        return root;
    }
}
