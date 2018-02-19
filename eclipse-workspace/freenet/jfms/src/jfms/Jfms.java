package jfms;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javafx.application.Application;

import jfms.config.Constants;
import jfms.ui.JfmsApplication;

public class Jfms {
	private static final Logger LOG = Logger.getLogger(Jfms.class.getName());

	public static void main(String[] args) {
		try (InputStream is = Jfms.class.getResourceAsStream(Constants.LOG_CONFIG_PATH)) {
			if (is == null) {
				throw new IOException("Failed to open " + Constants.LOG_CONFIG_PATH);
			}
			LogManager.getLogManager().readConfiguration(is);
		} catch (IOException e) {
			System.err.println("Logging setup failed: " + e.getMessage());
		}

		try {
			Application.launch(JfmsApplication.class, args);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Fatal error", e);
		}
	}
}
