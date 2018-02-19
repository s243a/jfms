package jfms.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

public class FreenetSettingsPage implements WizardPage {
	private final WizardSettings settings;
	private final TextField fcpHostText = new TextField();
	private final TextField fcpPortText = new TextField();

	public FreenetSettingsPage(WizardSettings settings) {
		this.settings = settings;
	}

	@Override
	public String getHeader() {
		return "Freenet Settings";
	}

	@Override
	public Node getContent() {
		final Tooltip tooltip = new Tooltip("Modify for nonstandard Freenet installation\n"
			+ "Leave unchanged otherwise");

		Label fcpHostLabel = new Label("FCP Host");
		fcpHostText.setText(settings.getFcpHost());
		fcpHostText.setTooltip(tooltip);

		Label fcpPortLabel = new Label("FCP Port");
		fcpPortText.setText(Integer.toString(settings.getFcpPort()));
		fcpPortText.setTooltip(tooltip);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		grid.add(fcpHostLabel, 0, 0);
		grid.add(fcpHostText, 1, 0);
		grid.add(fcpPortLabel, 0, 1);
		grid.add(fcpPortText, 1, 1);

		return grid;
	}

	@Override
	public boolean commit() {
		// TODO add checks
		settings.setFcpHost(fcpHostText.getText());
		settings.setFcpPort(Integer.parseInt(fcpPortText.getText()));
		return true;
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public WizardPage nextPage() {
		return new FmsSettingsPage(settings);
	}
}
