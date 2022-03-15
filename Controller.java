import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.*;

public class Controller {

    private Scene returnScene;
    private Stack<Change> undoStack = new Stack<Change>();
    private Stack<Change> redoStack = new Stack<Change>();


    private double screenWidth = Screen.getPrimary().getBounds().getWidth();
    private double screenHeight = Screen.getPrimary().getBounds().getHeight();

    private Cell selectedCell;
    private Cell cells[][] = new Cell[8][8];
    private GridPane gameGrid;
    private int numberOfCages;
    private int size;
    private int fontSize;
    private HashMap<String, String> cageSettings = new HashMap<String, String>();
    private CheckBox mistakeDetection = new CheckBox(" - MistakeDetection/WinDetection");

    private double oldX;
    private double oldY;

    private final Stage stage;

    private String whiteBackground = "-fx-background-color:#ffffff";
    private String yellowBackground = "-fx-background-color:#ffff00";
    private String darkRedBackground = "-fx-background-color:#880000";
    private String redBackground = "-fx-background-color:#ff0000";
    private String bold = "-fx-font-weight:bold";
    private String font = "-fx-font-size: ";

    private String borderColor = "-fx-border-color: #000000";

    Controller(Stage stage,int font) {
        returnScene = stage.getScene();
        oldX = stage.getX();
        oldY = stage.getY();
        this.stage = stage;
        this.fontSize = font;
    }

    Controller(int size, Stage stage, HashMap<String, String> cageSettings,int font) {
        this.cageSettings = cageSettings;
        returnScene = stage.getScene();
        oldX = stage.getX();
        oldY = stage.getY();
        this.stage = stage;
        this.size = size;
        this.stage.setX(screenWidth / 4);
        this.stage.setY(screenHeight / 4);
        this.fontSize = font;
    }

    void setCages() {

        Button backButton = new Button("Back");
        backButton.setStyle(whiteBackground + ";" + bold);

        Button nextButton = new Button("Next");
        nextButton.setStyle(whiteBackground + ";" + bold);


        VBox vBox = new VBox();
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(10));
        hBox.setSpacing(17 * screenWidth / 40);
        hBox.setAlignment(Pos.TOP_CENTER);
        hBox.getChildren().addAll(backButton, nextButton);
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setStyle(whiteBackground);
        vBox.getChildren().addAll(hBox);
        GridPane cageSettings = new GridPane();

        Label numberCagesLabel = new Label("Number of Cages : ");
        TextField numberCagesField = new TextField();
        cageSettings.setHgap(10);
        cageSettings.setVgap(10);
        cageSettings.add(numberCagesLabel, 1, 0);
        cageSettings.add(numberCagesField, 2, 0);
        cageSettings.setAlignment(Pos.CENTER);

        numberCagesField.setOnAction(actionEvent -> {
            numberOfCages = Integer.parseInt(numberCagesField.getText());
            for (int i = 1; i <= numberOfCages; i++) {

                Label cageDescLabel = new Label("Cage Description(eg. 11+)");
                TextField cageDescTextField = new TextField();
                Label cellNumberLabel = new Label("Cells coordinates(eg. x,y;x1,y1)");
                TextField cellNumberField = new TextField();

                cageSettings.add(cageDescLabel, 0, i);
                cageSettings.add(cageDescTextField, 1, i);
                cageSettings.add(cellNumberLabel, 2, i);
                cageSettings.add(cellNumberField, 3, i);
            }
        });

        vBox.getChildren().addAll(cageSettings);

        Scene gameScene = new Scene(vBox, screenWidth / 2, screenHeight / 2, Color.WHITE);
        stage.setX(screenWidth / 4);
        stage.setY(screenHeight / 4);

