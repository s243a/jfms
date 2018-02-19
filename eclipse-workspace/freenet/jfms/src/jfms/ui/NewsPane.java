package jfms.ui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import jfms.config.Config;
import jfms.fms.FmsManager;
import jfms.frost.FrostManager;

public class NewsPane {
	private static final Logger LOG = Logger.getLogger(NewsPane.class.getName());

	private final Stage primaryStage;
	private Mode mode = Mode.NONE;
	private Mode previousMode = Mode.NONE;
	private final Node rootNode;
	private TreeItem<Message> messagesRoot;
	private boolean threadedView = true;

	private final FmsManager fmsManager = FmsManager.getInstance();
	private final FrostManager frostManager = FrostManager.getInstance();


	private TreeTableView<Message> treeTable;
	private TreeTableColumn<Message, String> defaultSortColumn;

	private MessageBodyView messageBody;

	private final Label subjectText = new Label();
	private final Label fromText = new Label();
	private final Label boardsText = new Label();
	private final Label dateText = new Label();
	private final Label messageIdText = new Label();
	private final Label messageTrustText = new Label();

	private String currentBoard;
	private TreeItem<Board> fmsFolder;


	public enum Mode {
		NONE,
		FMS,
		FROST
	}

	public NewsPane(Stage stage) {
		this.primaryStage = stage;

		SplitPane sp = new SplitPane();

		sp.getItems().addAll(createFolderPane(), createMessagePane());
		sp.setDividerPositions(0.2f);

		rootNode = sp;

		fmsManager.getMessageManager().setListener(new NewMessageListener());
	}


	private class MessageTableCellImpl extends TreeTableCell<Message, String> {
		private final boolean showIcon;

		public MessageTableCellImpl(boolean showIcon) {
			this.showIcon = showIcon;

			final ContextMenu menu = new ContextMenu();
			final MenuItem markUnreadMenuItem = new MenuItem("Mark as unread");
			menu.getItems().add(markUnreadMenuItem);

			markUnreadMenuItem.setOnAction((ActionEvent t) -> {
				final Message message = getSelectedMessage();
				if (message.getRead()) {
					message.setRead(false);
					redrawRow(message);

					jfms.fms.Store store = FmsManager.getInstance().getStore();
					store.setMessageRead(message.getStoreId(), false);

					for (String boardName : message.getBoardList()) {
						changeReadCount(boardName, 1);
					}
				}
			});

			setContextMenu(menu);
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			Message msg = getTreeTableRow().getItem();
			if (empty || msg == null) {
				setText("");
				setGraphic(null);
			} else {
				setText(item);

				if (msg.getRead()) {
					setFont(Font.font("SansSerif", FontWeight.NORMAL, 12));
				} else {
					setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
				}

				if (showIcon) {
					Image messageIcon;
					if (msg.getRead()) {
						messageIcon = Icons.getInstance()
							.getMessageIcon(Message.Status.READ);
					} else {
						messageIcon = Icons.getInstance()
							.getMessageIcon(Message.Status.UNREAD);
					}

					setGraphic(new ImageView(messageIcon));
				} else {
					setGraphic(null);
				}
			}
		}
	}

	private class BoardTreeCellImpl extends TreeCell<Board> {
		private final ContextMenu folderMenu = new ContextMenu();
		private final ContextMenu boardMenu = new ContextMenu();

		public BoardTreeCellImpl() {
			MenuItem addMenuItem = new MenuItem("Subscribe Board...");
			folderMenu.getItems().add(addMenuItem);
			addMenuItem.setOnAction((ActionEvent t) -> {
					Dialog<String> dialog = new TextInputDialog();
					dialog.setTitle("Subscribe Board");
					dialog.setGraphic(null);
					dialog.setHeaderText("Name: ");
					Optional<String> result = dialog.showAndWait();
					if (result.isPresent() && !result.get().isEmpty()) {
						subscribeBoard(result.get());
					}
			});


			MenuItem readMenuItem = new MenuItem("Mark Board Read");
			readMenuItem.setOnAction((ActionEvent t) -> {
					final String boardName = getTreeItem().getValue().getName();
					jfms.fms.BoardManager boardManager = fmsManager.getBoardManager();
					boardManager.setBoardMessagesRead(boardName, true);
					getTreeItem().setValue(new Board(boardName, 0));

					int row = 0;
					TreeItem<Message> msgItem;
					while ((msgItem = treeTable.getTreeItem(row)) != null) {
						final Message msg = msgItem.getValue();
						if (!msg.getRead()) {
							msg.setRead(true);
							redrawRow(msg);
						}

						final int boardCount = msg.getBoardList().size();
						if (boardCount > 1) {
							for (int i=1; i<boardCount; i++) {
								changeReadCount(msg.getBoardList().get(i), -1);
							}
						}

						row++;
					}
			});

			MenuItem deleteMenuItem = new MenuItem("Unsubscribe");
			deleteMenuItem.setOnAction((ActionEvent t) -> {
					unsubscribeBoard(getTreeItem().getValue().getName());
			});

			boardMenu.getItems().addAll(readMenuItem, deleteMenuItem);
		}

