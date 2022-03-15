import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.swing.*;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

public class Main extends Application {

    String whiteBackground = "-fx-background-color:#ffffff";
    String bold = "-fx-font-weight:bold";

    double screenX = Screen.getPrimary().getBounds().getWidth();
    double screenY = Screen.getPrimary().getBounds().getHeight();

    int size = 0;

    Controller gameController;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.UNDECORATED);

        Label title = new Label(" KenKen: \nMathDoku");
        title.setAlignment(Pos.CENTER);
        title.setStyle(bold);

        Label fontSelectorLabel = new Label("Select cage/cell font size");
        fontSelectorLabel.setStyle(bold);

        ComboBox<String> fontSelector = new ComboBox<String>();
        fontSelector.setStyle(whiteBackground+";"+bold);
        fontSelector.getItems().addAll("Small","Medium","Large");
        fontSelector.setValue("Medium");

        Button newGameButton = new Button("New Game");
        Button loadFromFileButton = new Button("Load from file");
        loadFromFileButton.setOnAction(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println(selectedFile.getAbsolutePath());
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(selectedFile));
                    int size = 0;
                    HashSet<String> cellSet = new HashSet<String>();
                    HashMap<String, String> cageSettings = new HashMap<String, String>();
                    while (bufferedReader.ready()) {
                        String line[] = bufferedReader.readLine().split(" ");
                        String cells[] = line[1].split(",");
                        for (String s : cells) {
                            size++;
                            if (cellSet.contains(s)) {
                                throw new DuplicateCellException();
                            }
                            cellSet.add(s);
                        }
                        cageSettings.put(line[1], line[0]);
                    }
                    if (Math.sqrt(size) % 1 == 0) {
                        size = (int) Math.sqrt(size);
                    } else {
                        throw new MissingCellException();
                    }
                    int font = 0;
                    switch (fontSelector.getValue()){
                        case "Small":font=0;break;
                        case "Medium":font=1;break;
                        case "Large":font=2;break;
                        default:font=1;
                    }
                    Controller controller = new Controller(size, primaryStage, cageSettings,font);
                    controller.game();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DuplicateCellException | NoAdjacecentCellException | MissingCellException e) {
                    String warning;
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Mathdoku");
                    if (e instanceof DuplicateCellException) warning = "There are duplicate Cells in the loaded file";
                    else if (e instanceof MissingCellException) {
                        warning = "There is a missing Cell";
                    } else warning = "There is a cell in a cage with no adjcaen cell";
                    alert.setContentText(warning);
                    alert.show();
                }
            }
        });

        newGameButton.setStyle(whiteBackground + ";" + bold);
        loadFromFileButton.setStyle(whiteBackground + ";" + bold);



        Button quitButton = new Button("Quit");
        quitButton.setStyle(whiteBackground + ";" + bold);
        quitButton.setOnAction(actionEvent -> {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Mathdoku");
            alert.setContentText("Are you sure you want to exit");
            if (alert.showAndWait().get().equals(ButtonType.OK)) {
                Platform.exit();
            }
        });

        Button backButton = new Button("Back");
        backButton.setStyle(whiteBackground + ";" + bold);


        VBox mainVbox = new VBox(title, newGameButton,fontSelectorLabel,fontSelector, loadFromFileButton, quitButton);
        mainVbox.setSpacing(20);
        mainVbox.setStyle(whiteBackground);
        mainVbox.setAlignment(Pos.CENTER);
        Scene mainScene = new Scene(mainVbox, screenX / 10, screenY / 3, Color.WHITE);

        backButton.setOnAction(actionEvent -> {
            primaryStage.setScene(mainScene);
            primaryStage.show();
        });

        newGameButton.setOnAction(actionEvent -> {
            int font = 0;
            switch (fontSelector.getValue()){
                case "Small":font=0;break;
                case "Medium":font=1;break;
                case "Large":font=2;break;
                default:font=1;
            }
            gameController = new Controller(primaryStage,font);
            gameController.setCages();
        });

        primaryStage.setX(screenX / 2 - screenX / 20);
        primaryStage.setY(screenY / 3);
        primaryStage.setResizable(true);
        primaryStage.setTitle("Mathdoku");
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    static class DuplicateCellException extends Exception {
        DuplicateCellException() {
            super();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class MissingCellException extends Exception {
    MissingCellException() {
        super();
    }
}
