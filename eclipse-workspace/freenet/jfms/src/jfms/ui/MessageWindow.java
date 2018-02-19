package jfms.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.config.Config;
import jfms.fms.FmsManager;
import jfms.fms.IdentityManager;
import jfms.fms.InReplyTo;
import jfms.fms.LocalIdentity;
import jfms.fms.Store;

public class MessageWindow {
	private static final Logger LOG = Logger.getLogger(MessageWindow.class.getName());
	private static final Pattern UUIDREPLACE_PATTERN = Pattern.compile("[~-]");

	private final Stage stage = new Stage();
	private final ComboBox<String> fromCb = new ComboBox<>();
	private final TextField subjectTextField = new TextField();
	private final TextField boardsTextField = new TextField();
	private final TextArea messageContent;
	private final InReplyTo inReplyTo;
	private Map<Integer, LocalIdentity> localIds;

	public MessageWindow() {
		setFromList();
		messageContent = createMessageContent(null);
		inReplyTo = null;
	}

	public MessageWindow(jfms.fms.Message parentMessage) {
		setFromList();
		String subject = parentMessage.getSubject();
		if (!subject.startsWith("Re: ")) {
			subject = "Re: " + subject;
		}
		subjectTextField.setText(subject);

		StringBuilder boards = new StringBuilder();
		boards.append(parentMessage.getReplyBoard());
		for (String board : parentMessage.getBoards()) {
			if (!board.equals(parentMessage.getReplyBoard())) {
				boards.append(", ");
				boards.append(board);
			}
		}
		boardsTextField.setText(boards.toString());

		messageContent = createMessageContent(parentMessage);

		inReplyTo = parentMessage.getInReplyTo().increment();
		inReplyTo.add(0, parentMessage.getMessageUuid());
	}

	public void show(Window ownerWindow) {
		VBox topVBox = new VBox();
		topVBox.getChildren().addAll(createMenuBar(), createToolbar(),
				createHeaders());


		BorderPane borderPane = new BorderPane();
		borderPane.setTop(topVBox);
		borderPane.setCenter(messageContent);
		Scene scene = new Scene(borderPane);

		stage.initOwner(ownerWindow);
		stage.setTitle("Reply");
		stage.setScene(scene);
		stage.show();

		messageContent.requestFocus();
	}

	private Node createMenuBar() {
		MenuBar menuBar = new MenuBar();
		Menu messageMenu = new Menu("Message");
		MenuItem sendMenuItem = new MenuItem("Send");
		messageMenu.getItems().add(sendMenuItem);

		sendMenuItem.setOnAction((ActionEvent t) -> {
			sendMessage();
		});

		menuBar.getMenus().addAll(messageMenu);
		return menuBar;
	}

	private Node createToolbar() {
		Button sendMessageButton = new Button(null,
				new ImageView(Icons.getInstance().getSendMessageIcon()));
		sendMessageButton.setTooltip(new Tooltip("Send Message"));
		Utils.setToolBarButtonStyle(sendMessageButton);
		sendMessageButton.setOnAction((ActionEvent t) -> {
			sendMessage();
		});

		return new ToolBar(sendMessageButton);
	}

	private Node createHeaders() {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		Label fromLabel = new Label("From:");
		Label subjectLabel = new Label("Subject:");
		Label boardsLabel = new Label("Boards:");

		Font boldFont = Font.font("SansSerif", FontWeight.BOLD, 12);

		fromLabel.setFont(boldFont);
		subjectLabel.setFont(boldFont);
		boardsLabel.setFont(boldFont);

		grid.add(fromLabel, 0, 0);
		grid.add(fromCb, 1, 0);
		grid.add(subjectLabel, 0, 1);
		grid.add(subjectTextField, 1, 1);
		grid.add(boardsLabel, 0, 2);
		grid.add(boardsTextField, 1, 2);

		// second column gets any extra width
		ColumnConstraints col1 = new ColumnConstraints();
		ColumnConstraints col2 = new ColumnConstraints();
		col2.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(col1, col2);

		return grid;
	}