		@Override
		public void updateItem(Board item, boolean empty) {
			super.updateItem(item, empty);

			if (!empty && item != null) {
				final Board board = getItem();
				StringBuilder text = new StringBuilder(board.getName());
				if (!board.isFolder() && (board.getUnreadMessageCount() > 0)) {
					setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
					text.append(" (");
					text.append(board.getUnreadMessageCount());
					text.append(")");
				} else {
					setFont(Font.font("SansSerif", FontWeight.NORMAL, 12));
				}
				setText(text.toString());
				setGraphic(getTreeItem().getGraphic());

				final int level = treeTable.getTreeItemLevel(getTreeItem());
				switch (level) {
				case 0:
					break;
				case 1:
					setContextMenu(folderMenu);
					break;
				case 2:
					setContextMenu(boardMenu);
					break;
				default:
					LOG.log(Level.WARNING, "unexpected tree item level: {0}",
							level);
				}
			} else {
				setText(null);
				setGraphic(null);
			}
		}
	}

	private class MessageRowSelectionChangeListener implements ChangeListener<TreeItem<Message>> {
		@Override
		public void changed(ObservableValue<? extends TreeItem<Message>> observable, TreeItem<Message> oldValue, TreeItem<Message> newValue) {
			if (newValue != null) {
				if (mode == Mode.FROST) {
					Message message = newValue.getValue();
					LOG.log(Level.FINEST, "new frost row selected: {0}",
							message.getIndex());

					String body = FrostManager.getInstance().getStore()
						.getBody(currentBoard,
								message.getIndexDate(), message.getIndex());
					messageBody.setText(body, null);
				} else if (mode == Mode.FMS) {
					jfms.fms.Store store = FmsManager.getInstance().getStore();
					Message message = newValue.getValue();
					LOG.log(Level.FINEST, "new FMS row selected: {0}",
							message.getIndex());

					// TODO bind message to text fields?
					fromText.setText(message.getFrom());
					subjectText.setText(message.getSubject());
					dateText.setText(message.getDate());
					boardsText.setText(message.getBoards());
					messageIdText.setText(message.getMessageId());
					messageTrustText.setText(Integer.toString(message.getTrustLevel()));

					final int id = message.getStoreId();
					String body = store.getMessageBody(id);

					final jfms.fms.IdentityManager identityManager =
						FmsManager.getInstance().getIdentityManager();
					jfms.fms.Identity identity = identityManager
						.getIdentity(message.getIdentityId());
					messageBody.setText(body, identity.getSignature());


					if (!message.getRead()) {
						message.setRead(true);
						redrawRow(message);

						store.setMessageRead(id, true);

						for (String boardName : message.getBoardList()) {
							changeReadCount(boardName, -1);
						}
					}
				}
			} else {
				messageBody.setText("No message selected", null);
			}
		}
	}

	private class FolderSelectionChangeListener implements ChangeListener<TreeItem<Board>> {
		@Override
		public void changed(ObservableValue<? extends TreeItem<Board>> observable, TreeItem<Board> oldValue, TreeItem<Board> newValue) {

//			if (mode == Mode.FROST && currentBoard != null) {
//				jfms.frost.Board board = frostManager.getBoardManager()
//					.getBoard(currentBoard);
//			}

			mode = Mode.NONE;
			currentBoard = null;
			if (newValue != null) {
				TreeItem<Board> parentNode = newValue.getParent();
				if (parentNode != null) {
					switch (parentNode.getValue().getName()) {
					case "fms":
						mode = Mode.FMS;
						currentBoard = newValue.getValue().getName();
						updateMessagePane();
						break;
					case "frost":
						mode = Mode.FROST;
						currentBoard = newValue.getValue().getName();
						updateMessagePane();
						break;
					default:
						LOG.log(Level.FINEST, "Folder selected: {0}",
								newValue.getValue());
						updateMessagePane();
						break;
					}
				}
			} else {
				LOG.log(Level.FINEST, "Nothing selected");
				updateMessagePane();
			}
		}
	}

