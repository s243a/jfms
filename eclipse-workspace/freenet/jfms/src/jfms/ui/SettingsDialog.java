package jfms.ui;

import java.util.Arrays;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import jfms.config.ChoiceValidator;
import jfms.config.Config;
import jfms.config.ConfigEntry;
import jfms.config.Constants;

public class SettingsDialog {
	private final Dialog<ButtonType> dialog = new Dialog<>();
	private final GridPane grid = new GridPane();
	private int[] entries;
	private Control[] valueNodes;

	public SettingsDialog(Window ownerWindow) {
		entries = new int[0];

		final Node tree = createSettingsTree();
		SplitPane sp = new SplitPane(tree, createSettingsPane());
		SplitPane.setResizableWithParent(tree, false);

		dialog.initOwner(ownerWindow);
		dialog.setTitle("Settings");
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(sp);

		List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
		buttons.add(ButtonType.OK);
		buttons.add(ButtonType.APPLY);
		buttons.add(ButtonType.CANCEL);

		Button okButton = (Button)dialog.getDialogPane()
			.lookupButton(ButtonType.OK);
		okButton.addEventHandler(ActionEvent.ACTION, new OkButtonHandler(false));

		Button applyButton = (Button)dialog.getDialogPane()
			.lookupButton(ButtonType.APPLY);
		applyButton.addEventFilter(ActionEvent.ACTION, new OkButtonHandler(true));
	}


	private class OkButtonHandler implements EventHandler<ActionEvent> {
		private final boolean doConsume;

		public OkButtonHandler(boolean doConsume) {
			this.doConsume = doConsume;
		}

		@Override
		public void handle(ActionEvent e) {
			if (doConsume) {
				e.consume();
			}

			final Config config = Config.getInstance();
			for (int i=0; i<entries.length; i++) {
				int id = entries[i];
				ConfigEntry entry = config.getEntry(id);

				String value;
				switch (entry.getType()) {
				case BOOLEAN:
					CheckBox cb = (CheckBox)valueNodes[i];
					value = Boolean.toString(cb.isSelected());
					break;
				case CHOICE:
					@SuppressWarnings("rawtypes")
					ChoiceBox choice = (ChoiceBox)valueNodes[i];
					value = (String)choice.getValue();
					break;
				default:
					TextField text = (TextField)valueNodes[i];
					value = text.getText();
					break;
				}

				config.setStringValue(id, value);
			}
			config.saveToFile(Constants.JFMS_CONFIG_PATH);
		}
	}

	private class GroupSelectionChangeListener
		implements ChangeListener<TreeItem<String>> {

		@Override
		public void changed(
				ObservableValue<? extends TreeItem<String>> observable,
				TreeItem<String> oldValue, TreeItem<String> newValue) {

			String infoText = null;
			grid.getChildren().clear();

			if (newValue == null) {
				entries = new int[0];
				return;
			}

			switch (newValue.getValue()) {
			case "Freenet":
				entries = new int[] {
					Config.FCP_HOST,
					Config.FCP_PORT
				};
				break;
			case "FMS":
				entries = new int[] {
					Config.MESSAGEBASE,
					Config.DL_MSGLISTS_WITHOUT_TRUST,
					Config.DL_MESSAGES_WITHOUT_TRUST,
				};
				break;
			case "Theme":
				entries = new int[] {
					Config.ICON_SET
				};
				break;
			case "Messages":
				entries = new int[] {
					Config.WEBVIEW
				};
				infoText =
					"WARNING: WebView uses a built-in HTML browser to render "
					+ "messages. If there are bugs in the implementation, an "
					+ "attacker might be able perform an XSS attack (e.g. "
					+ "load an external image).";
				break;
			default:
				entries = new int[0];
				return;
			}

			valueNodes = new Control[entries.length];

			final Config config = Config.getInstance();
			int row = 0;
			for (int id: entries) {
				ConfigEntry entry = config.getEntry(id);

				Label label = new Label(entry.getName());
				Control value;

				switch (entry.getType()) {
				case BOOLEAN:
					CheckBox cb = new CheckBox();
					cb.setSelected(config.getBooleanValue(id));
					value = cb;
					break;
				case CHOICE:
					ChoiceValidator validator =
						(ChoiceValidator)entry.getValidator();
					List<String> items = validator.getAllowedValues();
					ChoiceBox<String> choice = new ChoiceBox<>(FXCollections
							.observableArrayList(items));
					choice.setValue(config.getStringValue(id));
					value = choice;
					break;
				default:
					value = new TextField(config.getStringValue(id));
				}

				if (entry.getDescription() != null) {
					Tooltip.install(value, new Tooltip(entry.getDescription()));
				}
				valueNodes[row] = value;
				grid.addRow(row++, label, value);
			}

			if (infoText != null) {
				Label info = new Label(infoText);
				info.setWrapText(true);
				grid.add(info, 0, row, 2, 1);
			}
		}
	}

	public void showAndWait() {
		dialog.showAndWait();
	}

	private Node createSettingsTree() {
		TreeItem<String> generalFolder = new TreeItem<>("General Settings");
		generalFolder.getChildren().addAll(Arrays.asList(
				new TreeItem<>("Freenet"),
				new TreeItem<>("FMS")
				));
		generalFolder.setExpanded(true);


		TreeItem<String> appearanceFolder = new TreeItem<>("Appearance");
		appearanceFolder.getChildren().addAll(Arrays.asList(
				new TreeItem<>("Theme"),
				new TreeItem<>("Messages")
				));
		appearanceFolder.setExpanded(true);

		TreeItem<String> rootItem = new TreeItem<>();
		rootItem.getChildren().addAll(Arrays.asList(
					generalFolder, appearanceFolder));
		TreeView<String> treeView = new TreeView<>(rootItem);
		treeView.setShowRoot(false);

		treeView.getSelectionModel().selectedItemProperty().addListener(
				new GroupSelectionChangeListener());

		return treeView;
	}

	private Node createSettingsPane() {
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));
		grid.setPrefWidth(300);

		return grid;
	}
}