        backButton.setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Mathdoku");
            alert.setContentText("This will erase all your progress so far");
            if (alert.showAndWait().get().equals(ButtonType.OK)) {
                stage.setX(oldX);
                stage.setY(oldY);
                stage.setScene(returnScene);
                stage.show();
            }

        });
        nextButton.setOnAction(actionEvent -> {
            try {
                int size = 0;
                HashSet<String> cells = new HashSet<String>();
                for (int i = 1; i <= numberOfCages; i++) {
                    TextField cageLabel = (TextField) getFromGridPane(cageSettings, 1, i);
                    TextField cageCells = (TextField) getFromGridPane(cageSettings, 3, i);
                    String cagecells[] = cageCells.getText().split(",");
                    for (String s : cagecells) {
                        if (cells.contains(s)) throw new Main.DuplicateCellException();
                        cells.add(s);
                        size++;
                    }
                    this.cageSettings.put(cageCells.getText(), cageLabel.getText());
                }
                if (Math.sqrt(size) % 1 == 0) {
                    this.size = (int) Math.sqrt(size);
                } else {
                    throw new MissingCellException();
                }

                game();
            } catch (Main.DuplicateCellException | NoAdjacecentCellException | MissingCellException e) {
                String warning;
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Mathdoku");
                if (e instanceof Main.DuplicateCellException) warning = "There are duplicate Cells in the loaded file";
                else if (e instanceof MissingCellException) {
                    warning = "There is a missing Cell";
                } else warning = "There is a cell in a cage with no adjcaen cell";
                alert.setContentText(warning);
                alert.show();
            }
        });

        stage.setScene(gameScene);
        stage.show();
    }

    void game() throws NoAdjacecentCellException {

        Button backButton = new Button("Back");
        backButton.setStyle(whiteBackground + ";" + bold);
        backButton.setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Mathdoku");
            alert.setContentText("This will erase all your progress so far");
            if (alert.showAndWait().get().equals(ButtonType.OK)) {
                stage.setX(oldX);
                stage.setY(oldY);
                stage.setScene(returnScene);
                stage.show();
            } else stage.show();
        });

        Button undoButton = new Button("Undo");
        undoButton.setStyle(whiteBackground + ";" + bold);
        Button redoButton = new Button("Redo");
        redoButton.setStyle(whiteBackground + ";" + bold);
        Button clearButton = new Button("Clear");
        clearButton.setStyle(whiteBackground + ";" + bold);

        undoButton.setDisable(true);
        redoButton.setDisable(true);

        undoButton.setOnAction(actionEvent -> {
            Change change = undoStack.pop();
            Label cellNumber = change.cell.getNumber();
            cellNumber.setText(change.prevValue);
            selectedCell = change.cell;
            mistakeDetection();
            selectedCell = null;
            redoStack.push(change);
            redoButton.setDisable(false);
            if (undoStack.isEmpty()) undoButton.setDisable(true);
        });

        redoButton.setOnAction(actionEvent -> {

            Change change = redoStack.pop();
            Label cellNumber = change.cell.getNumber();
            cellNumber.setText(change.newValue);
            selectedCell = change.cell;
            mistakeDetection();
            selectedCell = null;
            undoStack.push(change);
            undoButton.setDisable(false);
            if (redoStack.isEmpty()) redoButton.setDisable(true);
        });

        clearButton.setOnAction(actionEvent -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Mathdoku");
            alert.setContentText("This will erase all your progress so far");
            if (alert.showAndWait().get().equals(ButtonType.OK)) {
                if (selectedCell != null) {
                    selectedCell.stackPane.setStyle(whiteBackground + ";" + selectedCell.border + borderColor);
                }
                selectedCell = null;
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        cells[i][j].getNumber().setText("");
                    }
                }
                while (!undoStack.empty()) {
                    undoStack.pop();
                }
                while (!redoStack.empty()) {
                    redoStack.pop();
                }
            }
        });

        BorderPane pane = new BorderPane();
        pane.setStyle(whiteBackground);

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(10));
        topBar.setSpacing(screenWidth/12);
        topBar.getChildren().addAll(backButton, mistakeDetection);
        mistakeDetection.setStyle(bold);
        /*mistakeDetection.setOnAction(actionEvent -> {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    selectedCell = cells[i][j];
                    if (mistakeDetection()) selectedCell.filled = true;
                }
            }
            selectedCell = null;
        });*/
        pane.setTop(topBar);

        VBox numberSelect = new VBox();
        numberSelect.setAlignment(Pos.CENTER_LEFT);
        numberSelect.setPadding(new Insets(20));
        numberSelect.setSpacing(10);
        numberSelect.setAlignment(Pos.CENTER);
        pane.setRight(numberSelect);

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle(whiteBackground + ";" + bold);
        deleteButton.setOnAction(actionEvent -> {
            if (selectedCell != null) {
                undoStack.push(new Change(selectedCell, selectedCell.getNumber().getText(), ""));
                mistakeDetection();
                redoButton.setDisable(true);
                undoButton.setDisable(false);
                selectedCell.getNumber().setText("");
                selectedCell = null;
            }

        });


        GridPane buttonPane = new GridPane();
        buttonPane.add(undoButton, 0, 0);
        buttonPane.add(redoButton, 0, 1);
        buttonPane.add(clearButton, 1, 0);
        buttonPane.add(deleteButton, 1, 1);

        Label numberLabel = new Label("Select which number to fill with:");
        numberLabel.setStyle(bold);
        numberSelect.getChildren().addAll(buttonPane, numberLabel);


        for (int i = 1; i <= size; i++) {
            Button numberButton = new Button(i + "");
            numberButton.setStyle(/*whiteBackground + ";"+*/ bold);
            numberButton.setOnAction(actionEvent -> {
                if (selectedCell != null) {
                    Button source = (Button) actionEvent.getSource();
                    undoStack.push(new Change(selectedCell, selectedCell.getNumber().getText(), source.getText()));
                    for (int t = 1; t <= redoStack.size(); t++) {
                        redoStack.pop();
                    }
                    redoButton.setDisable(true);
                    undoButton.setDisable(false);
                    selectedCell.getNumber().setText(source.getText());

                    if (!mistakeDetection()) {
                        selectedCell.filled = true;
                    }
                    selectedCell.stackPane.setStyle(whiteBackground + ";" + selectedCell.border + borderColor);
                    selectedCell = null;
                }

            });
            numberSelect.getChildren().add(numberButton);
        }
        gameGrid = new GridPane();
        pane.setCenter(gameGrid);
        gameGrid.setAlignment(Pos.CENTER);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                StackPane stackPane = new StackPane();
                StackPane stackPane2 = new StackPane();
                stackPane2.setAlignment(Pos.TOP_LEFT);

                Rectangle rectangle = new Rectangle(screenWidth / 40, screenWidth / 40, Color.TRANSPARENT);
                Label cellLabel = new Label();
                Label cageLabel = new Label();

                cellLabel.setStyle(bold + ";" + font + (25+fontSize*5));
                cageLabel.setStyle(font + (10+fontSize*3));


                stackPane2.setOnKeyPressed(keyEvent -> {

                    StackPane source = (StackPane) keyEvent.getSource();
                    if (source.getStyle().contains(yellowBackground)) {
                        String key = keyEvent.getText();
                        if (keyEvent.getCode() == KeyCode.DELETE || keyEvent.getCode() == KeyCode.BACK_SPACE) {
                            undoStack.push(new Change(selectedCell, selectedCell.getNumber().getText(), ""));
                            selectedCell.getNumber().setText("");
                            selectedCell.stackPane.setStyle(whiteBackground + ";" + selectedCell.border + borderColor);
                            while (!redoStack.empty()) {
                                redoStack.pop();
                            }
                            undoButton.setDisable(false);
                            redoButton.setDisable(true);
                            selectedCell = null;
                            return;
                        } else if (keyEvent.getCode().isDigitKey()) {
                            if (key.charAt(0) > '0' && key.charAt(0) <= (size + "").charAt(0)) {
                                undoStack.push(new Change(selectedCell, selectedCell.getNumber().getText(), key));
                                selectedCell.getNumber().setText(key);
                                selectedCell.stackPane.setStyle(whiteBackground + ";" + selectedCell.border + borderColor);

                                if (!mistakeDetection()) {
                                    selectedCell.filled = true;
                                }

                                while (!redoStack.empty()) {
                                    redoStack.pop();
                                }
                                undoButton.setDisable(false);
                                redoButton.setDisable(true);
                                selectedCell = null;
                            }
                        }
                    }

                });

                stackPane2.setOnMouseClicked(mouseEvent -> {
                    StackPane source = (StackPane) mouseEvent.getSource();
                    if (source.getStyle().contains(yellowBackground)) {
                        selectedCell.stackPane.setStyle(whiteBackground + ";" + selectedCell.border + borderColor);
                        return;
                    }
                    if (selectedCell != null) {
                        selectedCell.stackPane.setStyle(whiteBackground + ";" + selectedCell.border + borderColor);
                    }
                    for (int k = 0; k < size; k++) {
                        for (int h = 0; h < size; h++) {
                            if (cells[k][h].stackPane.equals(source)) {
                                selectedCell = cells[k][h];
                                selectedCell.stackPane.setStyle(yellowBackground + ";" + selectedCell.border + borderColor);
                                selectedCell.stackPane.requestFocus();
                                break;
                            }
                        }
                    }
                });

                stackPane.getChildren().addAll(rectangle, cellLabel);
                stackPane2.getChildren().addAll(stackPane, cageLabel);

                cells[i][j] = new Cell(stackPane2, j, i);

                gameGrid.add(stackPane2, j, i);
            }
        }
        cageBuilding();


        Scene gameScene = new Scene(pane, 3 * screenWidth / 5, 3 * screenHeight / 5, Color.WHITE);
        stage.setX(screenWidth / 5);
        stage.setY(screenHeight / 5);
        stage.setScene(gameScene);

        stage.setX(screenWidth / 5);
        stage.setY(screenHeight / 5);
        stage.show();

    }

    private Node getFromGridPane(GridPane gridPane, int col, int row) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }
        return null;
    }

    class Cage {
        int sum;
        char sign;
        ArrayList<Cell> cageCells;

        Cage(String s) {
            if (s.length() > 1) {
                sum = Integer.parseInt(s.substring(0, s.length() - 1));

                sign = s.charAt(s.length() - 1);
            } else {
                sign = 'e';
                sum = Integer.parseInt(s);
            }
            cageCells = new ArrayList<Cell>();
        }

        Boolean isRight() {
            for (Cell c : cageCells) {
                if (c.getNumber().getText().equals("")) return true;
            }
            if (sign == 'e') {
                if (cageCells.get(0).getNumber().getText().equals(sum + "")) {
                    return true;
                }
            }
            if (sign == '+') {
                int sum = 0;
                for (Cell cell : cageCells) {
                    if (cell.getNumber().getText().equals("")) {
                        return true;
                    }
                    sum = sum + Integer.parseInt(cell.getNumber().getText());
                }
                if (sum == this.sum) return true;
            }
            if (sign == '-') {
                int sum = 0;
                int biggestNumber = 0;
                for (Cell cell : cageCells) {
                    if (cell.getNumber().getText().equals("")) {
                        return true;
                    }
                    int thisCellNumber = Integer.parseInt(cell.getNumber().getText());
                    if (thisCellNumber > biggestNumber) biggestNumber = thisCellNumber;
                    sum = sum + thisCellNumber;
                }
                if (biggestNumber * 2 - sum == this.sum) return true;
            }
            if (sign == '/' || sign == '÷') {
                int sum = 1;
                int biggestNumber = 0;
                for (Cell cell : cageCells) {
                    if (cell.getNumber().getText().equals("")) {
                        return true;
                    }
                    int thisCellNumber = Integer.parseInt(cell.getNumber().getText());
                    if (thisCellNumber > biggestNumber) biggestNumber = thisCellNumber;
                    sum = sum * thisCellNumber;
                }
                if ((biggestNumber * biggestNumber) / sum == this.sum) return true;
            }
            if (sign == 'x') {
                int sum = 1;
                for (Cell cell : cageCells) {
                    if (cell.getNumber().getText().equals("")) {
                        return true;
                    }
                    int thisCellNumber = Integer.parseInt(cell.getNumber().getText());
                    sum = sum * thisCellNumber;
                }
                if (sum == this.sum) return true;
            }
            return false;
        }
    }

    class Change {
        Cell cell;
        String prevValue;
        String newValue;

        Change(Cell c, String pV, String nV) {
            cell = c;
            prevValue = pV;
            newValue = nV;
        }
    }

    void cageBuilding() throws NoAdjacecentCellException {
        for (String s : cageSettings.keySet()) {
            ArrayList<String> cageCells = new ArrayList(Arrays.asList(s.split(",")));
            int min[] = {size, size};
            Cage cage = new Cage(cageSettings.get(s));
            for (String s1 : cageCells) {
                boolean adjacent = false;
                int cell = Integer.parseInt(s1);
                int cellX = cell;
                int cellY = 0;
                while (cellX > size) {
                    cellX = cellX - size;
                    cellY++;
                }
                cellX = cellX - 1;
                Cell c = this.cells[cellY][cellX];
                c.setCage(cage);
                c.cage.cageCells.add(c);
                if (cellX == 0) {
                    c.border = c.border.substring(0, 24) + "3" + c.border.substring(25);
                } else if (!cageCells.contains((cell - 1) + "")) {
                    c.border = c.border.substring(0, 24) + "3" + c.border.substring(25);
                } else {
                    adjacent = true;
                }
                if (cell <= size) {
                    c.border = c.border.substring(0, 18) + "3" + c.border.substring(19);
                } else if (!cageCells.contains((cell - size) + "")) {
                    c.border = c.border.substring(0, 18) + "3" + c.border.substring(19);
                } else {
                    adjacent = true;
                }
                if (cellX == size - 1) {
                    c.border = c.border.substring(0, 20) + "3" + c.border.substring(21);
                } else if (!cageCells.contains((cell + 1) + "")) {
                    c.border = c.border.substring(0, 20) + "3" + c.border.substring(21);
                } else {
                    adjacent = true;
                }
                if (cellY == size - 1) {
                    c.border = c.border.substring(0, 22) + "3" + c.border.substring(23);
                } else if (!cageCells.contains((cell + size) + "")) {
                    c.border = c.border.substring(0, 22) + "3" + c.border.substring(23);
                } else {
                    adjacent = true;
                }
                c.stackPane.setStyle(whiteBackground + ";" + c.border + borderColor);
                if (cellY < min[1]) {
                    min[1] = cellY;
                    min[0] = cellX;
                }
                if (cellY == min[1] && cellX <= min[0]) {
                    min[0] = cellX;
                }
                if (!adjacent && cageSettings.get(s).length() > 1) {
                    throw new NoAdjacecentCellException();
                }
            }
            Label cageLabel = (Label) this.cells[min[1]][min[0]].stackPane.getChildren().get(1);
            cageLabel.setText(cageSettings.get(s));
        }
    }

    class Cell {
        boolean filled = false;
        Cage cage;
        StackPane stackPane;
        int x;
        int y;
        Cell mistakeContributor;
        String border = "-fx-border-width: 1 1 1 1 ;";

        Cell(StackPane stackPane, int x, int y) {
            this.stackPane = stackPane;
            this.x = x;
            this.y = y;
        }

        void setCage(Cage cage) {
            this.cage = cage;
        }

        Label getNumber() {
            StackPane st = (StackPane) this.stackPane.getChildren().get(0);
            return (Label) st.getChildren().get(1);
        }

    }

    boolean mistakeDetection() {
        if (mistakeDetection.isSelected()) {
            boolean lineRight = false;
            boolean cageRight = false;
            for (int k = 0; k < size; k++) {
                if (cells[selectedCell.y][k].getNumber().getText().equals(selectedCell.getNumber().getText()) && selectedCell != cells[selectedCell.y][k]) {
                    selectedCell.stackPane.setStyle(darkRedBackground + ";" + selectedCell.border + borderColor);
                    for (int j = 0; j < size; j++) {
                        cells[selectedCell.y][j].stackPane.setStyle(redBackground + ";" + cells[selectedCell.y][j].border + borderColor);
                        cells[selectedCell.y][j].mistakeContributor = selectedCell;
                    }
                    selectedCell.mistakeContributor = cells[selectedCell.y][k];
                    lineRight = true;
                }
                if (cells[k][selectedCell.x].getNumber().getText().equals(selectedCell.getNumber().getText()) && selectedCell != cells[k][selectedCell.x]) {
                    selectedCell.stackPane.setStyle(darkRedBackground + ";" + selectedCell.border + borderColor);
                    for (int j = 0; j < size; j++) {
                        cells[j][selectedCell.x].stackPane.setStyle(redBackground + ";" + cells[j][selectedCell.x].border + borderColor);
                        cells[j][selectedCell.x].mistakeContributor = selectedCell;
                    }
                    selectedCell.mistakeContributor = cells[k][selectedCell.x];
                    lineRight = true;
                }
            }
            if (!selectedCell.cage.isRight()) {
                System.out.println(selectedCell.cage.sum + "" + selectedCell.cage.sign);
                for (Cell c : selectedCell.cage.cageCells) {
                    c.stackPane.setStyle(redBackground + ";" + c.border + borderColor);
                    c.mistakeContributor = selectedCell;
                }
                selectedCell.mistakeContributor = selectedCell;
                selectedCell.stackPane.setStyle(darkRedBackground + ";" + selectedCell.border + borderColor);
                cageRight = true;
            }
            boolean win = true;

            if (!(lineRight || cageRight)) {
                System.out.println(selectedCell.cage.sum + "" + selectedCell.cage.sign);
                for (int k = 0; k < size; k++) {
                    if (cells[selectedCell.y][k].mistakeContributor == selectedCell) {
                    cells[selectedCell.y][k].stackPane.setStyle(whiteBackground + ";" + cells[selectedCell.y][k].border + borderColor);
                    cells[selectedCell.y][k].mistakeContributor = null;
                    }
                    if (cells[k][selectedCell.y].mistakeContributor == selectedCell) {
                    cells[k][selectedCell.x].stackPane.setStyle(whiteBackground + ";" + cells[k][selectedCell.x].border + borderColor);
                    cells[k][selectedCell.x].mistakeContributor = null;
                    }
                }
                for (Cell cell : selectedCell.cage.cageCells) {
                    if (cell.mistakeContributor == selectedCell) {
                    cell.stackPane.setStyle(whiteBackground + ";" + cell.border + borderColor);
                    cell.mistakeContributor = null;
                    }
                }
                selectedCell.stackPane.setStyle(whiteBackground + ";" + selectedCell.border + borderColor);
            }


            if (!(lineRight || cageRight)) {
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        if (cells[i][j].getNumber().getText()==""||cells[i][j].stackPane.getStyle().contains(redBackground)||cells[i][j].stackPane.getStyle().contains(darkRedBackground)) {
                            win = false;
                            break;
                        }
                    }
                }
                if (win) win();
            }
            return lineRight || cageRight;
        } else return false;
    }

    void win() {
        StackPane stackPane = new StackPane();
        Canvas canvas = new Canvas(3*screenWidth/5,3*screenHeight/5);
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        for(int i =0 ;i <150;i++){
            Random r = new Random();
            float red = r.nextFloat();
            float green = r.nextFloat();
            float blue = r.nextFloat();
            double x = r.nextDouble()*(screenWidth*3/5+150);
            double y = r.nextDouble()*(screenWidth*3/5+150);
            graphicsContext.setFill(Color.color(red,green,blue));
            graphicsContext.fillOval(x-150,y-150,300,300);
        }
        Label congratulationLabel = new Label("Congratulations!");
        stackPane.setOnMouseClicked(mouseEvent -> {
            stage.setX(oldX);
            stage.setY(oldY);
            stage.setScene(returnScene);
            stage.show();
        });
        congratulationLabel.setStyle(bold+";"+font+100);
        stackPane.getChildren().addAll(stage.getScene().getRoot(),canvas,congratulationLabel);
        stage.setScene(new Scene(stackPane));
        stage.setX(screenWidth/5);
        stage.setY(screenHeight/5);
        stage.show();

    }


}

class NoAdjacecentCellException extends Exception {
    NoAdjacecentCellException() {
        super();
    }
}