	private class NewMessageListener implements jfms.fms.MessageListener {
		@Override
		public void newMessage(jfms.fms.Message message) {
			for (String boardName: message.getBoards()) {
				if (boardName.equals(currentBoard)) {
					insertMessageIntoTree(createMessage(message),
							threadedView, messagesRoot.getChildren());
				}

				changeReadCount(boardName, 1);
			}
		}
	};

	public Node getNode() {
		return rootNode;
	}

	public void setThreadedView(boolean threaded) {
		threadedView = threaded;
		updateMessagePane();
	}

	public Message getSelectedMessage() {
		if (mode == Mode.FMS) {
			Message message = treeTable.getSelectionModel()
				.getSelectedItem().getValue();
			LOG.log(Level.FINEST, "selected message with ID={0}",
					message.getStoreId());
			return message;
		}

		return null;
	}

	public void handleNewMessage() {
		MessageWindow messageWindow = new MessageWindow();
		messageWindow.show(primaryStage);
	}

	public void handleReply() {
		jfms.fms.Store store = FmsManager.getInstance().getStore();
		Message message = getSelectedMessage();
		jfms.fms.Message fmsMessage = store.getMessage(
				message.getStoreId());
		MessageWindow messageWindow = new MessageWindow(fmsMessage);
		messageWindow.show(primaryStage);
	}

	public void redrawRow(Message message) {
		// workaround to force re-rendering of single row
		final String subject = message.getSubject();
		message.setSubject(null);
		message.setSubject(subject);

		final String from = message.getFrom();
		message.setFrom(null);
		message.setFrom(from);

		final String date = message.getDate();
		message.setDate(null);
		message.setDate(date);
	}

	private Node createFolderPane() {
		TreeItem<Board> boardListRoot = new TreeItem<>(new Board("Boards"));
		boardListRoot.setExpanded(true);
		TreeView<Board> treeView = new TreeView<>(boardListRoot);
		treeView.setCellFactory((TreeView<Board> p) -> new BoardTreeCellImpl());
		treeView.getSelectionModel().selectedItemProperty().addListener(
				new FolderSelectionChangeListener());

		// TreeItem<Board> frostFolder =
		// 	buildFrostBoardTree(frostManager.getBoardManager().getBoardList());
		// boardListRoot.getChildren().add(frostFolder);

		fmsFolder = new TreeItem<>(new Board("fms"));

		jfms.fms.BoardManager boardManager = fmsManager.getBoardManager();
		final Image folderIcon = Icons.getInstance().getBoardIcon();
		for (String boardName : boardManager.getSubscribedBoardNames()) {
			final int unread = boardManager.getUnreadMessageCount(boardName);
			TreeItem<Board> publicBoard = new TreeItem<>(
					new Board(boardName, unread),
					new ImageView(folderIcon));
			fmsFolder.getChildren().add(publicBoard);
		}

		fmsFolder.setExpanded(true);
		boardListRoot.getChildren().add(fmsFolder);

		return treeView;
	}

	private TreeItem<Board> buildFrostBoardTree(List<jfms.frost.Board> boards) {
		TreeItem<Board> frostFolder = new TreeItem<>(new Board("frost"));
		frostFolder.setExpanded(true);

		for (jfms.frost.Board b : boards) {
			frostFolder.getChildren().add(new TreeItem<>(new Board(b.getName(), 0)));
		}

		return frostFolder;
	}

	public void subscribeBoard(String boardName) {
		for (TreeItem<Board> b : fmsFolder.getChildren()) {
			if (b.getValue().getName().equals(boardName)) {
				LOG.log(Level.FINE, "Board {0} already subscribed", boardName);
				return;
			}
		}

		jfms.fms.BoardManager boardManager = fmsManager.getBoardManager();
		boardManager.subscribe(boardName);
		int unread = boardManager.getUnreadMessageCount(boardName);

		TreeItem<Board> newBoard = new TreeItem<>(
				new Board(boardName, unread),
				new ImageView(Icons.getInstance().getBoardIcon()));
		fmsFolder.getChildren().add(newBoard);
	}

