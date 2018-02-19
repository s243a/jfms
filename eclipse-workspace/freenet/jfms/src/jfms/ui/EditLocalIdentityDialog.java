package jfms.ui;

import java.util.List;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import jfms.fms.FmsManager;
import jfms.fms.LocalIdentity;

public class EditLocalIdentityDialog {
	private final LocalIdentity localIdentity;
	private final Dialog<ButtonType> dialog;
	private final TextField nameText;
	private final TextArea signatureText;
	private final TextField avatarText;
	private final CheckBox activeCb;
	private final CheckBox publishTrustListCb;

	public EditLocalIdentityDialog(LocalIdentity id) {
		this.localIdentity = id;

		dialog = new Dialog<>();
		dialog.setTitle("Edit local identity" + id.getName());
		dialog.setHeaderText("Configure identity\n" + id.getFullName());

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		final Label nameLabel = new Label("Name");
		nameText = new TextField(id.getName());

		final Label signatureLabel = new Label("Signature");
		signatureText = new TextArea(id.getSignature());
		signatureText.setPrefRowCount(3);

		final Label avatarLabel = new Label("Avatar");
		avatarText = new TextField(id.getAvatar());

		final Label activeLabel = new Label("Active");
		activeCb = new CheckBox();
		activeCb.setSelected(id.getIsActive());

		final Label singleUseLabel = new Label("Single Use");
		final CheckBox singleUseCb = new CheckBox();
		singleUseCb.setSelected(id.getSingleUse());
		singleUseCb.setDisable(true);

		final Label publishTrustListLabel = new Label("Publish Trust List");
		publishTrustListCb = new CheckBox();
		publishTrustListCb.setSelected(id.getPublishTrustList());

		final Label publishBoardListLabel = new Label("Publish Board List");
		final CheckBox publishBoardListCb = new CheckBox();
		publishBoardListCb.setSelected(id.getSingleUse());
		publishBoardListCb.setDisable(true);

		final Label freesiteEditionLabel = new Label("Freesite Edition");
		final TextField freesiteEditionText =
			new TextField(Integer.toString(id.getFreesiteEdition()));
		freesiteEditionText.setDisable(true);

		grid.add(nameLabel, 0, 0);
		grid.add(nameText, 1, 0);
		grid.add(signatureLabel, 0, 1);
		grid.add(signatureText, 1, 1);
		grid.add(avatarLabel, 0, 2);
		grid.add(avatarText, 1, 2);
		grid.add(activeLabel, 0, 3);
		grid.add(activeCb, 1, 3);
		grid.add(singleUseLabel, 0, 4);
		grid.add(singleUseCb, 1, 4);
		grid.add(publishTrustListLabel, 0, 5);
		grid.add(publishTrustListCb, 1, 5);
		grid.add(publishBoardListLabel, 0, 6);
		grid.add(publishBoardListCb, 1, 6);
		grid.add(freesiteEditionLabel, 0, 7);
		grid.add(freesiteEditionText, 1, 7);
		dialog.getDialogPane().setContent(grid);

		List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
		buttons.add(ButtonType.CANCEL);
		buttons.add(ButtonType.OK);

		Button okButton = (Button)dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.setOnAction(new OkHandler());
	}

	private class OkHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			final jfms.fms.Store store = FmsManager.getInstance().getStore();

			final LocalIdentity updatedId = new LocalIdentity();
			updatedId.setSsk(localIdentity.getSsk());
			updatedId.setName(nameText.getText());
			updatedId.setSignature(signatureText.getText());
			updatedId.setIsActive(activeCb.isSelected());
			updatedId.setAvatar(avatarText.getText());
			updatedId.setPublishTrustList(publishTrustListCb.isSelected());

			store.updateLocalIdentity(updatedId);
		}
	}

	public boolean showAndWait() {
		Optional<ButtonType> result = dialog.showAndWait();

		return result.isPresent() && result.get() == ButtonType.OK;
	}
}
