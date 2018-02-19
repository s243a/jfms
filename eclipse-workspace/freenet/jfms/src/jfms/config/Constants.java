package jfms.config;

import java.util.Arrays;
import java.util.List;

public class Constants {
	public static final String JFMS_CONFIG_PATH    = "jfms.properties";
	public static final String LOG_CONFIG_PATH     = "/logging.properties";

	// timings
	public static final int STARTUP_IDLE_TIME       =  30; // 30s
	public static final int ONLINE_CHECK_INTERVAL   =  60; //  1m
	public static final int INSERT_IDLE_TIME        = 300; //  5m
	public static final int DOWNLOAD_IDLE_TIME      = 900; // 15m

	public static final int TTL_TODAY              =   60; // 60m
	public static final int TTL_OLD                =  480; //  8h

	public static final int ADD_SEED_IDENTITY      =   -1;
	public static final int ADD_MANUALLY           =   -2;

	// default settings
	public static final String DEFAULT_FCP_HOST    = "127.0.0.1";
	public static final String DEFAULT_FCP_PORT    = "9481";
	public static final String DEFAULT_DEFAULT_ID  = "0";
	public static final String DEFAULT_MESSAGEBASE = "fms";
	public static final String DEFAULT_WEBVIEW     = "false";
	public static final String DEFAULT_ICON_SET    = "oxygen";
	public static final String DEFAULT_OFFLINE     = "false";
	public static final String DEFAULT_DL_MSGLISTS_WITHOUT_TRUST = "false";
	public static final String DEFAULT_DL_MESSAGES_WITHOUT_TRUST = "true";

	// fixed values
	public static final String DATABASE_URL = "jdbc:sqlite:jfms.db3";
	public static final String DATABASE_DRIVER = "org.sqlite.JDBC";
	public static final String DATABASE_USER = null;
	public static final String DATABASE_PASSWORD = null;

	public static final int MAX_FCP_REQUESTS = 5;
	public static final int MAX_INSERTS = 2;
	public static final int MAX_IDENTITY_AGE = 7;
	public static final int MAX_MESSAGE_AGE = 30;
	public static final int MIN_TRUSTLIST_TRUST = 50;
	public static final int MIN_PEER_MESSSAGE_TRUST = 30;
	public static final int MAX_INDEX = 9999;
	public static final int BOOTSTRAP_RATIO = 110;
	public static final int MAX_LOCAL_MESSAGELIST_COUNT = 50;
	public static final int MAX_MESSAGELIST_COUNT = 600;
	public static final int MAX_BOARD_LENGTH = 40;
	public static final int MAX_NAME_LENGTH = 40;
	public static final int MAX_TRUST_COMMENT_LENGTH = 250;
	public static final int MAX_REQUEST_SIZE = 1000000; // 1MB

	public static final List<String> getDefaultSeedIdentities() {
		return Arrays.asList(
				// SomeDude
				"SSK@NuBL7aaJ6Cn4fB7GXFb9Zfi8w1FhPyW3oKgU9TweZMw,iXez4j3qCpd596TxXiJgZyTq9o-CElEuJxm~jNNZAuA,AQACAAE/",
				// cptn_insano
				"SSK@bloE1LJ~qzSYUkU2nt7sB9kq060D4HTQC66pk5Q8NpA,DOOASUnp0kj6tOdhZJ-h5Tk7Ka50FSrUgsH7tCG1usU,AQACAAE/",
				// boardstat
				"SSK@aYWBb6zo2AM13XCNhsmmRKMANEx6PG~C15CWjdZziKA,X1pAG4EIqR1gAiyGFVZ1iiw-uTlh460~rFACJ7ZHQXk,AQACAAE/",
				// herb
				"SSK@5FeJUDg2ZdEqo-u4yoYWc1zF4tgPwOWlqcAJVGCoRv8,ptJ1y0YBkdU9S5DeYC8AsLH0SrmTE9S3w2HKZvl5QKo,AQACAAE/",
				// Tommy[D]
				"SSK@EefdujDZxdWxl0qusX0cJofGmJBvd3dF4Ty61PZy8Y8,4-LkBILohhpX7znBPXZWEUoK2qQZs-CLbUFO3-yKJIo,AQACAAE/",
				/// benjamin
				"SSK@y7xEHiGMGlivnCq-a8SpYU0YO-XRNI3LcJHB8tCeaXI,lRZOVc0pEHTEPqZUJqc5qRv6JDxHZzqc~ybEC~I2y~A,AQACAAE/",
				// Eye
				"SSK@vcQHxA8U6PxTbAxTAf65jc~sx4Tg3bWPf2ODLqR-SBg,P~Qf~geqmIk50ylnBav7OzmcFtmbr1YgNiuOuzge6Vc,AQACAAE/",
				// Mitosis
				"SSK@8~dscNP1TFUHWMZMZtpFJDrwg0rVePL6fB1S7uy4fTM,XWubHZK5Oizj0A9ovdob2wPeSmg4ikcduDMAAnvmkbw,AQACAAE/",
				// ZugaZandy
				"SSK@YoLiLuT0frl6DQb5b6Zz8CghW0ZC3P8xsBnEEE5puFE,6PiWr2ZGWqE5uSjEVJcNKz0NJF5xndr1TMRogR~RECQ,AQACAAE/",
				// Justus_Ranvier
				"SSK@JOKHnSe4cTWMCeQNSHr~-xqcYb2Tq0sVhDYPcklXhA8,p1bkPusgKdAD5pBdy3-ZvwgG-0WtBH4tIpgAYIu1oec,AQACAAE/"
			);
	}
}
