package jfms.fms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.fms.xml.IdentityExportParser;

public class IdentityManager {
	private static final Logger LOG = Logger.getLogger(IdentityManager.class.getName());

	private Map<String, Integer> sskToIdentityId;
	private Map<Integer, Identity> identities = new HashMap<>();

	public void initialize() {
		sskToIdentityId = FmsManager.getInstance().getStore().getSsks();
		identities = FmsManager.getInstance().getStore().getIdentities();
		LOG.log(Level.FINEST, "IdentityManager initialized with {0} identities",
				identities.size());
	}

	public int size() {
		return identities.size();
	}

	public Integer getIdentityId(String ssk) {
		return sskToIdentityId.get(ssk);
	}

	public void addIdentity(int identityId, Identity id) {
		identities.put(identityId, id);
		sskToIdentityId.put(id.getSsk(), identityId);
	}

	public boolean updateIdentity(int identityId, Identity newIdentity) {
		final Identity oldIdentity = identities.get(identityId);
		if (oldIdentity.equals(newIdentity)) {
			return false;
		}

		LOG.log(Level.FINEST, "updating Identity of {0}",
				oldIdentity.getFullName());

		Store store = FmsManager.getInstance().getStore();
		store.updateIdentity(identityId, newIdentity);

		identities.put(identityId, newIdentity);

		return true;
	}

	public String getSsk(int identityId) {
		Identity id = identities.get(identityId);
		if (id != null) {
			return id.getSsk();
		} else {
			return null;
		}
	}

	public Identity getIdentity(int identityId) {
		return identities.get(identityId);
	}

	public Map<Integer, Identity> getIdentities() {
		return identities;
	}

	public int importLocalIdentities(File file) {
		LOG.log(Level.FINEST, "Importing local identities from {0}", file);
		int importCount = 0;

		try (FileInputStream fis = new FileInputStream(file)) {
			Store store = FmsManager.getInstance().getStore();
			IdentityExportParser parser = new IdentityExportParser();
			List<LocalIdentity> localIdentities = parser.parse(fis);
			if (localIdentities == null) {
				LOG.log(Level.WARNING, "Failed to import identities from {0}",
						file);
				return importCount;
			}

			for (LocalIdentity identity : localIdentities) {
				identity.setIsActive(true);
				int identityId = store.insertLocalIdentity(identity);
				if (identityId > 0) {
					store.addSeedTrust(identityId);

					Config config = Config.getInstance();
					if (config.getDefaultId() <= 0) {
						config.setStringValue(Config.DEFAULT_ID,
							Integer.toString(identityId));
						config.saveToFile(Constants.JFMS_CONFIG_PATH);
					}

					importCount++;
				}
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to open file: {0}", e);
		}

		return importCount;
	}
}
