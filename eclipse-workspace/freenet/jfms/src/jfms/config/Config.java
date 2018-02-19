package jfms.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Config {
	public static final int FCP_HOST    = 0;
	public static final int FCP_PORT    = 1;
	public static final int DEFAULT_ID  = 2;
	public static final int MESSAGEBASE = 3;
	public static final int WEBVIEW     = 4;
	public static final int ICON_SET    = 5;
	public static final int OFFLINE     = 6;
	public static final int DL_MSGLISTS_WITHOUT_TRUST = 7;
	public static final int DL_MESSAGES_WITHOUT_TRUST = 8;
	public static final int ID_SIZE     = 9;

	public static final int PORT_MAX = 65535;

	private static final Config instance = new Config();
	private static final Logger LOG = Logger.getLogger(Config.class.getName());

	private boolean dirty = false;
	private final ConfigEntry entries[];
	private final String[] values;

	private int fcpPort;
	private int defaultId;
	private boolean webViewEnabled;
	private boolean isOffline;
	private boolean dlMsgListsWithoutTrust;
	private boolean dlMessagesWithoutTrust;


	public static Config getInstance() {
		return instance;
	}

	private Config() {
		values = new String[ID_SIZE];
		entries= new ConfigEntry[ID_SIZE];

		// Freenet Settings
		entries[FCP_HOST] = new ConfigEntry(
					"fcp.host",
					ConfigType.STRING,
					"FCP Host",
					Constants.DEFAULT_FCP_HOST,
					"IP Address of your Freenet Node");
		entries[FCP_PORT] = new ConfigEntry(
					"fcp.port",
					ConfigType.INT,
					"FCP Port",
					Constants.DEFAULT_FCP_PORT,
					new IntRangeValidator(0, PORT_MAX),
					"FCP Port of your Freenet Node");

		// FMS Settings
		entries[DEFAULT_ID] = new ConfigEntry(
				"fms.default_id",
				ConfigType.INT,
				"Default Identity",
				Constants.DEFAULT_DEFAULT_ID,
				null);
		entries[MESSAGEBASE] = new ConfigEntry(
				"fms.messagebase",
				ConfigType.STRING,
				"Message Base",
				Constants.DEFAULT_MESSAGEBASE,
				"FMS Message Base");
		entries[DL_MSGLISTS_WITHOUT_TRUST] = new ConfigEntry(
				"fms.dl_msglists_without_trust",
				ConfigType.BOOLEAN,
				"Download message lists without trust",
				Constants.DEFAULT_DL_MSGLISTS_WITHOUT_TRUST,
				"Download messages lists from identities that appear in trust\n"
				+ "lists but have neither peer nor local trust level assigned");
		entries[DL_MESSAGES_WITHOUT_TRUST] = new ConfigEntry(
				"fms.dl_messages_without_trust",
				ConfigType.BOOLEAN,
				"Download messages without trust",
				Constants.DEFAULT_DL_MESSAGES_WITHOUT_TRUST,
				"Download messages from identities that appear in trust\n"
				+ "lists but have neither peer nor local trust level assigned");

		// UI Settings
		entries[ICON_SET] = new ConfigEntry(
				"ui.icon_set",
				ConfigType.CHOICE,
				"Icon Set",
				Constants.DEFAULT_ICON_SET,
				new ChoiceValidator(Arrays.asList(new String[] {
					"oxygen", "breeze" })),
				"Icons used in toolbars, etc. (requires restart)");
		entries[WEBVIEW] = new ConfigEntry(
				"ui.webview",
				ConfigType.BOOLEAN,
				"Enable WebView",
				Constants.DEFAULT_WEBVIEW,
				"Use HTML Engine (for color/emoticons support) for message " +
				"content (requires restart)");

		// configurable from Menu
		entries[OFFLINE] = new ConfigEntry(
				"fcp.offline",
				ConfigType.BOOLEAN,
				"Work offline",
				Constants.DEFAULT_OFFLINE,
				null);
	}

	public ConfigEntry getEntry(int id) {
		return entries[id];
	}

	public String getStringValue(int id) {
		return values[id];
	}

	public void setStringValue(int id, String value) {
		ConfigEntry entry = entries[id];
		String oldValue = values[id];

		if (!Objects.equals(value, oldValue)) {
			if (entry.validate(value)) {
				values[id] = value;
				update(id);
				dirty = true;
				LOG.log(Level.FINE,
						"Updated config value: {0} = {1}", new Object[]{
						entry.getName(), value});
			} else {
				LOG.log(Level.WARNING,
						"Invalid config value: {0} = {1}",
						new Object[]{entry.getName(), value});
			}
		}
	}

	public boolean getBooleanValue(int id) {
		return Boolean.parseBoolean(values[id]);
	}

	public int getIntValue(int id) {
		return Integer.parseInt(values[id]);
	}

	public String getFcpHost() {
		return values[FCP_HOST];
	}

	public int getFcpPort() {
		return fcpPort;
	}

	public String getMessageBase() {
		return values[MESSAGEBASE];
	}

	public int getDefaultId() {
		return defaultId;
	}

	public boolean getDownloadMsgListsWithoutTrust() {
		return dlMsgListsWithoutTrust;
	}

	public boolean getDownloadMessagesWithoutTrust() {
		return dlMessagesWithoutTrust;
	}

	public String getIconSet() {
		return values[ICON_SET];
	}

	public boolean getWebViewEnabled() {
		return webViewEnabled;
	}

	public boolean getOffline() {
		return isOffline;
	}

	public boolean loadFromFile(String filename) {
		try (FileInputStream in = new FileInputStream(filename)) {
			Properties properties = new Properties();
			properties.load(in);

			for (int id=0; id<ID_SIZE; id++) {
				ConfigEntry entry = entries[id];
				String value = properties.getProperty(entry.getPropertyName());
				if (value != null) {
					values[id] = value;
				}
			}

			LOG.log(Level.FINEST, "loaded config from {0}", filename);
			sanitizeConfig();

			return true;
		} catch (FileNotFoundException e) {
			LOG.log(Level.INFO, "config not found");
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to open config", e);
		}

		return false;
	}

	public void saveToFile(String filename, boolean force) {
		if (!dirty && !force) {
			return;
		}

		sanitizeConfig();

		Properties properties = new Properties();
		for (int id=0; id<ID_SIZE; id++) {
			if (values[id] != null) {
				properties.put(entries[id].getPropertyName(), values[id]);
			}
		}

		try (FileOutputStream out = new FileOutputStream(filename)) {
			properties.store(out, "JFMS Configuration File");
			dirty = false;
			LOG.log(Level.FINEST, "wrote config to {0}", filename);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to write config", e);
		}
	}

	public void saveToFile(String filename) {
		saveToFile(filename, false);
	}

	private void update(int id) {
		switch (id) {
		case FCP_PORT:
			fcpPort = getIntValue(FCP_PORT);
			break;
		case DEFAULT_ID:
			defaultId = getIntValue(DEFAULT_ID);
			break;
		case DL_MSGLISTS_WITHOUT_TRUST:
			dlMsgListsWithoutTrust = getBooleanValue(DL_MSGLISTS_WITHOUT_TRUST);
			break;
		case DL_MESSAGES_WITHOUT_TRUST:
			dlMessagesWithoutTrust = getBooleanValue(DL_MESSAGES_WITHOUT_TRUST);
			break;
		case WEBVIEW:
			webViewEnabled = getBooleanValue(WEBVIEW);
			break;
		case OFFLINE:
			isOffline = getBooleanValue(OFFLINE);
			break;
		default:
			// all other values are directly stored in values
			break;
		}
	}

	private void sanitizeConfig() {
		for (int id=0; id<ID_SIZE; id++) {
			final ConfigEntry entry = entries[id];

			final String value = values[id];
			if (value != null) {
				if (!entry.validate(value)) {
					LOG.log(Level.FINE, "Invalid value for {0}: {1}, "
					+ "falling back to default", new Object[]{
					entry.getName(), value});
					values[id] = entry.getDefaultValue();
				}
			} else {
				LOG.log(Level.FINEST, "Adding default value for {0}",
						entry.getName());
				values[id] = entry.getDefaultValue();
			}

			update(id);
		}
	}
}
