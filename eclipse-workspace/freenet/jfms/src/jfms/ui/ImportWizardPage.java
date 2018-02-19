package jfms.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

public class ImportWizardPage implements WizardPage {
	private final WizardSettings settings;
	private final RadioButton freshRb = new RadioButton();
	private final RadioButton importRb = new RadioButton();

	public ImportWizardPage(WizardSettings settings) {
		this.settings = settings;
	}

	@Override
	public String getHeader() {
		return "Import Settings";
	}

	@Override
	public Node getContent() {
		ToggleGroup group = new ToggleGroup();

		freshRb.setText("Fresh installation (don't import anything)");
		freshRb.setToggleGroup(group);
		freshRb.setSelected(true);

		importRb.setText("Import existing FMS database (not available)");
		importRb.setToggleGroup(group);
		importRb.setDisable(true);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		grid.add(freshRb, 0, 0);
		grid.add(importRb, 0, 1);
		return grid;
	}

	@Override
	public WizardPage nextPage() {
		if (freshRb.isSelected()) {
			return new FreenetSettingsPage(settings);
		} else {
			return null;
		}
	}

	@Override
	public boolean commit() {
		return true;
	}

	@Override
	public boolean hasNext() {
		return false;
	}
}
