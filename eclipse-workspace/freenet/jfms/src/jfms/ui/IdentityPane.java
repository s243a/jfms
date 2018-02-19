package jfms.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.config.Config;
import jfms.fms.FmsManager;
import jfms.fms.IdentityManager;
import jfms.fms.Store;
import jfms.fms.TrustManager;
import jfms.fms.Validator;

public class IdentityPane {
	private static final Logger LOG = Logger.getLogger(IdentityPane.class.getName());
	private static final Pattern TRUSTLEVEL_PATTERN = Pattern
		.compile("100|\\p{Digit}?\\p{Digit}");

	private final SplitPane sp = new SplitPane();
	private final TableView<Identity> table;
	private final Label nameText = new Label();
	private final Label sskText = new Label();
	private final Label peerMessageTrustText = new Label();
	private final Label peerTrustListTrustText= new Label();
	private final Spinner<Integer> localMessageTrustSpinner = new Spinner<>();
	private final Spinner<Integer> localTrustListTrustSpinner = new Spinner<>();
	private final Label signatureText = new Label();
	private Integer selectedIdentityId;

	public IdentityPane() {
		table = createIdentityTable();
		sp.setOrientation(Orientation.VERTICAL);
		sp.getItems().addAll(table, createIdentityDetails());
		//sp.setDividerPositions(0.3f);

		//SplitPane.setResizableWithParent(table, false);
	}

	private class IdentitySelectionChangeListener implements ChangeListener<Identity> {
		@Override
		public void changed(ObservableValue<? extends Identity> observable, Identity oldValue, Identity newValue) {
			IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
			jfms.fms.Identity id = null;

			if (newValue != null) {
				id = identityManager.getIdentity(newValue.getId());
				if (id != null) {
					selectedIdentityId = newValue.getId();
				} else {
					LOG.log(Level.WARNING, "failed to retrieve ID {0}",
							newValue.getId());
				}
			} else {
				selectedIdentityId = null;
			}

			if (id != null) {
				TrustManager trustManager = FmsManager.getInstance().getTrustManager();
				Integer peerMessageTrust = trustManager.getPeerMessageTrust(selectedIdentityId);
				String peerMessageTrustString;
				if (peerMessageTrust != null) {
					peerMessageTrustString = peerMessageTrust.toString();
				} else {
					peerMessageTrustString = "";
				}

				Integer peerTrustListTrust = trustManager.getPeerTrustListTrust(selectedIdentityId);
				String peerTrustListTrustString;
				if (peerTrustListTrust != null) {
					peerTrustListTrustString = peerTrustListTrust.toString();
				} else {
					peerTrustListTrustString = "";
				}

				Integer localMessageTrust = trustManager.getLocalMessageTrust(selectedIdentityId);
				String localMessageTrustString;
				if (localMessageTrust != null) {
					localMessageTrustString = localMessageTrust.toString();
				} else {
					localMessageTrustString = "";
				}

				Integer localTrustListTrust = trustManager.getLocalTrustListTrust(selectedIdentityId);
				String localTrustListTrustString;
				if (localTrustListTrust != null) {
					localTrustListTrustString = localTrustListTrust.toString();
				} else {
					localTrustListTrustString = "";
				}

				nameText.setText(id.getName());
				sskText.setText(id.getSsk());
				signatureText.setText(id.getSignature());
				peerMessageTrustText.setText(peerMessageTrustString);
				peerTrustListTrustText.setText(peerTrustListTrustString);
				localMessageTrustSpinner.getEditor().setText(localMessageTrustString);
				localTrustListTrustSpinner.getEditor().setText(localTrustListTrustString);
			} else {
				nameText.setText("");
				sskText.setText("");
				peerMessageTrustText.setText("");
				peerTrustListTrustText.setText("");
				localMessageTrustSpinner.getEditor().clear();
				localTrustListTrustSpinner.getEditor().clear();
				signatureText.setText("");
			}
		}
	}


