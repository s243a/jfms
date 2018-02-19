package jfms.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.fms.FmsManager;

public class BoardPane {
	private final TableView<BoardInfo> table = new TableView<>();
	private final VBox vBox = new VBox();
	private final Button subscribeButton = new Button();
	private final NewsPane newsPane;

	public BoardPane(NewsPane newsPane) {
		this.newsPane = newsPane;

		createBoardTable();
		createButtons();

		vBox.getChildren().addAll(table, subscribeButton);
	}

	class BoardSelectionChangeListener implements ChangeListener<BoardInfo> {
		@Override
		public void changed(ObservableValue<? extends BoardInfo> observable, BoardInfo oldValue, BoardInfo newValue) {
			if (newValue == null) {
				subscribeButton.setDisable(true);
			} else {
				subscribeButton.setDisable(false);
				if (newValue.getIsSubscribed()) {
					subscribeButton.setText("Unsubscribe");
				} else {
					subscribeButton.setText("Subscribe");
				}
			}
		}
	}

	public void show(Window ownerWindow) {
		Scene scene = new Scene(vBox);

		Stage stage = new Stage();
		stage.initOwner(ownerWindow);
		stage.setTitle("Boards");
		stage.setScene(scene);
		stage.show();
	}

	private void createBoardTable() {
		TableColumn<BoardInfo,String> nameColumn = new TableColumn<>("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameColumn.setSortType(TableColumn.SortType.ASCENDING);

		TableColumn<BoardInfo,String> messageCountColumn = new TableColumn<>("Messages");
		messageCountColumn.setCellValueFactory(new PropertyValueFactory<>("messageCount"));

		TableColumn<BoardInfo,String> subscribedColumn = new TableColumn<>("Subscribed");
		subscribedColumn.setCellValueFactory(new PropertyValueFactory<>("isSubscribed"));

		table.getColumns().addAll(Arrays.asList(nameColumn, messageCountColumn, subscribedColumn));
		table.setItems(getBoardInfoList());
		table.getSortOrder().add(nameColumn);

		table.getSelectionModel().selectedItemProperty()
			.addListener(new BoardSelectionChangeListener());
	}

	private void createButtons() {
		subscribeButton.setText("Subscribe");
		subscribeButton.setDisable(true);
		subscribeButton.setOnAction(e -> {
				final BoardInfo board = table.getSelectionModel().
					getSelectedItem();
				if (board.getIsSubscribed()) {
					newsPane.unsubscribeBoard(board.getName());
				} else {
					newsPane.subscribeBoard(board.getName());
				}
			});
	}

	private ObservableList<BoardInfo> getBoardInfoList() {
		ObservableList<BoardInfo> boardInfos = FXCollections.observableArrayList();

		jfms.fms.Store store = FmsManager.getInstance().getStore();
		List<String> subscribedBoards = store.getSubscribedBoardNames();

		for (Map.Entry<String,Integer> e : store.getBoardInfos().entrySet()) {
			final BoardInfo boardInfo = new BoardInfo();
			boardInfo.setName(e.getKey());
			boardInfo.setMessageCount(e.getValue());
			boardInfo.setIsSubscribed(subscribedBoards.contains(e.getKey()));
			boardInfos.add(boardInfo);
		}

		return boardInfos;
	}
}
