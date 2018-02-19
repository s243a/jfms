package jfms.ui;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import jfms.fcp.FcpClient;
import jfms.fcp.FcpStatusListener;

public class StatusBar implements FcpStatusListener {
	private final HBox hbox;
	private final ProgressBar progressBar;
	private final Label statusLabel;
	private final ImageView networkIcon;
	private Label notificationLabel;
	private final Tooltip networkTooltip;
	private Button cancelButton;

	public StatusBar() {
		// Online/Offline Icon
		networkIcon = new ImageView(Icons.getInstance()
				.getNetworkIcon(Icons.NetworkStatus.OFFLINE));
		networkTooltip = new Tooltip("Offline");
		Tooltip.install(networkIcon, networkTooltip);

		// status/progress text
		progressBar = new ProgressBar();
		progressBar.setTooltip(new Tooltip());

		statusLabel = new Label();
		statusLabel.setMaxWidth(Double.MAX_VALUE);

		// notification icon/text
		notificationLabel = new Label();
		notificationLabel.setPrefWidth(200);

		// notification cancel button
		cancelButton = new Button(null,
				new ImageView(Icons.getInstance().getCloseIcon()));
		Utils.setToolBarButtonStyle(cancelButton);
		cancelButton.setPadding(Insets.EMPTY);
		cancelButton.setOnAction((ActionEvent t) -> {
				cancelButton.setVisible(false);
				notificationLabel.setVisible(false);
				});
		cancelButton.setVisible(false);

		hbox = new HBox(networkIcon, progressBar, statusLabel,
				new Separator(Orientation.VERTICAL),
				notificationLabel, cancelButton);
		hbox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(statusLabel, Priority.ALWAYS);
		hbox.setPadding(new Insets(2, 5, 2, 5));
		hbox.setSpacing(4);

		update(Icons.NetworkStatus.OFFLINE, null, null);
	}

	public Node getNode() {
		return hbox;
	}

	public final StringProperty progressTitleProperty() {
		return statusLabel.textProperty();
	}

	public final DoubleProperty progressProperty() {
		return progressBar.progressProperty();
	}

	public final StringProperty statusTextProperty() {
		return progressBar.getTooltip().textProperty();
	}

	@Override
	public void statusChanged(FcpClient.Status status) {
		final Icons.NetworkStatus networkStatus;
		final Icons.NotificationType notifcationType;
		final String notificationText;

		switch (status) {
		case CONNECTED:
			networkStatus = Icons.NetworkStatus.ONLINE;
			notifcationType = Icons.NotificationType.INFORMATION;
			notificationText = "Connection established";
			break;
		case DISCONNECTED:
			networkStatus = Icons.NetworkStatus.OFFLINE;
			notifcationType = Icons.NotificationType.WARNING;
			notificationText = "Connection lost";
			break;
		case CONNECT_FAILED:
			networkStatus = Icons.NetworkStatus.OFFLINE;
			notifcationType = Icons.NotificationType.WARNING;
			notificationText = "Failed to connect";
			break;
		default:
			networkStatus = Icons.NetworkStatus.UNKNOWN;
			notifcationType = Icons.NotificationType.NONE;
			notificationText = null;
		}

		Platform.runLater(() ->
				update(networkStatus,notifcationType, notificationText));
	}

	public final void update(Icons.NetworkStatus networkStatus,
			Icons.NotificationType notifcationType,
			String notificationText) {
		if (networkStatus != null ) {
			String text = null;
			switch (networkStatus) {
			case ONLINE:
				text = "Online";
				break;
			case OFFLINE:
				text = "Offline";
				break;
			}

			networkIcon.setImage(Icons.getInstance().getNetworkIcon(networkStatus));
			networkTooltip.setText(text);
		}

		if (notificationText != null ) {
			notificationLabel.setVisible(true);
			notificationLabel.setText(notificationText);
			notificationLabel.setGraphic(new ImageView(Icons.getInstance().getNotificationIcon(notifcationType)));
			cancelButton.setVisible(true);
		} else {
			cancelButton.setVisible(false);
			notificationLabel.setVisible(false);
		}
	}
}