	public void show(Window ownerWindow) {
		Scene scene = new Scene(sp);

		Stage stage = new Stage();
		stage.initOwner(ownerWindow);
		stage.setTitle("Identities");
		stage.setScene(scene);
		stage.show();
	}

	private TableView<Identity> createIdentityTable() {
		TableColumn<Identity,String> nameColumn = new TableColumn<>("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameColumn.setComparator(Comparator.nullsLast(nameColumn.getComparator()));
		nameColumn.setSortType(TableColumn.SortType.ASCENDING);

		TableColumn<Identity,String> trustColumn = new TableColumn<>("Message Trust");
		trustColumn.setCellValueFactory(new PropertyValueFactory<>("messageTrust"));

		TableColumn<Identity,String> trustListTrustColumn =
			new TableColumn<>("Trustlist Trust");
		trustListTrustColumn.setCellValueFactory(
				new PropertyValueFactory<>("trustListTrust"));

		TableColumn<Identity,String> sskColumn = new TableColumn<>("SSK");
		sskColumn.setCellValueFactory(new PropertyValueFactory<>("ssk"));

		TableView<Identity> tableView = new TableView<>();
		tableView.getColumns().addAll(Arrays.asList(
					nameColumn, trustColumn, trustListTrustColumn, sskColumn));
		tableView.setMinWidth(Region.USE_PREF_SIZE);
		tableView.setItems(createIdentityList());
		tableView.getSortOrder().add(nameColumn);
		tableView.getSelectionModel().selectedItemProperty()
			.addListener(new IdentitySelectionChangeListener());

		tableView.setPrefWidth(600);

		return tableView;
	}

	private void initTrustSpinner(Spinner<Integer> spinner) {
		spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,100));
		spinner.setEditable(true);
		spinner.getEditor().focusedProperty().addListener(
			(ObservableValue<? extends Boolean> observable,
			 Boolean oldValue, Boolean newValue) -> {
				if (!newValue) {
					updateTrustSpinner(spinner);
				}
			});
	}

	private void updateTrustSpinner(Spinner<Integer> spinner) {
		final String trustStr = spinner.getEditor().getText();

		boolean trustValid = false;
		if (!trustStr.isEmpty()) {
			Matcher m = TRUSTLEVEL_PATTERN.matcher(trustStr);
			trustValid = m.matches();
		}

		if (trustValid) {
			// force update
			spinner.increment(0);
		} else {
			spinner.getEditor().clear();
		}
	}

	private Integer getTrustSpinnerValue(Spinner<Integer> spinner) {
		Integer trustValue = null;
		if (!spinner.getEditor().getText().isEmpty()) {
			trustValue = spinner.getValue();
		}

		return trustValue;
	}

	private Node createIdentityDetails() {
		Label nameLabel = new Label("Name");
		Label sskLabel = new Label("SSK");
		Label peerMessageTrustLabel = new Label("Peer Message Trust");
		Label peerTrustListTrustLabel = new Label("Peer Trust List Trust");
		Label localMessageTrustLabel = new Label("Local Message Trust");
		Label localTrustListTrustLabel = new Label("Local Trust List Trust");
		Label signatureLabel = new Label("Signature");

		signatureText.setWrapText(true);

		localMessageTrustSpinner.setMaxWidth(80);
		initTrustSpinner(localMessageTrustSpinner);

		localTrustListTrustSpinner.setMaxWidth(80);
		initTrustSpinner(localTrustListTrustSpinner);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(2);
		grid.setPadding(new Insets(2, 5, 2, 5));

		int row = 0;
		grid.addRow(row++, nameLabel, nameText);
		grid.addRow(row++, sskLabel, sskText);
		grid.addRow(row++, peerMessageTrustLabel, peerMessageTrustText);
		grid.addRow(row++, peerTrustListTrustLabel, peerTrustListTrustText);
		grid.addRow(row++, localMessageTrustLabel, localMessageTrustSpinner);
		grid.addRow(row++, localTrustListTrustLabel, localTrustListTrustSpinner);
		grid.addRow(row++, signatureLabel, signatureText);

		final Separator sepHor = new Separator();
		GridPane.setColumnSpan(sepHor, 2);

		grid.add(sepHor, 0, row++);

		Button refreshButton = new Button("Refresh");
		refreshButton.setOnAction((ActionEvent e) -> {
			// TODO preserve sort order
			table.setItems(createIdentityList());
		});

		final boolean hasDefaultId = Config.getInstance().getDefaultId() > 0;

		Button applyButton = new Button("Apply");
		if (!hasDefaultId) {
			applyButton.setDisable(true);
		}

		GridPane.setValignment(signatureLabel, VPos.TOP);

		applyButton.setOnAction((ActionEvent e) -> {
			TrustManager trustManager = FmsManager.getInstance().getTrustManager();
			trustManager.updateLocalTrust(selectedIdentityId,
					getTrustSpinnerValue(localMessageTrustSpinner),
					getTrustSpinnerValue(localTrustListTrustSpinner));
		});
		grid.addRow(row++, refreshButton, applyButton);

		Button addButton = new Button("Add Identity...");
		addButton.setOnAction(e -> {
				Dialog<String> dialog = new TextInputDialog();
				dialog.setTitle("Add Identity");
				dialog.setHeaderText("Enter SSK");
				dialog.setGraphic(null);
				Optional<String> result = dialog.showAndWait();
				if (result.isPresent() && !result.get().isEmpty()) {
					addIdentity(result.get());
				}
		});
		grid.add(addButton, 0, row++);

		if (!hasDefaultId) {
			Label info = new Label("Setting local trust is only possible "
					+ "if you have at least one local identity.");
			info.setWrapText(true);
			grid.add(info, 0, row++, 2, 1);
		}

		grid.setAlignment(Pos.TOP_LEFT);

		ColumnConstraints col1 = new ColumnConstraints();
		ColumnConstraints col2 = new ColumnConstraints();
		col1.setMinWidth(Region.USE_PREF_SIZE);
		grid.getColumnConstraints().addAll(col1, col2);

		return grid;
	}

	private ObservableList<Identity> createIdentityList() {
		ObservableList<Identity> identityList = FXCollections.observableArrayList();
		TrustManager trustManager = FmsManager.getInstance().getTrustManager();
		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();

		for (Map.Entry<Integer, jfms.fms.Identity> e : identityManager.getIdentities().entrySet()) {
			jfms.fms.Identity storeId = e.getValue();

			Identity id = new Identity();
			id.setId(e.getKey());
			id.setName(storeId.getName());
			id.setSsk(storeId.getSsk());

			Integer messageTrust = trustManager.getPeerMessageTrust(e.getKey());
			if (messageTrust != null) {
				id.setMessageTrust(messageTrust);
			} else {
				id.setMessageTrust(-1);
			}

			Integer trustListTrust = trustManager.getPeerTrustListTrust(e.getKey());
			if (trustListTrust != null) {
				id.setTrustListTrust(trustListTrust);
			} else {
				id.setTrustListTrust(-1);
			}

			identityList.add(id);
		}
		return identityList;
	}

	private void addIdentity(String ssk) {
		if (!Validator.isValidSsk(ssk)) {
			LOG.log(Level.INFO, "Ignoring identity {0} (SSK invalid)", ssk);
			return;
		}

		IdentityManager identityManager
				= FmsManager.getInstance().getIdentityManager();

		if (identityManager.getIdentityId(ssk) != null) {
			LOG.log(Level.FINER, "Ignoring identity {0} (already exists)", ssk);
			return;
		}

		Store store = FmsManager.getInstance().getStore();
		int identityId = store.addManualIdentity(ssk);
		if (identityId > 0) {
			identityManager.addIdentity(identityId,	new jfms.fms.Identity(ssk));
		}
	}
}