	public void unsubscribeBoard(String boardName) {
		Iterator<TreeItem<Board>> iter = fmsFolder.getChildren().iterator();
		while (iter.hasNext()) {
			TreeItem<Board> board = iter.next();
			if (board.getValue().getName().equals(boardName)) {
				iter.remove();
				break;
			}
		}

		fmsManager.getBoardManager().unsubscribe(boardName);
	}

	private Node createMessagePane() {
		SplitPane sp = new SplitPane();
		sp.setOrientation(Orientation.VERTICAL);

		Button newMessageButton = new Button(null,
				new ImageView(Icons.getInstance().getNewMessageIcon()));
		newMessageButton.setTooltip(new Tooltip("New Message"));
		newMessageButton.setOnAction((ActionEvent t) -> handleNewMessage());
		Utils.setToolBarButtonStyle(newMessageButton);

		Button replyButton = new Button(null,
			new ImageView(Icons.getInstance().getReplyIcon()));
		replyButton.setTooltip(new Tooltip("Reply"));
		Utils.setToolBarButtonStyle(replyButton);
		replyButton.setOnAction((ActionEvent t) -> handleReply());

		ToggleButton showEmoticonsButton = new ToggleButton(null,
			new ImageView(Icons.getInstance().getEmoticonIcon()));
		Utils.setToolBarButtonStyle(showEmoticonsButton);
		showEmoticonsButton.setOnAction(e -> messageBody.setShowEmoticons(showEmoticonsButton.isSelected()));
		showEmoticonsButton.setTooltip(new Tooltip("Show graphic emoticons"));

		ToggleButton monospaceButton = new ToggleButton(null,
			new ImageView(Icons.getInstance().getFontIcon()));
		Utils.setToolBarButtonStyle(monospaceButton);
		monospaceButton.setOnAction(e -> messageBody.setUseMonospaceFont(monospaceButton.isSelected()));
		monospaceButton.setTooltip(new Tooltip("Use Monospace Font"));

		ToggleButton muteQuotesButton = new ToggleButton(null,
			new ImageView(Icons.getInstance().getMuteIcon()));
		Utils.setToolBarButtonStyle(muteQuotesButton);
		muteQuotesButton.setOnAction(e -> messageBody.setMuteQuotes(muteQuotesButton.isSelected()));
		muteQuotesButton.setTooltip(new Tooltip("Mute Quoted Text"));

		ToggleButton showSignatureButton = new ToggleButton(null,
			new ImageView(Icons.getInstance().getSignatureIcon()));
		Utils.setToolBarButtonStyle(showSignatureButton);
		showSignatureButton.setOnAction(e -> messageBody.setShowSignature(showSignatureButton.isSelected()));
		showSignatureButton.setTooltip(new Tooltip("Show Signature"));

		ToolBar toolBar = new ToolBar(newMessageButton, replyButton,
			new Separator(Orientation.VERTICAL),
			monospaceButton, showEmoticonsButton,
			muteQuotesButton, showSignatureButton);


		treeTable = createMessageTable();
		VBox treeTableVbox = new VBox();
		treeTableVbox.getChildren().addAll(toolBar, treeTable);

		VBox msgVbox = new VBox();
		if (Config.getInstance().getWebViewEnabled()) {
			messageBody = new MessageBodyWebView();
		} else {
			messageBody = new MessageBodyTextView();
		}
		messageBody.setText("No message to display", null);
		msgVbox.getChildren().addAll(createHeaders(), messageBody.getNode());

		sp.getItems().addAll(treeTableVbox, msgVbox);
		sp.setDividerPositions(0.4f);

		return sp;
	}

	private TreeTableView<Message> createMessageTable() {
		TreeTableView<Message> treeTableView = new TreeTableView<>();
		treeTableView.getColumns().setAll(getFmsColumns());
		treeTableView.getSelectionModel().selectedItemProperty()
			.addListener(new MessageRowSelectionChangeListener());

		messagesRoot = new TreeItem<>();
		treeTableView.setRoot(messagesRoot);
		treeTableView.setShowRoot(false);

		return treeTableView;
	}


	private List<TreeTableColumn<Message,?>> getFmsColumns() {
		TreeTableColumn<Message,String> subjectColumn = new TreeTableColumn<>("Subject");
		subjectColumn.setCellFactory((TreeTableColumn<Message, String> p) -> new MessageTableCellImpl(true));
		subjectColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("subject"));


