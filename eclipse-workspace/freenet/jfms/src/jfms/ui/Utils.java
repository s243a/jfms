package jfms.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;

class Utils {
	public static final String DEFAULT_STYLE = "-fx-background-color: transparent;";

	public static void setToolBarButtonStyle(Button button) {
		button.setStyle(DEFAULT_STYLE);
		button.setOnMouseEntered(e -> button.setStyle(null));
		button.setOnMouseExited(e -> button.setStyle(DEFAULT_STYLE));
		button.setFocusTraversable(false);
	}

	public static void setToolBarButtonStyle(ToggleButton button) {
		button.setStyle(DEFAULT_STYLE);
		button.setFocusTraversable(false);
		button.setOnMouseEntered(e -> button.setStyle(null));
		button.setOnMouseExited(e -> {
			if (button.isSelected()) {
				button.setStyle(null);
			} else {
				button.setStyle(DEFAULT_STYLE);
			}
		});
	}
};
