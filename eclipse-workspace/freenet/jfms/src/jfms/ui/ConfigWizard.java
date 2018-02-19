package jfms.ui;

import java.util.List;
import java.util.LinkedList;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

public class ConfigWizard {
	private final WizardSettings settings;
	private WizardPage currentPage;
	private final Dialog<ButtonType> dialog = new Dialog<>();
	private final LinkedList<WizardPage> history = new LinkedList<>();

	public ConfigWizard() {
		dialog.setTitle("Welcome to JFMS");

		List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
		buttons.add(ButtonType.CANCEL);
		buttons.add(ButtonType.NEXT);

		settings = new WizardSettings();
		currentPage = new ImportWizardPage(settings);

		Button nextButton = (Button)dialog.getDialogPane().lookupButton(ButtonType.NEXT);
		nextButton.addEventFilter(ActionEvent.ACTION, new NextButtonHandler());

		updateDialog();
	}

	private class NextButtonHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			if (!currentPage.commit()) {
				e.consume();
			}

			history.push(currentPage);
			currentPage = currentPage.nextPage();
			if (currentPage == null) {
				return;
			}

			e.consume();

			List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
			if (!currentPage.hasNext()) {
				buttons.remove(ButtonType.NEXT);
				if (!buttons.contains(ButtonType.FINISH)) {
					buttons.add(ButtonType.FINISH);
					Button finishButton = (Button)dialog.getDialogPane().lookupButton(ButtonType.FINISH);
					finishButton.addEventFilter(ActionEvent.ACTION, new NextButtonHandler());
				}
			}

			if (!buttons.contains(ButtonType.PREVIOUS)) {
				buttons.add(ButtonType.PREVIOUS);
				Button previousButton = (Button)dialog.getDialogPane().lookupButton(ButtonType.PREVIOUS);
				previousButton.addEventFilter(ActionEvent.ACTION, new PreviousButtonHandler());
			}

			updateDialog();
		}
	}

	private class PreviousButtonHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			e.consume();
			currentPage = history.pop();

			List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
			buttons.remove(ButtonType.FINISH);
			if (!buttons.contains(ButtonType.NEXT)) {
				buttons.add(ButtonType.NEXT);
				Button nextButton = (Button)dialog.getDialogPane().lookupButton(ButtonType.NEXT);
				nextButton.addEventFilter(ActionEvent.ACTION, new NextButtonHandler());
			}
			if (history.isEmpty()) {
				buttons.remove(ButtonType.PREVIOUS);
			}

			updateDialog();
		}
	}

	public void run() {
		dialog.showAndWait();
	}

	public WizardSettings getSettings() {
		return settings;
	}

	private void updateDialog()
	{
		DialogPane dialogPane = dialog.getDialogPane();

		dialogPane.setHeaderText(currentPage.getHeader());
		dialogPane.setContent(currentPage.getContent());
		dialogPane.getScene().getWindow().sizeToScene();
	}

}
