package ui.controls.clients.natural;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.clients.NaturalPerson;
import ui.controls.AbstractView;

import java.net.URL;

public class NaturalPersonView extends AbstractView<NaturalPerson> {
    @FXML private Label nameField;
    @FXML private Label birthField;

    public NaturalPersonView() {
    }

    public NaturalPersonView(NaturalPerson data) {
        super(data);
    }

    @Override
    protected void update() {
        if (data != null) {
            nameField.setText(data.getName());
            birthField.setText(data.getDateOfBirth().toString());
        }
        else {
            nameField.setText("");
            birthField.setText("");
        }
    }

    @Override
    protected URL getFxmlUrl() {
        return getClass().getResource("NaturalPersonView.fxml");
    }
}
