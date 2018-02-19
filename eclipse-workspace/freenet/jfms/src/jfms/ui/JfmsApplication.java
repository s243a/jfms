package jfms.ui;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.fms.FmsManager;
import jfms.frost.FrostManager;

public class JfmsApplication extends Application {
	private static final Logger LOG = Logger.getLogger(JfmsApplication.class.getName());

	private final FmsManager fmsManager = FmsManager.getInstance();
	private final FrostManager frostManager = FrostManager.getInstance();
	private Stage primaryStage;
	private StatusBar statusBar;
	private NewsPane newsPane;

	@Override
	public void start(Stage stage) {
		this.primaryStage = stage;

		LOG.log(Level.FINEST, "Starting UI");

		try {
			if (!Config.getInstance().loadFromFile(Constants.JFMS_CONFIG_PATH)) {
				LOG.log(Level.FINEST, "Running wizard...");
				// jfms.properties does not exist
				// assume first run and start wizard
				ConfigWizard wizard = new ConfigWizard();
				wizard.run();

				// try initialization before saving configuration; otherwise
				// seed identites will be lost if store is not available
				fmsManager.initialize(wizard.getSettings().getSeedIdentities());

				Config.getInstance().saveToFile(Constants.JFMS_CONFIG_PATH, true);
			} else {
				fmsManager.initialize();
			}

			// FrostManager.getInstance().initialize();

			statusBar = new StatusBar();

			BorderPane borderPane = new BorderPane();
			borderPane.setTop(addMenuBar());
			borderPane.setCenter(addCenterPane());
			borderPane.setBottom(statusBar.getNode());


			Scene scene = new Scene(borderPane, 1024, 768);

			stage.setTitle("jfms");
			stage.setScene(scene);

			File file = new File("jfms.css");
			if (file.exists()) {
				scene.getStylesheets().add("jfms.css");
			}
			stage.show();



			fmsManager.setFcpStatusListener(statusBar);
			fmsManager.startBackgroundThread();

			statusBar.progressTitleProperty()
				.bind(fmsManager.getProgressTitleProperty());
			statusBar.progressProperty()
				.bind(fmsManager.getProgressProperty());
			statusBar.statusTextProperty()
				.bind(fmsManager.getStatusTextProperty());



			// frostManager.startBackgroundThread();
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, "Initialization failed", e);

			Dialog<ButtonType> dialog = new Alert(Alert.AlertType.ERROR);
			dialog.setTitle("jfms Initialization Failed");
			dialog.setHeaderText("Failed to inititalize SQLite Database");
			dialog.setContentText("Please check that you have a working SQLite "
					+ "JDBC driver\nin your classpath.\n"
					+ "Consult the log file for a detailed error message.");
			dialog.showAndWait();
		}
	}

	@Override
	public void stop() throws Exception {
		LOG.log(Level.FINEST, "stop called");
		super.stop();

		frostManager.shutdown();
		fmsManager.shutdown();
	}


	private Node addMenuBar() {
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("File");
		CheckMenuItem workOffline = new CheckMenuItem("Work Offline");
		workOffline.setSelected(fmsManager.isOffline());
		workOffline.setOnAction((ActionEvent t) -> {
			fmsManager.setOffline(workOffline.isSelected());
		});
		MenuItem quit = new MenuItem("Quit");
		quit.setOnAction((ActionEvent t) -> {
			Platform.exit();
		});
		fileMenu.getItems().addAll(workOffline, quit);

		Menu viewMenu = new Menu("View");
		CheckMenuItem threaded = new CheckMenuItem("Threaded");
		threaded.setSelected(true);
		threaded.setOnAction((ActionEvent t) -> {
			newsPane.setThreadedView(threaded.isSelected());
		});
		viewMenu.getItems().add(threaded);

		Menu messageMenu = new Menu("Message");
		MenuItem reply = new MenuItem("Reply");
		reply.setOnAction((ActionEvent t) -> {
			newsPane.handleReply();
		});

		MenuItem newMessage = new MenuItem("New Message");
		newMessage.setOnAction((ActionEvent t) -> {
			newsPane.handleNewMessage();
		});
		messageMenu.getItems().addAll(newMessage, reply);

		Menu boardsMenu = new Menu("Boards");
		MenuItem boardList = new MenuItem("Show Board List");
		boardList.setOnAction((ActionEvent t) -> {
			BoardPane boardPane = new BoardPane(newsPane);
			boardPane.show(primaryStage);
		});
		boardsMenu.getItems().addAll(boardList);

		MenuItem manageLocalIds = new MenuItem("Manage Local Identities...");
		manageLocalIds.setOnAction((ActionEvent t) -> {
			LocalIdentityPane localIdentityPane = new LocalIdentityPane();
			localIdentityPane.show(primaryStage);
		});

		MenuItem manageIds = new MenuItem("Manage Identities...");
		manageIds.setOnAction((ActionEvent t) -> {
			IdentityPane identityPane = new IdentityPane();
			identityPane.show(primaryStage);
		});

		MenuItem settings = new MenuItem("Settings...");
		settings.setOnAction((ActionEvent t) -> {
			SettingsDialog settingsDialog = new SettingsDialog(primaryStage);
			settingsDialog.showAndWait();
		});

		Menu identitiesMenu = new Menu("Options");
		identitiesMenu.getItems().addAll(manageLocalIds, manageIds,
				new SeparatorMenuItem(), settings);


		Menu helpMenu = new Menu("Help");
		MenuItem about = new MenuItem("About jfms");
		about.setOnAction((ActionEvent t) -> {
			final Image pdIcon = new Image(getClass()
					.getResourceAsStream("/icons/pd-icon.png"));
			Dialog<ButtonType> aboutDialog = new Alert(Alert.AlertType.INFORMATION);
			aboutDialog.setTitle("About jfms");
			aboutDialog.setGraphic(new ImageView(pdIcon));
			aboutDialog.setHeaderText("Version 0.0.6");
			TextArea info = new TextArea(
				"This is free and unencumbered software released into the public domain.\n"
				+ "\n"
				+ "JDBC driver: " + fmsManager.getStore().getInfo() + "\n"
				+ "(check your driver provider for license information)\n"
				+ "\n"
				+ "The Breeze Icon Theme (LGPL3+)\n"
				+ "Copyright (C) 2014 Uri Herrera <uri_herrera@nitrux.in> and others\n"
				+ "\n"

				+ "The Oxygen Icon Theme (LGPL3+)\n"
				+ "Copyright (C) 2007 Nuno Pinheiro <nuno@oxygen-icons.org>\n"
				+ "Copyright (C) 2007 David Vignoni <david@icon-king.com>\n"
				+ "Copyright (C) 2007 David Miller <miller@oxygen-icons.org>\n"
				+ "Copyright (C) 2007 Johann Ollivier Lapeyre <johann@oxygen-icons.org>\n"
				+ "Copyright (C) 2007 Kenneth Wimer <kwwii@bootsplash.org>\n"
				+ "Copyright (C) 2007 Riccardo Iaconelli <riccardo@oxygen-icons.org>"
				);
			info.setEditable(false);
			aboutDialog.getDialogPane().setContent(info);
			aboutDialog.showAndWait();
		});
		helpMenu.getItems().add(about);

		menuBar.getMenus().addAll(fileMenu, viewMenu, messageMenu,
				boardsMenu, identitiesMenu, helpMenu);

		return menuBar;
	}

	private Node addToolBar() {
		Button identitiesButton = new Button(null,
				new ImageView(Icons.getInstance().getIdentitiesIcon()));
		identitiesButton.setTooltip(new Tooltip("Manage Identities"));
		Utils.setToolBarButtonStyle(identitiesButton);
		identitiesButton.setOnAction((ActionEvent t) -> {
			IdentityPane identityPane = new IdentityPane();
			identityPane.show(primaryStage);
		});

		Button boardsButton = new Button(null,
				new ImageView(Icons.getInstance().getBoardsIcon()));
		boardsButton.setTooltip(new Tooltip("Show Board List"));
		Utils.setToolBarButtonStyle(boardsButton);
		boardsButton.setOnAction((ActionEvent t) -> {
			BoardPane boardPane = new BoardPane(newsPane);
			boardPane.show(primaryStage);
		});

		return new ToolBar(identitiesButton, boardsButton);
	}

	private Node addCenterPane() {
		VBox vbox = new VBox();
		newsPane = new NewsPane(primaryStage);
		vbox.getChildren().addAll(addToolBar(), newsPane.getNode());

		return vbox;
	}

}