		TreeTableColumn<Message,String> fromColumn = new TreeTableColumn<>("From");
		fromColumn.setCellFactory((TreeTableColumn<Message, String> p) -> new MessageTableCellImpl(false));
		fromColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("fromShort"));


		TreeTableColumn<Message,String> dateColumn = new TreeTableColumn<>("Date");
		dateColumn.setCellFactory((TreeTableColumn<Message, String> p) -> new MessageTableCellImpl(false));
		dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
		dateColumn.setSortType(TreeTableColumn.SortType.DESCENDING);

		defaultSortColumn = dateColumn;

		return Arrays.asList(subjectColumn, fromColumn, dateColumn);
	}

	private List<TreeTableColumn<Message,?>> getFrostColumns() {
		TreeTableColumn<Message,String> subjectColumn = new TreeTableColumn<>("Subject");
		subjectColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("subject"));

		TreeTableColumn<Message,String> fromColumn = new TreeTableColumn<>("From");
		fromColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("from"));

		TreeTableColumn<Message,String> dateColumn = new TreeTableColumn<>("Date");
		dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));

		TreeTableColumn<Message,Integer> indexColumn = new TreeTableColumn<>("Index");
		indexColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("index"));

		return Arrays.asList(subjectColumn, fromColumn, dateColumn, indexColumn);
	}

	private Node createHeaders() {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		Label fromLabel = new Label("From:");
		Label subjectLabel = new Label("Subject:");
		Label dateLabel = new Label("Date:");
		Label boardsLabel = new Label("Boards:");
		Label messageIdLabel = new Label("Message ID:");
		Label messageTrustLabel = new Label("Message Trust:");

		Font boldFont = Font.font("SansSerif", FontWeight.BOLD, 12);

		subjectLabel.setFont(boldFont);
		fromLabel.setFont(boldFont);
		dateLabel.setFont(boldFont);
		boardsLabel.setFont(boldFont);
		messageIdLabel.setFont(boldFont);
		messageTrustLabel.setFont(boldFont);

		grid.add(fromLabel, 0, 0);
		grid.add(fromText, 1, 0);
		grid.add(subjectLabel, 0, 1);
		grid.add(subjectText, 1, 1);
		grid.add(dateLabel, 0, 2);
		grid.add(dateText, 1, 2);
		grid.add(boardsLabel, 0, 3);
		grid.add(boardsText, 1, 3);
		grid.add(messageIdLabel, 0, 4);
		grid.add(messageIdText, 1, 4);
		grid.add(messageTrustLabel, 0, 5);
		grid.add(messageTrustText, 1, 5);

		return grid;
	}

	private void updateMessagePane() {
		if (mode == Mode.FROST) {
			LOG.log(Level.FINEST, "Frost board selected: {0}", currentBoard);

			if (previousMode != Mode.FROST) {
				treeTable.getColumns().setAll(getFrostColumns());
			}

			jfms.frost.Board board = frostManager
				.getBoardManager()
				.getBoard(currentBoard);

			createFrostMessageTree(board, threadedView);
		} else if (mode == Mode.FMS) {
			LOG.log(Level.FINEST, "FMS board selected: {0}", currentBoard);
			if (previousMode != Mode.FMS) {
				treeTable.getColumns().setAll(getFmsColumns());
			}
			createFmsMessageTree(currentBoard, threadedView);
		}

		previousMode = mode;
	}

	private Message createMessage(jfms.fms.Message fmsMessage) {
		String name = "not found";
		jfms.fms.Identity id = fmsManager
			.getStore()
			.retrieveIdentity(fmsMessage.getIdentityId());
		if (id != null) {
			name = id.getFullName();
		}

		Message message = new Message();
		message.setStoreId(fmsMessage.getMessageId());
		message.setIdentityId(fmsMessage.getIdentityId());
		message.setSubject(fmsMessage.getSubject());
		message.setFrom(name);

		StringBuilder str = new StringBuilder();
		str.append(fmsMessage.getDate()
				.format(DateTimeFormatter.ISO_LOCAL_DATE));
		str.append(" ");
		str.append(fmsMessage.getTime()
				.format(DateTimeFormatter.ISO_LOCAL_TIME));
		message.setDate(str.toString());

		message.setMessageId(fmsMessage.getMessageUuid());
		message.setReplyBoard(fmsMessage.getReplyBoard());
		message.setBoardList(fmsMessage.getBoards());
		message.setBoards();
		message.setRead(fmsMessage.getRead());
		message.setParentMessageId(fmsMessage.getParentId());
		if (id != null) {
			Integer trustLevel = fmsManager.getTrustManager()
				.getPeerMessageTrust(fmsMessage.getIdentityId());
			if (trustLevel != null) {
				message.setTrustLevel(trustLevel);
			}
		}

		return message;
	}

	private Message createMessage(jfms.frost.Message frostMessage) {
		Message message = new Message();
		message.setMessageId(frostMessage.getMessageId());
		message.setParentMessageId(frostMessage.getParentMessageId());
		message.setFrom(frostMessage.getFrom());
		message.setSubject(frostMessage.getSubject());
		message.setDate(frostMessage.getDate());

		message.setIndexDate(frostMessage.getSlotDate());
		message.setIndex(frostMessage.getIndex());

		return message;
	}

	private void createFrostMessageTree(jfms.frost.Board board, boolean threaded) {
		List<Message> boardMessages = new ArrayList<>();
		if (board != null) {
			for (jfms.frost.Message m : board.getMessages()) {
				boardMessages.add(createMessage(m));
			}
		}

		createMessageTree(boardMessages, threaded);
	}

	private void createFmsMessageTree(String boardName, boolean threaded) {
		List<Message> boardMessages = new ArrayList<>();

		for (jfms.fms.Message m : fmsManager.getStore().getMessagesForBoard(boardName)) {
			boardMessages.add(createMessage(m));
		}

		createMessageTree(boardMessages, threaded);
	}

	void insertMessageIntoTree(final Message m, boolean threaded,
			final List<TreeItem<Message>> rootNodes) {
		TreeItem<Message> newNode = new TreeItem<>(m);
		if (!threaded) {
			rootNodes.add(newNode);
			return;
		}

		boolean isRootNode = true;

		final String newMessageId = m.getMessageId();

		// check if we are parent of any existing nodes
		Iterator<TreeItem<Message>> iter = rootNodes.iterator();
		while (iter.hasNext()) {
			TreeItem<Message> rootNode = iter.next();
			final String rootNodeParentId = rootNode.getValue().getParentMessageId();

			if (rootNodeParentId != null && rootNodeParentId.equals(newMessageId)) {
				iter.remove();
				newNode.getChildren().add(rootNode);
				newNode.setExpanded(true);
			}
		}

		// check if any existing node is out parent
		final String newParentId = m.getParentMessageId();
		if (newParentId != null) {
			for (TreeItem<Message> rootNode : rootNodes) {
				TreeItem<Message> parent = findMessageNode(rootNode,
						newParentId);
				if (parent != null) {
					parent.getChildren().add(newNode);
					parent.setExpanded(true);
					isRootNode = false;
					break;
				}
			}
		}

		if (isRootNode) {
			rootNodes.add(newNode);
		}
	}

	TreeItem<Message> findMessageNode(TreeItem<Message> node, String messageId) {
		Message msg = node.getValue();
		if (msg != null && msg.getMessageId().equals(messageId)) {
			return node;
		}

		for (TreeItem<Message> child : node.getChildren()) {
			TreeItem<Message> requestedNode = findMessageNode(child, messageId);
			if (requestedNode != null) {
				return requestedNode;
			}
		}

		return null;
	}

	private void createMessageTree(List<Message> boardMessages, boolean threaded) {
		TreeItem<Message> newMessagesRoot = new TreeItem<>();
		List<TreeItem<Message>> rootNodes = new ArrayList<>();

		for (Message m : boardMessages) {
			insertMessageIntoTree(m, threaded, rootNodes);
		}

		LOG.log(Level.FINEST, "found {0} messages in {1} threads", new Object[]{boardMessages.size(), rootNodes.size()});

		newMessagesRoot.getChildren().addAll(rootNodes);

		treeTable.setRoot(newMessagesRoot);
		messagesRoot = newMessagesRoot;

		treeTable.getSortOrder().clear();
		treeTable.getSortOrder().add(defaultSortColumn);
	}

	private void changeReadCount(String boardName, int delta) {
		// TODO is there an easy way to avoid iterating over
		// all shown boards?
		for (TreeItem<Board> boardItem : fmsFolder.getChildren()) {
			if (boardItem.getValue().getName().equals(boardName)) {
				int count = boardItem.getValue().getUnreadMessageCount() + delta;
				if (count < 0) {
					LOG.log(Level.WARNING, "tried to set read count < 0");
					count = 0;
				}
				boardItem.setValue(new Board(boardName, count));
			}
		}
	}
}
