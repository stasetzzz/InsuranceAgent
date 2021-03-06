package ui.controls.insurances.attributes;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.insurances.attributes.InsuranceAttribute;
import ui.controls.AbstractView;

import java.net.URL;
import java.sql.SQLException;

public class AttributeView extends AbstractView<InsuranceAttribute> {
    @FXML private Label nameField;
    @FXML private Label valueField;

    public AttributeView() {
    }

    public AttributeView(InsuranceAttribute data) {
        super(data);
    }

    @Override
    protected void update() {
        if (data != null) {
            try {
                nameField.setText(data.getAttributeTypeName());
                valueField.setText(data.getAttributeValue());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        else {
            nameField.setText("");
            valueField.setText("");
        }
    }

    @Override
    protected URL getFxmlUrl() {
        return getClass().getResource("AttributeView.fxml");
    }
}
