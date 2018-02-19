package jfms.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import jfms.config.Config;

public class FinishPage implements WizardPage {
	private final WizardSettings settings;

	public FinishPage(WizardSettings settings) {
		this.settings = settings;
	}

	@Override
	public String getHeader() {
		return "Configuration Complete";
	}

	@Override
	public Node getContent() {
		Label label = new Label("Please Review your settings:");

		Label fcpHostLabel = new Label("FCP Host:");
		Label fcpHostText= new Label(settings.getFcpHost());

		Label fcpPortLabel = new Label("FCP Port:");
		Label fcpPortText = new Label(Integer.toString(settings.getFcpPort()));

		Label messageBaseLabel = new Label("Message Base:");
		Label messageBaseText = new Label(settings.getMessageBase());

		Label seedIdsLabel = new Label("Seed Identities:");
		Label seedIdsText = new Label(
				Integer.toString(settings.getSeedIdentities().size())
				+ " (details on previous page)");

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		grid.add(label, 0, 0);
		grid.add(fcpHostLabel, 0, 1);
		grid.add(fcpHostText, 1, 1);
		grid.add(fcpPortLabel, 0, 2);
		grid.add(fcpPortText, 1, 2);
		grid.add(messageBaseLabel, 0, 3);
		grid.add(messageBaseText, 1, 3);
		grid.add(seedIdsLabel, 0, 4);
		grid.add(seedIdsText, 1, 4);

		return grid;
	}

	@Override
	public boolean commit() {
		Config config = Config.getInstance();
		config.setStringValue(Config.FCP_HOST, settings.getFcpHost());
		// TODO don't do double string<>int conversion
		config.setStringValue(Config.FCP_PORT, Integer.toString(settings.getFcpPort()));
		config.setStringValue(Config.MESSAGEBASE, settings.getMessageBase());
		// TODO seed identities

		return true;
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public WizardPage nextPage() {
		return null;
	}
}
