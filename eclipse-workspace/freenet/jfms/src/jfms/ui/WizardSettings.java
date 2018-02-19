package jfms.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jfms.config.Constants;
import jfms.fms.Validator;

public class WizardSettings {
	private static final Logger LOG = Logger.getLogger(WizardSettings.class.getName());

	private String fcpHost;
	private int fcpPort;
	private String messageBase;
	private List<String> seedIdentities;

	public WizardSettings() {
		fcpHost = Constants.DEFAULT_FCP_HOST;
		fcpPort = Integer.parseInt(Constants.DEFAULT_FCP_PORT);
		messageBase = Constants.DEFAULT_MESSAGEBASE;
		seedIdentities = Constants.getDefaultSeedIdentities();
	}

	public String getFcpHost() {
		return fcpHost;
	}

	public void setFcpHost(String fcpHost) {
		this.fcpHost = fcpHost;
	}

	public int getFcpPort() {
		return fcpPort;
	}

	public void setFcpPort(int fcpPort) {
		this.fcpPort = fcpPort;
	}

	public String getMessageBase() {
		return messageBase;
	}

	public void setMessageBase(String messageBase) {
		this.messageBase = messageBase;
	}

	public List<String> getSeedIdentities() {
		return seedIdentities;
	}

	public void setSeedIdentities(String seedIdentitiesText) {
		String[] ssks = seedIdentitiesText.split("\n");
		seedIdentities = new ArrayList<>();
		for (String ssk : ssks) {
			if (Validator.isValidSsk(ssk)) {
				seedIdentities.add(ssk);
			} else {
				LOG.log(Level.CONFIG, "Ignoring seed identity {0} (SSK invalid)",
						ssk);
			}
		}
	}

	public String getSeedIdentitiesAsText() {
		return seedIdentities.stream().collect(Collectors.joining("\n"));
	}
}
