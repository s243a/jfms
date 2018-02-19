package jfms.fms;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RequestCache {
	private static final Logger LOG = Logger.getLogger(RequestCache.class.getName());

	private final Map<String, Instant> negativeCacheEntries = new HashMap<>();

	public void addNegativeCacheEntry(String key, Duration ttl) {
		final Instant expiry = Instant.now().plus(ttl);
		negativeCacheEntries.put(key, expiry);
	}

	public boolean isPresent(String key) {
		final Instant expiry = negativeCacheEntries.get(key);
		if (expiry == null) {
			return false;
		}

		if (Instant.now().isBefore(expiry)) {
			return true;
		} else {
			LOG.log(Level.FINEST, "removing expired negative cache entry");
			negativeCacheEntries.remove(key);
			return false;
		}
	}

	public void cleanup() {
		final Instant now = Instant.now();

		int removeCount = 0;

		for (Iterator<Map.Entry<String, Instant>> it=negativeCacheEntries.entrySet().iterator(); it.hasNext(); ) {
			if (it.next().getValue().isBefore(now)) {
				it.remove();
				removeCount++;
			}
		}

		LOG.log(Level.FINEST, "Removed {0} entries from cache; "
				+ "{1} entries kept", new Object[]{
				removeCount, negativeCacheEntries.size()});
	}
}
