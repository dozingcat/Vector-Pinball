package com.dozingcatsoftware.vectorpinball.editor;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

import java.util.function.Consumer;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableBumperElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableDropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFlipperElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableRolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableSensorElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableSpinnerElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallArcElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallPathElement;

public class ElementPaletteView extends VBox {

    static String IMAGE_PATH_PREFIX = "/images/";

    Consumer<Class<? extends EditableFieldElement>> createCallback;

    public ElementPaletteView(Consumer<Class<? extends EditableFieldElement>> createCallback) {
        super();
        this.createCallback = createCallback;

        VBox vbox = new VBox(5);

        Label addElementLabel = new Label(localizedString("Create element"));
        addElementLabel.setFont(new Font(16));
        vbox.getChildren().add(addElementLabel);

        HBox elementRow1 = new HBox(5);
        Button bumperButton = createNewElementButton("bumper.png", "Bumper", EditableBumperElement.class);
        Button wallButton = createNewElementButton("wall.png", "Wall", EditableWallElement.class);
        Button wallPathButton = createNewElementButton("wallpath.png", "Path", EditableWallPathElement.class);
        Button wallArcButton = createNewElementButton("wallarc.png", "Arc", EditableWallArcElement.class);
        Button rolloverButton = createNewElementButton(
                "rollovers.png", "Rollovers", EditableRolloverGroupElement.class);
        elementRow1.getChildren().addAll(bumperButton, wallButton, wallPathButton, wallArcButton, rolloverButton);

        HBox elementRow2 = new HBox(5);
        Button dropTargetsButton = createNewElementButton(
                "droptargets.png", "Targets", EditableDropTargetGroupElement.class);
        Button spinnerButton = createNewElementButton(
                "spinner.png", "Spinner", EditableSpinnerElement.class);
        Button sensorButton = createNewElementButton(
                "sensor.png", "Sensor", EditableSensorElement.class);
        Button flipperButton = createNewElementButton(
                "flipper.png", "Flipper", EditableFlipperElement.class);
        elementRow2.getChildren().addAll(dropTargetsButton, spinnerButton, sensorButton, flipperButton);

        vbox.getChildren().addAll(elementRow1, elementRow2);

        this.getChildren().add(vbox);
    }

    Button createImageButton(String imageFilename, String label) {
        String url = IMAGE_PATH_PREFIX + imageFilename;
        Image img = new Image(url, 256, 192, true, true);
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(48);
        imageView.setFitHeight(36);
        Button button = new Button(localizedString(label));
        button.setGraphic(imageView);
        button.setContentDisplay(ContentDisplay.TOP);
        button.setMaxWidth(48);
        button.setMaxHeight(48);
        return button;
    }

    Button createNewElementButton(String imageFilename, String label, Class<? extends EditableFieldElement> elementClass) {
        Button button = createImageButton(imageFilename, label);
        button.setOnAction((event) -> createCallback.accept(elementClass));
        button.setFont(new Font(10));
        return button;
    }
}