	private void sendMessage() {
		jfms.fms.Message msg = new jfms.fms.Message();

		LocalDateTime dateTime = LocalDateTime.now(ZoneOffset.UTC)
			.truncatedTo(ChronoUnit.SECONDS);
		String subject = subjectTextField.getText();

		jfms.fms.Store store = FmsManager.getInstance().getStore();

		int localIdentityId = 0;
		String ssk = null;
		for (Map.Entry<Integer, LocalIdentity> e : localIds.entrySet()) {
			final LocalIdentity id = e.getValue();
			if (id.getFullName().equals(fromCb.getValue())) {
				localIdentityId = e.getKey();
				ssk = id.getSsk();
			}
		}

		if (ssk == null) {
			LOG.log(Level.WARNING, "Failed to get ID for {0}",
					fromCb.getValue());
			return;
		}

		StringBuilder uuid = new StringBuilder(UUID.randomUUID().toString().toUpperCase());
		uuid.append('@');
		uuid.append(UUIDREPLACE_PATTERN.matcher(ssk.substring(4,47)).replaceAll(""));

		final List<String> boards = getBoards();

		msg.setDate(dateTime.toLocalDate());
		msg.setTime(dateTime.toLocalTime());
		msg.setSubject(subject);
		msg.setMessageUuid(uuid.toString());
		msg.setReplyBoard(boards.get(0));
		msg.setBody(messageContent.getText());

		msg.setBoards(boards);
		if (inReplyTo != null) {
			msg.setInReplyTo(inReplyTo);
		}

		jfms.fms.xml.MessageWriter writer =
			new jfms.fms.xml.MessageWriter();
		final String messageXml = writer.writeXml(msg);
		if (messageXml == null) {
			LOG.log(Level.WARNING, "Failed to create XML from message");
			return;
		}


		store.addLocalMessage(localIdentityId,
				messageXml, dateTime.toLocalDate());

		stage.hide();
	}

	private TextArea createMessageContent(jfms.fms.Message parentMessage) {
		TextArea textArea = new TextArea();
		textArea.setPrefColumnCount(80);
		textArea.setPrefRowCount(25);
		textArea.setWrapText(true);

		if (parentMessage != null) {
			IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
			jfms.fms.Identity id = identityManager.getIdentity(parentMessage.getIdentityId());
			String name = id.getFullName();

			try (BufferedReader reader = new BufferedReader(
						new StringReader(parentMessage.getBody()))) {

				StringBuilder newMessage = new StringBuilder(name);
				newMessage.append(" wrote:\n");
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isEmpty() && line.charAt(0) == '>') {
						newMessage.append('>');
					} else {
						newMessage.append("> ");
					}
					newMessage.append(line);
					newMessage.append('\n');
				}
				textArea.setText(newMessage.toString());
				textArea.end();
			} catch (IOException e) {
			}
		}


		return textArea;
	}

	private void  setFromList() {
		final Store store = FmsManager.getInstance().getStore();
		final int defaultId = Config.getInstance().getDefaultId();
		localIds = store.retrieveLocalIdentities();

		ObservableList<String> fromList = FXCollections.observableArrayList();
		for (Map.Entry<Integer, LocalIdentity> e : localIds.entrySet()) {
			final LocalIdentity id = e.getValue();
			if (defaultId == e.getKey()) {
				fromCb.setValue(id.getFullName());
			}
			fromList.add(id.getFullName());
		}

		fromCb.setItems(fromList);
	}

	private List<String> getBoards() {
		String[] boards = boardsTextField.getText().split(",");
		List<String> checkedBoards = new ArrayList<>();

		for (String board : boards) {
			String trimmed = board.trim();
			if (!trimmed.isEmpty()) {
				checkedBoards.add(trimmed);
			}
		}

		return checkedBoards;
	}
}
