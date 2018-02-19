package jfms.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.fms.FmsManager;
import jfms.fms.IdentityManager;
import jfms.fms.LocalIdentity;
import jfms.fms.Store;

public class LocalIdentityPane {
	private final Map<String, Integer> sskToStoreIdMap = new HashMap<>();
	private final ListView<LocalIdentity> list = new ListView<>();
	private final Button defaultButton = new Button();
	private final Button editButton = new Button();
	private final Button deleteButton = new Button();

	static private class LocalIdentityCell extends ListCell<LocalIdentity> {
		@Override
		public void updateItem(LocalIdentity item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
			} else {
				setText(item.getFullName());
			}
		}
	}

	private class IdSelectionChangeListener implements ChangeListener<LocalIdentity> {
		@Override
		public void changed(ObservableValue<? extends LocalIdentity> observable, LocalIdentity oldValue, LocalIdentity newValue) {
			if (newValue == null) {
				defaultButton.setDisable(true);
				editButton.setDisable(true);
				deleteButton.setDisable(true);
			} else {
				final int selectedId = sskToStoreIdMap.get(newValue.getSsk());
				final boolean isDefault =
					selectedId == Config.getInstance().getDefaultId();
				defaultButton.setDisable(isDefault);
				editButton.setDisable(false);
				deleteButton.setDisable(false);
			}
		}
	}

	public void show(Window ownerWindow) {
		Stage stage = new Stage();

		list.getSelectionModel().selectedItemProperty()
			.addListener(new IdSelectionChangeListener());
		list.setCellFactory((ListView<LocalIdentity> l) -> new LocalIdentityCell());
		updateListItems();

		Button addButton = new Button("Add...");
		addButton.setDisable(true);
		addButton.setMaxWidth(120);

		Button importButton = new Button("Import...");
		importButton.setMaxWidth(120);
		importButton.setOnAction((ActionEvent t) -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Open Identity XML");
			fileChooser.getExtensionFilters().addAll(
					new ExtensionFilter("XML Files", "*.xml"),
					new ExtensionFilter("All Files", "*.*"));

			File selectedFile = fileChooser.showOpenDialog(stage);
			if (selectedFile != null) {
				final int importCount = FmsManager.getInstance()
					.getIdentityManager()
					.importLocalIdentities(selectedFile);

				Dialog<ButtonType> importDialog = new Alert(Alert.AlertType.INFORMATION);
				importDialog.setTitle("Identity import complete");
				importDialog.setHeaderText("Identity import complete");
				importDialog.setContentText("Imported " + importCount +
						" identities.");
				Optional<ButtonType> result = importDialog.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.OK) {
					updateListItems();
				}
			}
		});

		editButton.setText("Edit...");
		editButton.setDisable(true);
		editButton.setMaxWidth(120);
		editButton.setOnAction((ActionEvent t) -> {
			final LocalIdentity id = list.getSelectionModel()
				.getSelectedItem();
			final EditLocalIdentityDialog dialog = new EditLocalIdentityDialog(id);
			if (dialog.showAndWait()) {
				updateListItems();
			}
		});

		defaultButton.setText("Set Default");
		defaultButton.setDisable(true);
		defaultButton.setMaxWidth(120);
		defaultButton.setOnAction((ActionEvent t) -> {
			final IdentityManager identityManager =
				FmsManager.getInstance().getIdentityManager();
			final LocalIdentity id = list.getSelectionModel()
				.getSelectedItem();
			if (id != null) {
				int storeId = sskToStoreIdMap.get(id.getSsk());
				Config.getInstance().setStringValue(Config.DEFAULT_ID, Integer.toString(storeId));
				Config.getInstance().saveToFile(Constants.JFMS_CONFIG_PATH);
			}
		});

		deleteButton.setText("Delete");
		deleteButton.setMaxWidth(120);
		deleteButton.setOnAction((ActionEvent t) -> deleteSelectedIdentity());

		Label info = new Label("WARNING: It is easy to detect if you are "
			+ "using jfms instead of the original client. You should not "
			+ "use separate identities for security reasons. It may be easy "
			+ "to guess if two identities are linked, especially if you do "
			+ "not run jfms 24/7.");
		info.setMaxWidth(160);
		info.setWrapText(true);


		VBox vbox = new VBox();
		vbox.setSpacing(10);
		vbox.setPadding(new Insets(0, 10, 0, 10));
		vbox.setSpacing(5.0);
		vbox.getChildren().addAll(addButton, importButton,
				editButton, defaultButton, deleteButton, info);


		HBox hbox = new HBox();
		hbox.getChildren().addAll(list, vbox);
		hbox.setPadding(new Insets(10, 10, 10, 10));

		Scene scene = new Scene(hbox);

		stage.initOwner(ownerWindow);
		stage.setTitle("Local Identities");
		stage.setScene(scene);
		stage.show();
	}

	private void deleteSelectedIdentity() {
		final IdentityManager identityManager =
			FmsManager.getInstance().getIdentityManager();
		final LocalIdentity id = list.getSelectionModel().getSelectedItem();
		if (id == null) {
			return;
		}

		Dialog<ButtonType> dialog = new Alert(Alert.AlertType.CONFIRMATION);
		dialog.setTitle("Deleting local identity");
		dialog.setHeaderText("Are you sure you want to delete the "
				+ "local identity\n"
				+ id.getFullName());
		dialog.setContentText("The local identity and all associated data will "
			+ "be permanently deleted.");
		Optional<ButtonType> result = dialog.showAndWait();
		if (!result.isPresent() || result.get() != ButtonType.OK) {
			return;
		}

		final int storeId = sskToStoreIdMap.get(id.getSsk());
		Store store = FmsManager.getInstance().getStore();
		store.deleteLocalIdentity(storeId);

		final Config config = Config.getInstance();
		if (storeId == config.getDefaultId()) {
			config.setStringValue(Config.DEFAULT_ID,
					Constants.DEFAULT_DEFAULT_ID);
			config.saveToFile(Constants.JFMS_CONFIG_PATH);
		}
		updateListItems();
	}

	private void updateListItems() {
		Store store = FmsManager.getInstance().getStore();

		Map<Integer, LocalIdentity> identities = store.retrieveLocalIdentities();
		List<LocalIdentity> ids = new ArrayList<>();
		for (Map.Entry<Integer, LocalIdentity> e : identities.entrySet()) {
			sskToStoreIdMap.put(e.getValue().getSsk(), e.getKey());
			ids.add(e.getValue());
		}

		ObservableList<LocalIdentity> items = FXCollections.observableArrayList(ids);
		list.setItems(items);
	}

}
