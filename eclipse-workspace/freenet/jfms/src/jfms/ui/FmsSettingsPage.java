package jfms.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;


public class FmsSettingsPage implements WizardPage {
	private final WizardSettings settings;
	private final TextField messageBaseText = new TextField();
	private final TextArea seedIdsText = new TextArea();

	public FmsSettingsPage(WizardSettings settings) {
		this.settings = settings;
	}

	@Override
	public String getHeader() {
		return "FMS Settings";
	}

	@Override
	public Node getContent() {
		final Tooltip messageBaseTooltip = new Tooltip("Leave unchanged to be compatible with FMS\n" +
		"modify to create your own network");
		Label messageBaseLabel = new Label("Message Base");
		messageBaseText.setText(settings.getMessageBase());
		messageBaseText.setTooltip(messageBaseTooltip);

		final Tooltip seedIdsTooltip = new Tooltip("Leave unchanged to use default FMS seed identities\n");
		Label seedIdsLabel = new Label("Seed Identities");
		seedIdsText.setText(settings.getSeedIdentitiesAsText());
		seedIdsText.setTooltip(seedIdsTooltip);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		grid.add(messageBaseLabel, 0, 0);
		grid.add(messageBaseText, 1, 0);
		grid.add(seedIdsLabel, 0, 1);
		grid.add(seedIdsText, 1, 1);

		return grid;
	}

	@Override
	public boolean commit() {
		settings.setSeedIdentities(seedIdsText.getText());
		settings.setMessageBase(messageBaseText.getText());

		return true;
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public WizardPage nextPage() {
		return new FinishPage(settings);
	}
}
