package com.dozingcatsoftware.vectorpinball.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class ScoreView extends StackPane {

    Label messageLabel;
    Label ballLabel;
    Label multiplierLabel;

    public ScoreView() {
        setMinHeight(40);
        setPrefHeight(40);
        setPrefWidth(200);
        setAlignment(Pos.CENTER);
        setBackground(new Background(new BackgroundFill(Color.rgb(32, 32, 32), null, null)));

        // Label for score and messages.
        messageLabel = new Label();
        messageLabel.setFont(new Font(24));
        messageLabel.setTextFill(Color.YELLOW);
        messageLabel.setAlignment(Pos.CENTER);

        // Label for ball number.
        ballLabel = new Label();
        ballLabel.setFont(new Font(18));
        ballLabel.setTextFill(Color.AQUAMARINE);
        StackPane.setAlignment(ballLabel, Pos.CENTER_LEFT);
        StackPane.setMargin(ballLabel, new Insets(0, 0, 0, 10));

        // Label for multiplier.
        multiplierLabel = new Label();
        multiplierLabel.setFont(new Font(18));
        multiplierLabel.setTextFill(Color.GOLDENROD);
        StackPane.setAlignment(multiplierLabel, Pos.CENTER_LEFT);
        StackPane.setMargin(multiplierLabel, new Insets(0, 0, 0, 80));

        this.getChildren().addAll(messageLabel, ballLabel, multiplierLabel);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setBallNumber(int ballNumber) {
        ballLabel.setText("Ball " + ballNumber);
    }

    public void setMultiplier(double multiplier) {
        if (multiplier <= 1) {
            multiplierLabel.setText("");
        }
        else {
            int intVal = (int) multiplier;
            String text = (multiplier == intVal) ?
                    intVal + "x" : String.format("%.2fx", multiplier);
            multiplierLabel.setText(text);
        }
    }
}
