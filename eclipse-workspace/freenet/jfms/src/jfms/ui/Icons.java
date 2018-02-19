package jfms.ui;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.image.Image;

import jfms.config.Config;

public class Icons {
	private static final Logger LOG = Logger.getLogger(Icons.class.getName());
	private static Icons instance;

	private final String theme;

	// main toolbar
	private final Image identitiesIcon;
	private final Image boardsIcon;

	// notifications
	private final Image infoIcon;
	private final Image warnIcon;
	private final Image closeIcon;

	// message pane
	private final Image boardIcon;
	private final Image messageReadIcon;
	private final Image messageUnreadIcon;

	// new message window
	private final Image sendMessageIcon;

	// message toolbar
	private final Image newMessageIcon;
	private final Image replyIcon;
	private final Image muteIcon;
	private final Image signatureIcon;
	private final Image emoticonIcon;
	private final Image fontIcon;

	// status bar
	private final Image onlineIcon;
	private final Image offlineIcon;


	public enum NetworkStatus {
		UNKNOWN,
		ONLINE,
		OFFLINE
	}

	public enum NotificationType {
		NONE,
		WARNING,
		INFORMATION
	}

	public static synchronized Icons getInstance() {
		if (instance == null) {
			instance = new Icons();
		}

		return instance;
	}

	private Icons() {
		theme = Config.getInstance().getIconSet();

		identitiesIcon = getImage("user-properties.png");
		boardsIcon = getImage("applications-internet.png");

		infoIcon = getImage("dialog-information.png");
		warnIcon = getImage("dialog-warning.png");
		closeIcon = getImage("dialog-close.png");

		boardIcon = getImage("folder-blue.png");
		messageReadIcon = getImage("mail-read.png");
		messageUnreadIcon = getImage("mail-unread.png");

		sendMessageIcon = getImage("mail-send.png");

		newMessageIcon = getImage("mail-message-new.png");
		replyIcon = getImage("mail-reply-sender.png");
		muteIcon = getImage("audio-volume-muted.png");
		signatureIcon = getImage("document-sign.png");
		emoticonIcon = getImage("face-smile.png");
		fontIcon = getImage("gtk-select-font.png");

		onlineIcon = getImage("network-connect.png");
		offlineIcon = getImage("network-disconnect.png");
	}

	public Image getIdentitiesIcon() {
		return identitiesIcon;
	}

	public Image getBoardsIcon() {
		return boardsIcon;
	}

	public Image getNotificationIcon(NotificationType type) {
		Image image = null;
		switch (type) {
		case WARNING:
			image = warnIcon;
			break;
		case INFORMATION:
			image = infoIcon;
			break;
		}

		return image;
	}

	public Image getCloseIcon() {
		return closeIcon;
	}

	public Image getBoardIcon() {
		return boardIcon;
	}

	public Image getMessageIcon(Message.Status status) {
		Image image = null;
		switch (status) {
		case READ:
			image = messageReadIcon;
			break;
		case UNREAD:
			image = messageUnreadIcon;
			break;
		}

		return image;
	}

	public Image getSendMessageIcon() {
		return sendMessageIcon;
	}

	public Image getNewMessageIcon() {
		return newMessageIcon;
	}

	public Image getReplyIcon() {
		return replyIcon;
	}

	public Image getMuteIcon() {
		return muteIcon;
	}

	public Image getEmoticonIcon() {
		return emoticonIcon;
	}

	public Image getFontIcon() {
		return fontIcon;
	}

	public Image getSignatureIcon() {
		return signatureIcon;
	}

	public Image getNetworkIcon(NetworkStatus status) {
		Image image = null;
		switch (status) {
		case ONLINE:
			image = onlineIcon;
			break;
		case OFFLINE:
			image = offlineIcon;
			break;
		}

		return image;
	}

	private Image getImage(String name) {
		final String path = "/icons/" + theme + "/" + name;
		LOG.log(Level.FINEST, "Trying to load icon from {0}", path);

		final InputStream is = Icons.class.getResourceAsStream(path);
		if (is != null) {
			return new Image(is);
		} else {
			LOG.log(Level.FINEST, "Icon missing: {0}", path);
			return null;
		}
	}
}
