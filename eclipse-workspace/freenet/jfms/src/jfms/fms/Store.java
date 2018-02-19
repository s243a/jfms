package jfms.fms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jfms.config.Constants;

public class Store {
	private static final Logger LOG = Logger.getLogger(Store.class.getName());

	private final Connection connection;
	private String info;

	public static synchronized void logSqlException(String message, SQLException ex) {
		for (Throwable e : ex) {
			if (e instanceof SQLException) {
				LOG.log(Level.WARNING, message, e);
				LOG.log(Level.WARNING, "SQLState: {0}", ((SQLException)e).getSQLState());

				LOG.log(Level.WARNING, "Error Code: {0}", ((SQLException)e).getErrorCode());

				LOG.log(Level.WARNING, "Message: {0}", e.getMessage());

				Throwable t = ex.getCause();
				while (t != null) {
					LOG.log(Level.WARNING, "Cause", t);
					t = t.getCause();
				}
			}
		}
	}

	private static String getHistoryColumnName(Request.Type type, String basename) {
		StringBuilder str = new StringBuilder("last_");

		switch (type) {
		case IDENTITY:
			str.append("identity");
			break;
		case TRUST_LIST:
			str.append("trustlist");
			break;
		case MESSAGE_LIST:
			str.append("messagelist");
			break;
		default:
			throw new AssertionError("invalid type: " + type.name());
		}

		str.append('_');
		str.append(basename);

		return str.toString();
	}


	public Store() throws SQLException {
		Properties properties = new Properties();
		if (Constants.DATABASE_DRIVER != null) {
			try {
				Class.forName(Constants.DATABASE_DRIVER);
			} catch (ClassNotFoundException e) {
				LOG.log(Level.WARNING, "failed to load driver " + Constants.DATABASE_DRIVER, e);
			}
		}
		if (Constants.DATABASE_USER != null) {
			properties.put("user", Constants.DATABASE_USER);
		}
		if (Constants.DATABASE_PASSWORD != null) {
			properties.put("password", Constants.DATABASE_PASSWORD);
		}

		connection = DriverManager.getConnection(Constants.DATABASE_URL, properties);
	}

	public void initialize(List<String> seedIdentities) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();

		LOG.log(Level.INFO, "Using driver ''{0}'' version ''{1}''", new Object[]{
				metadata.getDriverName(), metadata.getDriverVersion()});
		LOG.log(Level.INFO, "Database is ''{0}'' version ''{1}''", new Object[]{
				metadata.getDatabaseProductName(),
				metadata.getDatabaseProductVersion()});

		info = metadata.getDriverName()
			+ " (" + metadata.getDriverVersion() + ')';

		createTables();
		if (seedIdentities != null) {
			addSeedIdentities(seedIdentities);
		}
		addSeedBoards();
	}

	public void close() throws SQLException {
		connection.close();
	}

	public String getInfo() {
		return info;
	}

	public synchronized Integer getLocalTrustListTrust(int localIdentityId, int identityId) {
		Integer trustListTrust = null;
		final String selectTrust = "SELECT trustlist_trust "
			+ "FROM local_trust "
			+ "WHERE local_identity_id=? AND identity_id=?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectTrust)) {
			pstmt.setInt(1, localIdentityId);
			pstmt.setInt(2, identityId);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				int trust = rs.getInt(1);
				if (!rs.wasNull()) {
					trustListTrust = trust;
				}
			}
		} catch (SQLException e) {
			logSqlException("select local_trust failed", e);
		}

		return trustListTrust;
	}

	public synchronized Integer getLocalMessageTrust(int localIdentityId, int identityId) {
		Integer messageTrust = null;
		final String selectTrust = "SELECT message_trust "
			+ "FROM local_trust "
			+ "WHERE local_identity_id=? AND identity_id=?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectTrust)) {
			pstmt.setInt(1, localIdentityId);
			pstmt.setInt(2, identityId);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				int trust = rs.getInt(1);
				if (!rs.wasNull()) {
					messageTrust = trust;
				}
			}
		} catch (SQLException e) {
			logSqlException("select local_trust failed", e);
		}

		return messageTrust;
	}

	/**
	 * Updates local trust.
	 * @param localIdentityId numeric ID of local identity
	 * @param identityId numeric ID of identity to change
	 * @param trust new trust values to apply.
	 *
	 * The identity of parameter trust is ignored. Instead the numeric
	 * identityId will be used.
	 */
	public synchronized void updateLocalTrust(Integer localIdentityId,
			Integer identityId, Trust trust) {
		final String updateTrust = "UPDATE local_trust "
			+ "SET trustlist_trust=?, message_trust=?, "
			+ "trustlist_trust_comment=?, message_trust_comment=? "
			+ "WHERE local_identity_id=? AND identity_id=?";
		final String insertTrust = "INSERT INTO local_trust"
			+ "(local_identity_id, identity_id, "
			+ "trustlist_trust, message_trust, "
			+ "trustlist_trust_comment, message_trust_comment) "
			+ "VALUES(?,?,?,?,?,?)";

		// TODO remove entry if all values are zero

		try {
			connection.setAutoCommit(false);

			int rowCount = 0;
			try (PreparedStatement pstmt = connection.prepareStatement(updateTrust)) {
				if (trust.getTrustListTrustLevel() >= 0) {
					pstmt.setInt(1, trust.getTrustListTrustLevel());
				} else {
					pstmt.setNull(1, Types.INTEGER);
				}
				if (trust.getMessageTrustLevel() >= 0) {
					pstmt.setInt(2, trust.getMessageTrustLevel());
				} else {
					pstmt.setNull(2, Types.INTEGER);
				}
				final String trustListComment = trust.getTrustListTrustComment();
				if (trustListComment != null && !trustListComment.isEmpty()) {
					pstmt.setString(3, trustListComment);
				} else {
					pstmt.setString(3, null);
				}
				final String msgComment = trust.getMessageTrustComment();
				if (msgComment != null && !msgComment.isEmpty()) {
					pstmt.setString(4, msgComment);
				} else {
					pstmt.setString(4, null);
				}
				pstmt.setInt(5, localIdentityId);
				pstmt.setInt(6, identityId);

				rowCount = pstmt.executeUpdate();
			}

			if (rowCount == 0 && (trust.getTrustListTrustLevel() >= 0 ||
						trust.getMessageTrustLevel() >= 0)) {
				try (PreparedStatement pstmt = connection.prepareStatement(insertTrust)) {
					pstmt.setInt(1, localIdentityId);
					pstmt.setInt(2, identityId);
					if (trust.getTrustListTrustLevel() >= 0) {
						pstmt.setInt(3, trust.getTrustListTrustLevel());
					} else {
						pstmt.setNull(3, Types.INTEGER);
					}
					if (trust.getMessageTrustLevel() >= 0) {
						pstmt.setInt(4, trust.getMessageTrustLevel());
					} else {
						pstmt.setNull(4, Types.INTEGER);
					}
					final String trustListComment = trust.getTrustListTrustComment();
					if (trustListComment != null && !trustListComment.isEmpty()) {
						pstmt.setString(5, trustListComment);
					} else {
						pstmt.setString(5, null);
					}
					final String msgComment = trust.getMessageTrustComment();
					if (msgComment != null && !msgComment.isEmpty()) {
						pstmt.setString(6, msgComment);
					} else {
						pstmt.setString(6, null);
					}

					pstmt.executeUpdate();
				}
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("update local_trust failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}
	}

	public synchronized Map<Integer,Integer> getLocalTrustListTrusts(
		int localIdentityId, int minTrust) {
		Map<Integer, Integer> trusts = new HashMap<>();

		final String selectTrusts = "SELECT identity_id, trustlist_trust "
			+ "FROM local_trust "
			+ "WHERE local_identity_id=? AND trustlist_trust>=?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectTrusts)) {
			pstmt.setInt(1, localIdentityId);
			pstmt.setInt(2, minTrust);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				int identityId = rs.getInt(1);
				int trustListTrust = rs.getInt(2);
				trusts.put(identityId, trustListTrust);
			}
		} catch (SQLException e) {
			logSqlException("select local trustlist trusts failed", e);
		}

		return trusts;
	}

	public synchronized Map<Integer,Integer> getLocalMessageTrusts(
		int localIdentityId) {

		Map<Integer, Integer> trusts = new HashMap<>();

		final String selectTrusts = "SELECT identity_id, message_trust "
			+ "FROM local_trust "
			+ "WHERE local_identity_id=? AND message_trust IS NOT NULL";
		try (PreparedStatement pstmt = connection.prepareStatement(selectTrusts)) {
			pstmt.setInt(1, localIdentityId);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				int identityId = rs.getInt(1);
				int messageTrust = rs.getInt(2);
				trusts.put(identityId, messageTrust);
			}
		} catch (SQLException e) {
			logSqlException("select local message trusts failed", e);
		}

		return trusts;
	}

	public synchronized int insertLocalIdentity(LocalIdentity identity) {
		final String selectIdentity = "SELECT COUNT(*) FROM local_identity "
			+ "WHERE ssk=?";
		final String insertIdentity = "INSERT INTO local_identity "
			+ "(ssk, private_ssk, name, signature, avatar, single_use, "
			+ "publish_trustlist, publish_boardlist, active) "
			+ "VALUES(?,?,?,?,?,?,?,?,?)";

		int identityId = -1;

		try {
			connection.setAutoCommit(false);

			try (PreparedStatement pstmt = connection.prepareStatement(selectIdentity)) {
				pstmt.setString(1, identity.getSsk());
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					int idCount = rs.getInt(1);
					if (idCount > 0) {
						LOG.log(Level.FINE, "identity already exists");
						return -1;
					}
				}
			}

			try (PreparedStatement pstmt = connection.prepareStatement(insertIdentity)) {
				pstmt.setString(1, identity.getSsk());
				pstmt.setString(2, identity.getPrivateSsk());
				pstmt.setString(3, identity.getName());
				pstmt.setString(4, identity.getSignature());
				pstmt.setString(5, identity.getAvatar());
				pstmt.setBoolean(6, identity.getSingleUse());
				pstmt.setBoolean(7, identity.getPublishTrustList());
				pstmt.setBoolean(8, identity.getPublishBoardList());
				pstmt.setBoolean(9, identity.getIsActive());

				pstmt.executeUpdate();

				ResultSet rs = pstmt.getGeneratedKeys();
				if (rs.next()) {
					identityId = rs.getInt(1);
				}
			}
			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("insert local identity failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}

		return identityId;
	}

	public synchronized void deleteLocalIdentity(int localIdentityId) {
		final String[] deleteQueries = new String[] {
			"DELETE FROM local_identity WHERE local_identity_id=?",
			"DELETE FROM local_identity_insert WHERE local_identity_id=?",
			"DELETE FROM local_message WHERE local_identity_id=?",
			"DELETE FROM local_trust WHERE local_identity_id=?",
			"DELETE FROM messagelist_insert WHERE local_identity_id=?"
		};

		try {
			connection.setAutoCommit(false);


			for (String q : deleteQueries) {
				try (PreparedStatement pstmt = connection.prepareStatement(q)) {
					pstmt.setInt(1, localIdentityId);
					pstmt.executeUpdate();
				}
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("delete local identity failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}
	}

	public synchronized void updateLocalIdentity(LocalIdentity localIdentity) {
		final String updateLocalIdentity = "UPDATE local_identity "
			+ "SET name=?, signature=?, avatar=?, publish_trustlist=?, "
			+ "active=? "
			+ "WHERE ssk=?";


		try (PreparedStatement pstmt = connection.prepareStatement(updateLocalIdentity)) {
			pstmt.setString(1, localIdentity.getName());
			pstmt.setString(2, localIdentity.getSignature());
			pstmt.setString(3, localIdentity.getAvatar());
			pstmt.setBoolean(4, localIdentity.getPublishTrustList());
			pstmt.setBoolean(5, localIdentity.getIsActive());
			pstmt.setString(6, localIdentity.getSsk());

			pstmt.executeUpdate();
		} catch (SQLException e) {
			logSqlException("update local identity failed", e);
		}
	}

	public synchronized Map<Integer, LocalIdentity> retrieveLocalIdentities() {
		final String selectIdentities = "SELECT local_identity_id, "
			+ "ssk, private_ssk, name, signature, avatar, single_use, "
			+ "publish_trustlist, publish_boardlist, active "
			+ "FROM local_identity";

		Map<Integer, LocalIdentity> identities = new HashMap<>();

		try (Statement stmt = connection.createStatement()) {
			ResultSet resultSet = stmt.executeQuery(selectIdentities);
			while (resultSet.next()) {
				LocalIdentity id = new LocalIdentity();
				int identityId = resultSet.getInt(1);
				id.setSsk(resultSet.getString(2));
				id.setPrivateSsk(resultSet.getString(3));
				id.setName(resultSet.getString(4));
				id.setSignature(resultSet.getString(5));
				id.setAvatar(resultSet.getString(6));
				id.setSingleUse(resultSet.getBoolean(7));
				id.setPublishTrustList(resultSet.getBoolean(8));
				id.setPublishBoardList(resultSet.getBoolean(9));
				id.setIsActive(resultSet.getBoolean(10));

				identities.put(identityId, id);
			}
		} catch (SQLException e) {
			logSqlException("failed to retrieve identities", e);
		}

		return identities;
	}

	public synchronized LocalIdentity retrieveLocalIdentity(int identityId) {
		final String selectIdentity = "SELECT "
			+ "ssk, private_ssk, name, signature, avatar, single_use, "
			+ "publish_trustlist, publish_boardlist, active "
			+ "FROM local_identity "
			+ "WHERE local_identity_id=?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectIdentity)) {
			pstmt.setInt(1, identityId);

			ResultSet resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				LocalIdentity id = new LocalIdentity();
				id.setSsk(resultSet.getString(1));
				id.setPrivateSsk(resultSet.getString(2));
				id.setName(resultSet.getString(3));
				id.setSignature(resultSet.getString(4));
				id.setAvatar(resultSet.getString(5));
				id.setSingleUse(resultSet.getBoolean(6));
				id.setPublishTrustList(resultSet.getBoolean(7));
				id.setPublishBoardList(resultSet.getBoolean(8));
				id.setIsActive(resultSet.getBoolean(9));

				return id;
			}
		} catch (SQLException e) {
			logSqlException("failed to retrieve identity " + identityId, e);
		}

		return null;
	}

/*
	public synchronized void insertIdentity(Identity identity) {
		final String insertIdentity = "INSERT INTO identity "
			+ "(ssk, name, signature, avatar, single_use, "
			+ "publish_trustlist, publish_boardlist, freesite_edition) "
			+ "VALUES(?,?,?,?,?,?,?,?,?)";

		try (PreparedStatement pstmt = connection.prepareStatement(insertIdentity)) {
			pstmt.setString(1, identity.getSsk());
			pstmt.setString(2, identity.getName());
			pstmt.setString(3, identity.getSignature());
			pstmt.setString(4, identity.getAvatar());
			pstmt.setBoolean(5, identity.getSingleUse());
			pstmt.setBoolean(6, identity.getPublishTrustList());
			pstmt.setBoolean(7, identity.getPublishBoardList());

			final int freesiteEdition = identity.getFreesiteEdition();
			if (freesiteEdition >= 0) {
				pstmt.setInt(8, freesiteEdition);
			} else {
				pstmt.setNull(8, Types.INTEGER);
			}

			pstmt.executeUpdate();
		} catch (SQLException e) {
			logSqlException("failed to insert identity " + identity.getSsk(), e);
		}
	}
*/

	public synchronized void updateIdentity(int identityId, Identity identity) {
		final String updateIdentity = "UPDATE identity SET "
			+ "name=?, "
			+ "signature=?, "
			+ "avatar=?, "
			+ "single_use=?, "
			+ "publish_trustlist=?, "
			+ "publish_boardlist=?, "
			+ "freesite_edition=? "
			+ "WHERE identity_id=?";

		try (PreparedStatement pstmt = connection.prepareStatement(updateIdentity)) {
			pstmt.setString(1, identity.getName());
			pstmt.setString(2, identity.getSignature());
			pstmt.setString(3, identity.getAvatar());
			pstmt.setBoolean(4, identity.getSingleUse());
			pstmt.setBoolean(5, identity.getPublishTrustList());
			pstmt.setBoolean(6, identity.getPublishBoardList());

			final int freesiteEdition = identity.getFreesiteEdition();
			if (freesiteEdition >= 0) {
				pstmt.setInt(7, freesiteEdition);
			} else {
				pstmt.setNull(7, Types.INTEGER);
			}

			pstmt.setInt(8, identityId);

			int rows = pstmt.executeUpdate();
			if (rows != 1) {
				LOG.log(Level.WARNING, "identity update affected {0} rows",
						rows);
			}
		} catch (SQLException e) {
			logSqlException("failed to insert identity " + identity.getSsk(), e);
		}
	}

	public synchronized boolean messageExists(int identityId,
			LocalDate insertDate, int insertIndex) {
		boolean exists = false;
		final String selectMessage = "SELECT COUNT(*) "
			+ "FROM message "
			+ "WHERE identity_id=? AND insert_date=? AND insert_index=?";
		final String dateStr = insertDate.
				format(DateTimeFormatter.ISO_LOCAL_DATE);

		try {
			try (PreparedStatement pstmt = connection.prepareStatement(selectMessage)) {
				pstmt.setInt(1, identityId);
				pstmt.setString(2, dateStr);
				pstmt.setInt(3, insertIndex);

				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					if (rs.getInt(1) > 0) {
						exists = true;
					}
				}
			}
		} catch (SQLException e) {
			try {
				logSqlException("message exists check failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		}

		return exists;
	}

	public synchronized int insertMessage(Message message) {
		final String insertMessage = "INSERT INTO message "
		+ "(identity_id, date, time, subject, message_uuid, reply_board_id, "
		+ "insert_date, insert_index, body) "
		+ "VALUES(?,?,?,?,?,?,?,?,?)";

		final String insertDateStr = message.getInsertDate()
				.format(DateTimeFormatter.ISO_LOCAL_DATE);
		final String dateStr = message.getDate()
				.format(DateTimeFormatter.ISO_LOCAL_DATE);
		final String timeStr = message.getTime()
				.format(DateTimeFormatter.ISO_LOCAL_TIME);

		int messageId = -1;
		try {
			connection.setAutoCommit(false);

			BoardManager boardManager = FmsManager.getInstance().getBoardManager();
			Integer replyBoardId = boardManager.getBoardId(message.getReplyBoard());
			if (replyBoardId == null) {
				replyBoardId = addBoard(message.getReplyBoard());
				// TODO don't add on SQLException
				boardManager.addBoard(replyBoardId, message.getReplyBoard());
			}

			try (PreparedStatement pstmt = connection.prepareStatement(insertMessage)) {
				pstmt.setInt(1, message.getIdentityId());
				pstmt.setString(2, dateStr);
				pstmt.setString(3, timeStr);
				pstmt.setString(4, message.getSubject());
				pstmt.setString(5, message.getMessageUuid());
				pstmt.setInt(6, replyBoardId);
				pstmt.setString(7, insertDateStr);
				pstmt.setInt(8, message.getInsertIndex());
				pstmt.setString(9, message.getBody());

				pstmt.executeUpdate();

				ResultSet rs = pstmt.getGeneratedKeys();
				if (rs.next()) {
					messageId = rs.getInt(1);
				}
			}

			if (messageId == -1) {
				throw new SQLException("failed to get message ID");
			}

			final String insertMessageBoard = "INSERT INTO board_message VALUES(?,?)";
			final List<String> boards = message.getBoards();


			final List<Integer> boardIds = new ArrayList<>();
			if (boards != null) {
				for (String b : boards) {
					Integer boardId = boardManager.getBoardId(b);
					if (boardId == null) {
						boardId = addBoard(b);
						// TODO don't add on SQLException
						boardManager.addBoard(boardId, b);
					}
					boardIds.add(boardId);
				}
			}

			if (boardIds.size() > 0) {
				try (PreparedStatement pstmt =
						connection.prepareStatement(insertMessageBoard)) {
					for (Integer boardId : boardIds) {
						pstmt.setInt(1, boardId);
						pstmt.setInt(2, messageId);
						pstmt.addBatch();
					}
					pstmt.executeBatch();
				}
			}

			final InReplyTo inReplyTo = message.getInReplyTo();
			if (inReplyTo != null /*&& !inReplyTo.isEmpty()*/) {
				final String insertReplyTo = "INSERT INTO message_reply_to "
					+ "VALUES(?,?,?)";
				try (PreparedStatement pstmt =
						connection.prepareStatement(insertReplyTo)) {
					pstmt.setInt(1, messageId);
					for (Map.Entry<Integer, String> e : inReplyTo.getMessages().entrySet()) {
						pstmt.setInt(2, e.getKey());
						pstmt.setString(3, e.getValue());
						pstmt.addBatch();
					}
					pstmt.executeBatch();
				}
			}

			final List<Attachment> attachments = message.getAttachments();
			if (attachments != null) {
				final String insertAttachment = "INSERT INTO attachment "
					+ "VALUES(?,?,?)";
				try (PreparedStatement pstmt =
						connection.prepareStatement(insertAttachment)) {
					for (Attachment a : attachments) {
						pstmt.setInt(1, messageId);
						pstmt.setString(2, a.getKey());
						pstmt.setInt(3, a.getSize());
						pstmt.addBatch();
					}
					pstmt.executeBatch();
				}
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("insert message failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("setting autocommit failed", ex);
			}
		}

		return messageId;
	}

	public synchronized void insertTrustList(int trusterId, List<Trust> trustList) {
		LOG.log(Level.FINEST, "Processing trust list of ID {0}", trusterId);

		try {
			connection.setAutoCommit(false);

			List<Trust> currentTrustList = getTrustList(trusterId);

			Map<String, Trust> currentTrustMap = currentTrustList
				.stream()
				.collect(Collectors.toMap(t -> t.getIdentity(), t->t));

			List<Trust> newTrusts = new ArrayList<>();
			List<Trust> changedTrusts = new ArrayList<>();
			Set<String> removedTrusts = currentTrustList
				.stream()
				.map(t -> t.getIdentity())
				.collect(Collectors.toSet());


			for (Trust t : trustList) {
				addIdentityIfNotExists(trusterId, t.getIdentity());

				// don't store empty trust entries
				if (t.getTrustListTrustLevel() < 0 && t.getMessageTrustLevel() < 0) {
					continue;
				}

				Trust currentTrust = currentTrustMap.get(t.getIdentity());
				if (currentTrust != null) {
					if (!currentTrust.equals(t)) {
						changedTrusts.add(t);
					}
				} else {
					newTrusts.add(t);
				}
				removedTrusts.remove(t.getIdentity());
			}

			removePeerTrustEntries(trusterId, removedTrusts);
			addPeerTrustEntries(trusterId, newTrusts);
			updatePeerTrustEntries(trusterId, changedTrusts);

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("insert trustlist failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("setting autocommit failed", ex);
			}
		}
	}

	public synchronized void setBoardSubscribed(int boardId, boolean subscribed) {
		final String updateBoard = "UPDATE board "
			+ "SET subscribed=? "
			+ "WHERE board_id=?";

		try {
			try (PreparedStatement pstmt = connection.prepareStatement(updateBoard)) {
				pstmt.setBoolean(1, subscribed);
				pstmt.setInt(2, boardId);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			logSqlException("update board failed", e);
		}
	}


	public synchronized Identity retrieveIdentity(int identityId) {
		final String selectIdentity = "SELECT "
			+ "ssk, name, signature, avatar, single_use, "
			+ "publish_trustlist, publish_boardlist, freesite_edition "
			+ "FROM identity "
			+ "WHERE identity_id=?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectIdentity)) {
			pstmt.setInt(1, identityId);

			ResultSet resultSet = pstmt.executeQuery();
			if (resultSet.next()) {
				Identity id = new Identity();
				id.setSsk(resultSet.getString(1));
				id.setName(resultSet.getString(2));
				id.setSignature(resultSet.getString(3));
				id.setAvatar(resultSet.getString(4));
				id.setSingleUse(resultSet.getBoolean(5));
				id.setPublishTrustList(resultSet.getBoolean(6));
				id.setPublishBoardList(resultSet.getBoolean(7));

				int freesiteEdition = resultSet.getInt(8);
				if (resultSet.wasNull()) {
					freesiteEdition = -1;
				}
				id.setFreesiteEdition(freesiteEdition);

				return id;
			}
		} catch (SQLException e) {
			logSqlException("failed to retrieve identity " + identityId, e);
		}

		return null;
	}

	public synchronized Map<Integer, Identity> getIdentities() {
		Map<Integer, Identity> identities = new HashMap<>();

		final String selectIdentities = "SELECT "
			+ "identity_id, ssk, name, signature, avatar, single_use, "
			+ "publish_trustlist, publish_boardlist, freesite_edition "
			+ "FROM identity";
		try (Statement stmt = connection.createStatement()) {
			ResultSet rs = stmt.executeQuery(selectIdentities);
			while (rs.next()) {
				Identity id = new Identity();
				int identityId = rs.getInt(1);
				id.setSsk(rs.getString(2));
				id.setName(rs.getString(3));

				id.setSignature(rs.getString(4));
				id.setAvatar(rs.getString(5));
				id.setSingleUse(rs.getBoolean(6));
				id.setPublishTrustList(rs.getBoolean(7));
				id.setPublishBoardList(rs.getBoolean(8));

				int freesite_edition = rs.getInt(9);
				if (rs.wasNull()) {
					freesite_edition = -1;
				}
				id.setFreesiteEdition(freesite_edition);

				identities.put(identityId, id);
			}
		} catch (SQLException e) {
			logSqlException("getting identities failed", e);
		}

		return identities;
	}

	public synchronized Map<String, Integer> getSsks() {
		Map<String,Integer> ssks = new HashMap<>();

		final String selectIds = "SELECT identity_id, ssk "
			+ "FROM identity";
		try (PreparedStatement pstmt = connection.prepareStatement(selectIds)) {
			ResultSet results = pstmt.executeQuery();
			while (results.next()) {
				int identityId = results.getInt(1);
				String ssk = results.getString(2);
				ssks.put(ssk, identityId);
			}
		} catch (SQLException e) {
			logSqlException("getting identities failed", e);
		}

		return ssks;
	}

	public synchronized List<Trust> getTrustList(int identityId) {
		try {
			return getTrustListInternal(identityId);
		} catch (SQLException e) {
			logSqlException("select trusts failed", e);
			return Collections.emptyList();
		}
	}

	public synchronized void addLocalMessage(int localIdentityId,
			String messageXml, LocalDate date) {
		final String selectLocalMessage = "SELECT MAX(insert_index) "
			+ "FROM local_message "
			+ "WHERE local_identity_id=? AND insert_date=?";
		final String insertLocalMessage = "INSERT INTO local_message "
			+ "(local_identity_id, insert_date, insert_index, message_xml) "
			+ "VALUES(?,?,?,?)";

		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try {
			connection.setAutoCommit(false);

			int maxIndex = -1;
			try (PreparedStatement pstmt = connection.prepareStatement(selectLocalMessage)) {
				pstmt.setInt(1, localIdentityId);
				pstmt.setString(2, dateStr);

				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					maxIndex = rs.getInt(1);
					if (rs.wasNull()) {
						maxIndex = -1;
					}
				}
			}

			try (PreparedStatement pstmt = connection.prepareStatement(insertLocalMessage)) {
				pstmt.setInt(1, localIdentityId);
				pstmt.setString(2, dateStr);
				pstmt.setInt(3, maxIndex + 1);
				pstmt.setString(4, messageXml);

				pstmt.executeUpdate();
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("insert local_message failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}
	}

	public synchronized void updateLocalMessageIndex(int localIdentityId,
			LocalDate date, int index) {
		final String updateLocalMessage = "UPDATE local_message "
			+ "SET insert_index=("
				+ "SELECT MAX(insert_index)+1 "
				+ "FROM local_message "
				+ "WHERE local_identity_id=? AND insert_date=?) "
			+ "WHERE local_identity_id=? AND insert_date=? AND insert_index=?";

		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try {
			try (PreparedStatement pstmt = connection.prepareStatement(updateLocalMessage)) {
				pstmt.setInt(1, localIdentityId);
				pstmt.setString(2, dateStr);
				pstmt.setInt(3, localIdentityId);
				pstmt.setString(4, dateStr);
				pstmt.setInt(5, index);

				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			logSqlException("update local_message failed", e);
		}
	}

	public synchronized void setLocalMessageInserted(int localIdentityId,
		LocalDate date, int index, boolean inserted) {
		final String updateLocalMessage = "UPDATE local_message " +
			"SET inserted=? " +
			"WHERE local_identity_id=? AND insert_date=? AND insert_index=?";

		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try (PreparedStatement pstmt = connection.prepareStatement(updateLocalMessage)) {
			pstmt.setBoolean(1, inserted);
			pstmt.setInt(2, localIdentityId);
			pstmt.setString(3, dateStr);
			pstmt.setInt(4, index);

			pstmt.executeUpdate();
		} catch (SQLException e) {
			logSqlException("update local_message failed", e);
		}
	}

	public synchronized List<MessageReference> getLocalMessageList(
			int localIdentityId, boolean selectNotInsertedOnly, int limit) {
		StringBuilder str = new StringBuilder();

		// XXX probably we shouldn't include messages not yet inserted
		// in message list
		str.append("SELECT insert_date, insert_index ");
		str.append("FROM local_message ");
		str.append("WHERE local_identity_id=? ");

		if (selectNotInsertedOnly) {
			str.append("AND inserted = 0 ");
		}
		str.append("ORDER BY insert_date DESC, insert_index DESC ");
		if (limit >= 0) {
			str.append("LIMIT ?");
		}
		final String selectMessages = str.toString();

		List<MessageReference> messages = new ArrayList<>();
		try (PreparedStatement pstmt = connection.prepareStatement(selectMessages)) {
			pstmt.setInt(1, localIdentityId);
			if (limit >= 0) {
				pstmt.setInt(2, limit);
			}

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				MessageReference msgRef = new MessageReference();
				final LocalDate date = LocalDate.parse(rs.getString(1),
						DateTimeFormatter.ISO_LOCAL_DATE);
				msgRef.setDate(date);
				msgRef.setIndex(rs.getInt(2));

				messages.add(msgRef);
			}
		} catch (SQLException e) {
			logSqlException("getting messages failed", e);
		}

		return messages;
	}

	public synchronized String getLocalMessage(
			int localIdentityId, LocalDate insertDate, int insertIndex) {
		final String selectMessage = "SELECT message_xml "
			+ "FROM local_message "
			+ "WHERE local_identity_id=? AND insert_date=? AND insert_index=?";
		final String dateStr = insertDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try (PreparedStatement pstmt =
				connection.prepareStatement(selectMessage)) {
			pstmt.setInt(1, localIdentityId);
			pstmt.setString(2, dateStr);
			pstmt.setInt(3, insertIndex);

			ResultSet rs = pstmt.executeQuery();
			String messageXml = null;
			if (rs.next()) {
				messageXml = rs.getString(1);
			}

			if (messageXml == null) {
				LOG.log(Level.WARNING, "local message not found");
			}

			return messageXml;
		} catch (SQLException e) {
			logSqlException("getting message failed", e);
		}

		return null;
	}

	public synchronized void updateRequestHistory(Integer identityId,
			Request.Type type, LocalDate date, int index) {

		final String updateHistory = "UPDATE request_history "
			+ "SET " + getHistoryColumnName(type, "date") + "=?, "
			+ getHistoryColumnName(type, "index") + "=? "
			+ "WHERE identity_id=?";
		final String insertHistory = "INSERT INTO request_history "
			+ "(identity_id, " + getHistoryColumnName(type, "date") + ", "
			+ getHistoryColumnName(type, "index") + ") "
			+ "VALUES(?,?,?) ";

		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try {
			connection.setAutoCommit(false);

			int rowCount = 0;
			try (PreparedStatement pstmt = connection.prepareStatement(updateHistory)) {
				pstmt.setString(1, dateStr);
				pstmt.setInt(2, index);
				pstmt.setInt(3, identityId);
				rowCount = pstmt.executeUpdate();
			}

			if (rowCount == 0) {
				try (PreparedStatement pstmt = connection.prepareStatement(insertHistory)) {
					pstmt.setInt(1, identityId);
					pstmt.setString(2, dateStr);
					pstmt.setInt(3, index);
					pstmt.executeUpdate();
				}
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("update request_history failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}
	}

	public synchronized int getLocalIdentityInsertIndex(
			int localIdentityId, LocalDate date) {
		final String selectLocalIdentityInsert = "SELECT MAX(insert_index) "
			+ "FROM local_identity_insert "
			+ "WHERE local_identity_id=? AND insert_date=?";
		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		int maxIndex = -1;
		try {
			try (PreparedStatement pstmt = connection.prepareStatement(selectLocalIdentityInsert)) {
				pstmt.setInt(1, localIdentityId);
				pstmt.setString(2, dateStr);

				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					maxIndex = rs.getInt(1);
					if (rs.wasNull()) {
						maxIndex = -1;
					}
				}
			}
		} catch (SQLException e) {
			logSqlException("select local_identity_insert failed", e);
		}

		return maxIndex;
	}

	public synchronized void updateLocalIdentityInsert(
			Integer localIdentityId, LocalDate date, int index) {
		final String updateIdentity = "UPDATE local_identity_insert "
			+ "SET insert_date=?, insert_index=? "
			+ "WHERE local_identity_id=?";
		final String insertIdentity = "INSERT INTO local_identity_insert "
			+ "(local_identity_id, insert_date, insert_index) "
			+ "VALUES(?,?,?)";
		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try {
			connection.setAutoCommit(false);

			int rowCount = 0;
			try (PreparedStatement pstmt = connection.prepareStatement(updateIdentity)) {
				pstmt.setString(1, dateStr);
				pstmt.setInt(2, index);
				pstmt.setInt(3, localIdentityId);
				rowCount = pstmt.executeUpdate();
			}

			if (rowCount == 0) {
				try (PreparedStatement pstmt = connection.prepareStatement(insertIdentity)) {
					pstmt.setInt(1, localIdentityId);
					pstmt.setString(2, dateStr);
					pstmt.setInt(3, index);
					pstmt.executeUpdate();
				}
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("update local_identity_insert failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}
	}

	public synchronized int getMessageListInsertIndex(
			int localIdentityId, LocalDate date, boolean selectNotInsertedOnly) {
		StringBuilder str = new StringBuilder();
		str.append("SELECT MAX(insert_index) ");
		str.append("FROM messagelist_insert ");
		str.append("WHERE local_identity_id=? AND insert_date=?");

		if (selectNotInsertedOnly) {
			str.append(" AND inserted = 0");
		}

		final String selectMessageListInsert = str.toString();
		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		int maxIndex = -1;
		try {
			try (PreparedStatement pstmt = connection.prepareStatement(selectMessageListInsert)) {
				pstmt.setInt(1, localIdentityId);
				pstmt.setString(2, dateStr);

				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					maxIndex = rs.getInt(1);
					if (rs.wasNull()) {
						maxIndex = -1;
					}
				}
			}
		} catch (SQLException e) {
			logSqlException("select messagelist_insert failed", e);
		}

		return maxIndex;
	}

	public synchronized void updateMessageListInsert(Integer localIdentityId,
			LocalDate date, int index, boolean inserted) {
		final String updateMessageList = "UPDATE messagelist_insert "
			+ "SET insert_date=?, insert_index=?, inserted=?"
			+ "WHERE local_identity_id=?";
		final String insertMessageList = "INSERT INTO messagelist_insert "
			+ "(local_identity_id, insert_date, insert_index, inserted) "
			+ "VALUES(?,?,?,?)";
		final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try {
			connection.setAutoCommit(false);

			int rowCount = 0;
			try (PreparedStatement pstmt = connection.prepareStatement(updateMessageList)) {
				pstmt.setString(1, dateStr);
				pstmt.setInt(2, index);
				pstmt.setBoolean(3, inserted);
				pstmt.setInt(4, localIdentityId);
				rowCount = pstmt.executeUpdate();
			}

			if (rowCount == 0) {
				try (PreparedStatement pstmt = connection.prepareStatement(insertMessageList)) {
					pstmt.setInt(1, localIdentityId);
					pstmt.setString(2, dateStr);
					pstmt.setInt(3, index);
					pstmt.setBoolean(4, inserted);
					pstmt.executeUpdate();
				}
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("update messagelist_insert failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}
	}

	public synchronized void setMessageRead(int messageId, boolean read) {
		final String updateMessage = "UPDATE message "
			+ "SET read=? "
			+ "WHERE message_id=?";

		try {
			try (PreparedStatement pstmt = connection.prepareStatement(updateMessage)) {
				pstmt.setBoolean(1, read);
				pstmt.setInt(2, messageId);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			logSqlException("update board failed", e);
		}
	}

	public synchronized void setBoardMessagesRead(int boardId, boolean read) {
		final String updateMessage = "UPDATE message "
			+ "SET read=? "
			+ "WHERE message.read=? AND EXISTS "
			+ "(SELECT * FROM board_message bm WHERE bm.board_id=? AND message.message_id = bm.message_id)";

		try {
			try (PreparedStatement pstmt = connection.prepareStatement(updateMessage)) {
				pstmt.setBoolean(1, read);
				pstmt.setBoolean(2, !read);
				pstmt.setInt(3, boardId);
				pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			logSqlException("update message failed", e);
		}
	}

	// TODO remove?
	public synchronized List<MessageReference> getMessageList(int identityId, int limit) {
		final String selectMessages = "SELECT m.message_id, "
			+ "m.insert_date, m.insert_index, "
			+ "bm.board_id FROM message m "
			+ "INNER JOIN board_message bm ON (m.message_id = bm.message_id) "
			+ "WHERE m.identity_id=? "
			+ "ORDER BY m.date DESC, m.time DESC LIMIT ?";

		BoardManager boardManager = FmsManager.getInstance().getBoardManager();
		Map<Integer, MessageReference> messageList = new HashMap<>();

		try (PreparedStatement pstmt = connection.prepareStatement(selectMessages)) {
			pstmt.setInt(1, identityId);
			pstmt.setInt(2, limit);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				int messageId = rs.getInt(1);
				MessageReference msgRef = messageList.get(messageId);
				if (msgRef == null) {
					msgRef = new MessageReference();
					msgRef.setBoards(new ArrayList<>());
					messageList.put(messageId, msgRef);
				}

				final LocalDate date = LocalDate.parse(rs.getString(2),
						DateTimeFormatter.ISO_LOCAL_DATE);
				msgRef.setDate(date);
				msgRef.setIndex(rs.getInt(3));

				int boardId = rs.getInt(4);

				String boardName = boardManager.getBoardName(boardId);
				msgRef.getBoards().add(boardName);
			}
		} catch (SQLException e) {
			logSqlException("getting messages failed", e);
		}

		return messageList.entrySet().stream()
			.map(t -> t.getValue())
			.collect(Collectors.toList());
	}

	public synchronized List<MessageReference> getExternalMessageList(int identityId, int limit) {
		final String selectMessages = "SELECT m.message_id, "
			+ "m.identity_id, m.insert_date, m.insert_index, "
			+ "bm.board_id from message m "
			+ "INNER JOIN board_message bm ON (m.message_id = bm.message_id) "
			+ "WHERE m.identity_id<>? "
			+ "ORDER BY m.date DESC, m.time DESC LIMIT ?";

		BoardManager boardManager = FmsManager.getInstance().getBoardManager();
		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
		Map<Integer, MessageReference> messageList = new HashMap<>();

		try (PreparedStatement pstmt = connection.prepareStatement(selectMessages)) {
			pstmt.setInt(1, identityId);
			pstmt.setInt(2, limit);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				int messageId = rs.getInt(1);
				MessageReference msgRef = messageList.get(messageId);
				if (msgRef == null) {
					msgRef = new MessageReference();
					msgRef.setBoards(new ArrayList<>());
					messageList.put(messageId, msgRef);
				}

				int msgIdentityId = rs.getInt(2);
				msgRef.setIdentityId(msgIdentityId);
				msgRef.setSsk(identityManager.getSsk(msgIdentityId));
				final LocalDate date = LocalDate.parse(rs.getString(3),
						DateTimeFormatter.ISO_LOCAL_DATE);
				msgRef.setDate(date);
				msgRef.setIndex(rs.getInt(4));

				int boardId = rs.getInt(5);

				String boardName = boardManager.getBoardName(boardId);
				msgRef.getBoards().add(boardName);
			}
		} catch (SQLException e) {
			logSqlException("getting messages failed", e);
		}

		return messageList.entrySet().stream()
			.map(t -> t.getValue())
			.collect(Collectors.toList());
	}

	public synchronized Message getMessage(int messageId) {
		Message message = null;

		final String selectMessage = "SELECT m.identity_id, "
			+ "m.date, m.time, m.subject, m.message_uuid, m.reply_board_id, "
			+ "m.body "
			+ "FROM message m "
			+ "WHERE m.message_id=?";
		final String selectBoard = "SELECT board_id "
			+ "FROM board_message "
			+ "WHERE message_id=?";
		final String selectInReplyTo = "SELECT reply_order, message_uuid "
			+ "FROM message_reply_to "
			+ "WHERE message_id=?";
		final String selectAttachment = "SELECT uri, size "
			+ "FROM attachment "
			+ "WHERE message_id=?";
		BoardManager boardManager = FmsManager.getInstance().getBoardManager();

		try {
			try (PreparedStatement pstmt =
					connection.prepareStatement(selectMessage)) {
				pstmt.setInt(1, messageId);

				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					message = new Message();
					message.setIdentityId(rs.getInt(1));
					final LocalDate date = LocalDate.parse(rs.getString(2),
							DateTimeFormatter.ISO_LOCAL_DATE);
					message.setDate(date);
					final LocalTime time = LocalTime.parse(rs.getString(3),
							DateTimeFormatter.ISO_LOCAL_TIME);
					message.setTime(time);
					message.setSubject(rs.getString(4));
					message.setMessageUuid(rs.getString(5));
					int boardId = rs.getInt(6);
					message.setBody(rs.getString(7));

					message.setReplyBoard(boardManager.getBoardName(boardId));
				}
			}

			if (message == null) {
				LOG.log(Level.WARNING, "message with ID={0} not found",
						messageId);
				return null;
			}

			List<String> boards = new ArrayList<>();
			try (PreparedStatement pstmt =
					connection.prepareStatement(selectBoard)) {
				pstmt.setInt(1, messageId);

				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					int boardId = rs.getInt(1);
					boards.add(boardManager.getBoardName(boardId));
				}
			}
			message.setBoards(boards);

			InReplyTo inReplyTo = new InReplyTo();
			try (PreparedStatement pstmt =
					connection.prepareStatement(selectInReplyTo)) {
				pstmt.setInt(1, messageId);

				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					int order = rs.getInt(1);
					String messageUuid = rs.getString(2);
					inReplyTo.add(order, messageUuid);
				}
			}
			message.setInReplyTo(inReplyTo);

			List<Attachment> attachments = new ArrayList<>();
			try (PreparedStatement pstmt =
					connection.prepareStatement(selectAttachment)) {
				pstmt.setInt(1, messageId);

				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					String uri = rs.getString(1);
					int size = rs.getInt(2);
					attachments.add(new Attachment(uri, size));
				}
			}
			if (!attachments.isEmpty()) {
				message.setAttachments(attachments);
			}
		} catch (SQLException e) {
			message = null;
			logSqlException("get messages failed", e);
		}

		return message;
	}

	public synchronized List<Message> getMessagesForBoard(String board) {
		List<Message> messages = new ArrayList<>();
		BoardManager boardManager = FmsManager.getInstance().getBoardManager();
		Integer boardId = boardManager.getBoardId(board);
		if (boardId == null) {
			LOG.log(Level.WARNING, "Skipping unknown board {0}", board);
			return messages;
		}

		final String selectBoardMessages = "SELECT bm.board_id, bm.message_id "
			+ "FROM board_message bm "
			+ "WHERE bm.board_id<>? AND EXISTS "
			+ "(SELECT message_id FROM board_message inner_bm "
			+ "WHERE inner_bm.board_id=? AND bm.message_id = inner_bm.message_id)";

		final String selectMessages = "SELECT m.message_id, m.identity_id, "
			+ "m.date, m.time, m.subject, m.message_uuid, m.reply_board_id, "
			+ " m.read, rt.message_uuid "
			+ "FROM message m "
			+ "INNER JOIN board_message bm ON (m.message_id = bm.message_id) "
			+ "LEFT OUTER JOIN message_reply_to rt ON (m.message_id = rt.message_id) "
			+ "WHERE bm.board_id=? "
			+ "AND (rt.reply_order IS NULL OR rt.reply_order=0)";

		// get all additional boards to handle cross posting

		try {
			connection.setAutoCommit(false);

			Map<Integer, List<String>> xMessageBoardMap = new HashMap<>();
			try (PreparedStatement pstmt =
					connection.prepareStatement(selectBoardMessages)) {
				pstmt.setInt(1, boardId);
				pstmt.setInt(2, boardId);

				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					final int xBoardId = rs.getInt(1);
					final int xMessageId = rs.getInt(2);

					final String xBoardName = boardManager.getBoardName(xBoardId);
					if (xBoardName == null) {
						LOG.log(Level.WARNING, "Skipping unknown board {0}",
								board);
						continue;
					}

					List<String> xBoardList = xMessageBoardMap.get(xMessageId);
					if (xBoardList == null) {
						xBoardList = new ArrayList<>();
						xBoardList.add(board);

						xMessageBoardMap.put(xMessageId, xBoardList);
					}
					xBoardList.add(xBoardName);
				}
			}

			try (PreparedStatement pstmt =
					connection.prepareStatement(selectMessages)) {
				pstmt.setInt(1, boardId);

				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					Message m = new Message();
					m.setMessageId(rs.getInt(1));
					m.setIdentityId(rs.getInt(2));
					final LocalDate date = LocalDate.parse(rs.getString(3),
							DateTimeFormatter.ISO_LOCAL_DATE);
					m.setDate(date);
					final LocalTime time = LocalTime.parse(rs.getString(4),
							DateTimeFormatter.ISO_LOCAL_TIME);
					m.setTime(time);
					m.setSubject(rs.getString(5));
					m.setMessageUuid(rs.getString(6));
					int replyBoardId = rs.getInt(7);
					m.setRead(rs.getBoolean(8));
					m.setParentId(rs.getString(9));

					String replyBoard = boardManager.getBoardName(replyBoardId);
					if (replyBoard == null) {
						LOG.log(Level.WARNING, "unknown reply board {0}",
								replyBoardId);
						replyBoard = board;
					}
					m.setReplyBoard(replyBoard);

					List<String> boards = xMessageBoardMap.get(m.getMessageId());
					if (boards == null) {
						boards = Arrays.asList(new String[]{board});
					}
					m.setBoards(boards);

					messages.add(m);
				}
			}
		} catch (SQLException e) {
			try {
				logSqlException("get messages failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				logSqlException("failed to enable autocommit", ex);
			}
		}

		return messages;
	}

	public synchronized String getMessageBody(int messageId) {
		final String selectMessage = "SELECT body "
			+ "FROM message "
			+ "WHERE message_id=?";
		try (PreparedStatement statement =
				connection.prepareStatement(selectMessage)) {
			statement.setInt(1, messageId);

			ResultSet results = statement.executeQuery();
			if (results.next()) {
				return results.getString(1);
			}
		} catch (SQLException e) {
			logSqlException("select message failed", e);
		}

		return "Failed to retrieve message " + messageId;
	}

	// XXX unused
	public synchronized List<String> getBoardsForMessage(int messageId) {
		final String selectBoard = "SELECT board_id "
			+ "FROM board_message "
			+ "WHERE message_id=?";

		List<String> boards = new ArrayList<>();
		try (PreparedStatement statement =
				connection.prepareStatement(selectBoard)) {
			statement.setInt(1, messageId);

			try (PreparedStatement pstmt =
					connection.prepareStatement(selectBoard)) {
				pstmt.setInt(1, messageId);

				ResultSet rs = pstmt.executeQuery();

				BoardManager boardManager = FmsManager.getInstance().getBoardManager();
				while (rs.next()) {
					int boardId = rs.getInt(1);
					boards.add(boardManager.getBoardName(boardId));
				}
			}
		} catch (SQLException e) {
			logSqlException("select message failed", e);
		}

		return boards;
	}

	public synchronized Map<String, Integer> getBoardNames() {
		Map<String,Integer> boardNames = new HashMap<>();

		final String selectBoards = "SELECT board_id, name "
			+ "FROM board";
		try (Statement stmt = connection.createStatement()) {
			ResultSet results = stmt.executeQuery(selectBoards);
			while (results.next()) {
				int boardId = results.getInt(1);
				String name = results.getString(2);
				boardNames.put(name, boardId);
			}
		} catch (SQLException e) {
			logSqlException("getting boards failed", e);
		}

		return boardNames;
	}

	public synchronized Map<String, Integer> getBoardInfos() {
		Map<String,Integer> boardInfos = new HashMap<>();

		final String selectBoards = "SELECT b.name, COUNT(bm.message_id) "
			+ "FROM board b, board_message bm "
			+ "WHERE b.board_id = bm.board_id "
			+ "GROUP BY b.board_id";
		try (Statement stmt = connection.createStatement()) {
			ResultSet rs = stmt.executeQuery(selectBoards);
			while (rs.next()) {
				String name = rs.getString(1);
				int messageCount = rs.getInt(2);
				boardInfos.put(name, messageCount);
			}
		} catch (SQLException e) {
			logSqlException("getting board infos failed", e);
		}

		return boardInfos;
	}

	public synchronized List<String> getSubscribedBoardNames() {
		List<String> boardNames = new ArrayList<>();

		final String selectBoards = "SELECT name "
			+ "FROM board "
			+ "WHERE subscribed = 1 "
			+ "ORDER BY name";
		try (Statement stmt = connection.createStatement()) {
			ResultSet results = stmt.executeQuery(selectBoards);
			while (results.next()) {
				String name = results.getString(1);
				boardNames.add(name);
			}
		} catch (SQLException e) {
			logSqlException("getting boards failed", e);
		}

		return boardNames;
	}

	public synchronized int getUnreadMessageCount(int boardId) {
		int count = 0;

		final String selectMessages = "SELECT COUNT(*) "
			+ "FROM message m "
			+ "INNER JOIN board_message bm ON (m.message_id = bm.message_id) "
			+ "WHERE bm.board_id=? AND m.read = 0";
		try (PreparedStatement pstmt = connection.prepareStatement(selectMessages)) {
			pstmt.setInt(1, boardId);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException e) {
			logSqlException("getting unread message count failed", e);
		}

		return count;
	}

	public synchronized Request getLastRequestFromHistory(Integer identityId, Request.Type type) {
		final String selectHistory = "SELECT "
			+ getHistoryColumnName(type, "date") + ","
			+ getHistoryColumnName(type, "index") + " "
			+ "FROM request_history "
			+ "WHERE identity_id=?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectHistory)) {
			pstmt.setInt(1, identityId);

			ResultSet rs = pstmt.executeQuery();
			String dateStr = null;
			int index = -1;
			if (rs.next()) {
				dateStr = rs.getString(1);
				index = rs.getInt(2);
				if (rs.wasNull()) {
					index = -1;
				}
			}

			if (dateStr != null && index >= 0) {
				final LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
				return new Request(type, date, index);
			}
		} catch (SQLException e) {
			logSqlException("select request_history failed", e);
		}

		return null;
	}

	public synchronized List<String> getSeedIdentitySsks() {
		List<String> ssks = new ArrayList<>();

		final String selectIdentities = "SELECT ssk "
			+ "FROM identity "
			+ "WHERE added_by=?";

		try (PreparedStatement pstmt =
				connection.prepareStatement(selectIdentities)) {
			pstmt.setInt(1, Constants.ADD_SEED_IDENTITY);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				ssks.add(rs.getString(1));
			}
		} catch (SQLException e) {
			logSqlException("getting seed identities failed", e);
		}

		return ssks;
	}

	public void addSeedTrust(Integer localIdentityId) {
		final String insertSeedTrust = "INSERT INTO local_trust"
			+ "(local_identity_id, identity_id, trustlist_trust) "
			+ "SELECT ?, id.identity_id, ? "
			+ "FROM identity id "
			+ "WHERE added_by=?";

		try (PreparedStatement pstmt =
				connection.prepareStatement(insertSeedTrust)) {
			pstmt.setInt(1, localIdentityId);
			pstmt.setInt(2, 50);
			pstmt.setInt(3, Constants.ADD_SEED_IDENTITY);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			logSqlException("adding seed trust failed", e);
		}
	}

	public int addManualIdentity(String ssk) {
		final String insertIdentity =
			"INSERT INTO identity(ssk, date_added, added_by) VALUES(?,?,?)";
		final String now = LocalDate.now()
			.format(DateTimeFormatter.ISO_LOCAL_DATE);

		int identityId = -1;

		try (PreparedStatement pstmt =
				connection.prepareStatement(insertIdentity)) {

			pstmt.setString(1, ssk);
			pstmt.setString(2, now);
			pstmt.setInt(3, Constants.ADD_MANUALLY);
			pstmt.executeUpdate();

			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				identityId = rs.getInt(1);
			}
		} catch (SQLException e) {
			logSqlException("insert identity failed", e);
		}

		return identityId;
	}

	private void addIdentityIfNotExists(int trusterId, String ssk)
		throws SQLException {

		final IdentityManager identityManager =
			FmsManager.getInstance().getIdentityManager();
		Integer identityId = identityManager.getIdentityId(ssk);
		if (identityId != null) {
			return;
		}

		LOG.log(Level.FINE, "Found new ID {0} in trust list", ssk);
		final String insertIdentity = "INSERT INTO identity "
			+ "(ssk, date_added, added_by) "
			+ "VALUES(?,?,?)";
		final String now = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

		try (PreparedStatement pstmt = connection.prepareStatement(insertIdentity)) {
			pstmt.setString(1, ssk);
			pstmt.setString(2, now);
			pstmt.setInt(3, trusterId);
			pstmt.executeUpdate();

			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				identityId = rs.getInt(1);
			}

			if (identityId == null) {
				throw new SQLException("failed to get ID for identity " + ssk);
			}

			identityManager.addIdentity(identityId, new Identity(ssk));
		}
	}

	private void addPeerTrustEntries(int trusterId, List<Trust> newTrusts) throws SQLException {
		final String insertTrust = "INSERT INTO peer_trust VALUES(?,?,?,?,?,?)";
		final IdentityManager identityManager =
			FmsManager.getInstance().getIdentityManager();

		try (PreparedStatement statement = connection.prepareStatement(insertTrust)) {
			statement.setInt(1, trusterId);
			for (Trust t : newTrusts) {
				final String ssk = t.getIdentity();
				LOG.log(Level.FINEST, "adding new peer trust entry for {0}", ssk);

				Integer identityId = identityManager.getIdentityId(ssk);
				if (identityId == null) {
					LOG.log(Level.WARNING, "Lookup of Identity {0} failed", ssk);
					continue;
				}

				statement.setInt(2, identityId);
				if (t.getTrustListTrustLevel() >= 0) {
					statement.setInt(3, t.getTrustListTrustLevel());
				} else {
					statement.setNull(3, Types.INTEGER);
				}
				if (t.getMessageTrustLevel() >= 0) {
					statement.setInt(4, t.getMessageTrustLevel());
				} else {
					statement.setNull(4, Types.INTEGER);
				}
				final String trustListComment = t.getTrustListTrustComment();
				if (trustListComment != null && !trustListComment.isEmpty()) {
					statement.setString(5, trustListComment);
				} else {
					statement.setString(5, null);
				}
				final String msgComment = t.getMessageTrustComment();
				if (msgComment != null && !msgComment.isEmpty()) {
					statement.setString(6, msgComment);
				} else {
					statement.setString(6, null);
				}

				statement.addBatch();
			}

			statement.executeBatch();
		}
	}

	private void updatePeerTrustEntries(int trusterId, List<Trust> changedTrusts)
	throws SQLException {
		final String updatePeerTrust = "UPDATE peer_trust "
			+ "SET trustlist_trust=?, message_trust=?, "
			+ "trustlist_trust_comment=?, message_trust_comment=? "
			+ "WHERE identity_id=? AND target_identity_id=?";

		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();

		try (PreparedStatement pstmt = connection.prepareStatement(updatePeerTrust)) {
			for (Trust t : changedTrusts) {
				LOG.log(Level.FINEST, "updating peer trust entry for {0}",
						t.getIdentity());

				if (t.getTrustListTrustLevel() >= 0) {
					pstmt.setInt(1, t.getTrustListTrustLevel());
				} else {
					pstmt.setNull(1, Types.INTEGER);
				}
				if (t.getMessageTrustLevel() >= 0) {
					pstmt.setInt(2, t.getMessageTrustLevel());
				} else {
					pstmt.setNull(2, Types.INTEGER);
				}
				final String trustListComment = t.getTrustListTrustComment();
				if (trustListComment != null && !trustListComment.isEmpty()) {
					pstmt.setString(3, trustListComment);
				} else {
					pstmt.setString(3, null);
				}
				final String msgComment = t.getMessageTrustComment();
				if (msgComment != null && !msgComment.isEmpty()) {
					pstmt.setString(4, msgComment);
				} else {
					pstmt.setString(4, null);
				}

				pstmt.setInt(5, trusterId);
				Integer targetIdentityId = identityManager.getIdentityId(t.getIdentity());
				pstmt.setInt(6, targetIdentityId);

				pstmt.addBatch();
			}

			pstmt.executeBatch();
		}
	}

	private void removePeerTrustEntries(int trusterId, Set<String> trustsToRemove)
	throws SQLException {
		final String deletePeerTrust = "DELETE FROM peer_trust "
			+ "WHERE identity_id=? AND target_identity_id=?";

		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();

		try (PreparedStatement pstmt = connection.prepareStatement(deletePeerTrust)) {
			pstmt.setInt(1, trusterId);
			for (String targetIdentity : trustsToRemove) {
				LOG.log(Level.FINEST, "removing peer trust entry for {0}",
						targetIdentity);
				pstmt.setInt(2, identityManager.getIdentityId(targetIdentity));
				pstmt.addBatch();
			}

			pstmt.executeBatch();
		}
	}

	private void createTables() throws SQLException {
		LOG.log(Level.INFO, "Creating Database tables");

		final String createIdentityTable = "CREATE TABLE IF NOT EXISTS identity("
			+ "identity_id INTEGER NOT NULL, "
			+ "ssk CHAR(100) UNIQUE NOT NULL, "
			+ "name VARCHAR(255), "
			+ "signature VARCHAR(255), "
			+ "avatar VARCHAR(255), "
			+ "single_use BOOLEAN DEFAULT 0, "
			+ "publish_trustlist BOOLEAN DEFAULT 0, "
			+ "publish_boardlist BOOLEAN DEFAULT 0, "
			+ "freesite_edition INTEGER, "
			+ "date_added DATE, "
			+ "added_by INTEGER, "
			+ "PRIMARY KEY(identity_id)"
			+ ")";

		final String createLocalIdentityTable = "CREATE TABLE IF NOT EXISTS local_identity("
			+ "local_identity_id INTEGER NOT NULL, "
			+ "ssk CHAR(100) UNIQUE NOT NULL, "
			+ "private_ssk CHAR(100) UNIQUE NOT NULL, "
			+ "name VARCHAR(255), "
			+ "signature VARCHAR(255), "
			+ "avatar VARCHAR(255), "
			+ "single_use BOOLEAN DEFAULT 0, "
			+ "publish_trustlist BOOLEAN DEFAULT 0, "
			+ "publish_boardlist BOOLEAN DEFAULT 0, "
			+ "active BOOLEAN DEFAULT 0, "
			+ "PRIMARY KEY(local_identity_id)"
			+ ")";

		final String createInsertHistoryTable = "CREATE TABLE IF NOT EXISTS insert_history("
			+ "local_identity_id INTEGER NOT NULL, "
			+ "last_identity_date DATE, "
			+ "last_identity_index INTEGER, "
			+ "last_trustlist_date DATE, "
			+ "last_trustlist_index INTEGER, "
			+ "last_messagelist_date DATE, "
			+ "last_messagelist_index INTEGER, "
			+ "PRIMARY KEY(local_identity_id)"
			+")";

		final String createLocalIdentityInsertTable = "CREATE TABLE IF NOT EXISTS local_identity_insert("
			+ "local_identity_id INTEGER NOT NULL, "
			+ "insert_date DATE, "
			+ "insert_index INTEGER, "
			+ "PRIMARY KEY(local_identity_id, insert_date, insert_index)"
			+")";

		final String createMessageListInsertTable = "CREATE TABLE IF NOT EXISTS messagelist_insert("
			+ "local_identity_id INTEGER NOT NULL, "
			+ "insert_date DATE, "
			+ "insert_index INTEGER, "
			+ "inserted BOOLEAN DEFAULT 0,"
			+ "PRIMARY KEY(local_identity_id, insert_date, insert_index)"
			+")";


		final String createLocalMessageTable = "CREATE TABLE IF NOT EXISTS local_message("
			+ "local_identity_id INTEGER NOT NULL, "
			+ "insert_date DATE, "
			+ "insert_index INTEGER, "
			+ "message_xml TEXT, "
			+ "inserted BOOLEAN DEFAULT 0"
			+ ")";

		final String createTrustTable = "CREATE TABLE IF NOT EXISTS peer_trust("
			+ "identity_id INTEGER NOT NULL, "
			+ "target_identity_id INTEGER, "
			+ "trustlist_trust INTEGER, "
			+ "message_trust INTEGER, "
			+ "trustlist_trust_comment VARCHAR(255), "
			+ "message_trust_comment VARCHAR(255), "
			+ "PRIMARY KEY(identity_id, target_identity_id)"
			+ ")";

		final String createMessageTable = "CREATE TABLE IF NOT EXISTS message("
			+ "message_id INTEGER NOT NULL, "
			+ "identity_id INTEGER, "
			+ "date DATE, "
			+ "time TIME, "
			+ "subject VARCHAR(255), "
			+ "message_uuid CHAR(80), "
			+ "reply_board_id INTEGER, "
			+ "insert_date CHAR(10), "
			+ "insert_index INTEGER, "
			+ "body TEXT, "
			+ "read BOOLEAN DEFAULT 0, "
			+ "PRIMARY KEY(message_id)"
			+ ")";

		final String createInReplyToTable = "CREATE TABLE IF NOT EXISTS message_reply_to("
			+ "message_id INTEGER NOT NULL, "
			+ "reply_order INTEGER NOT NULL, "
			+ "message_uuid TEXT, "
			+ "PRIMARY KEY(message_id, reply_order)"
			+ ")";

		final String createAttachmentTable = "CREATE TABLE IF NOT EXISTS attachment("
			+ "message_id INTEGER NOT NULL, "
			+ "uri VARCHAR(255), "
			+ "size INTEGER"
			+ ")";

		final String createBoardTable = "CREATE TABLE IF NOT EXISTS board("
			+ "board_id INTEGER NOT NULL, "
			+ "name VARCHAR(255) UNIQUE, "
			+ "subscribed BOOLEAN, "
			+ "PRIMARY KEY(board_id)"
			+ ")";

		final String createBoardMessageTable = "CREATE TABLE IF NOT EXISTS board_message("
			+ "board_id INTEGER NOT NULL, "
			+ "message_id INTEGER NOT NULL, "
			+ "PRIMARY KEY(board_id, message_id)"
			+ ")";

		final String createLocalTrustTable = "CREATE TABLE IF NOT EXISTS local_trust("
			+ "local_identity_id INTEGER NOT NULL, "
			+ "identity_id INTEGER NOT NULL, "
			+ "trustlist_trust INTEGER, "
			+ "message_trust INTEGER, "
			+ "trustlist_trust_comment VARCHAR(255), "
			+ "message_trust_comment VARCHAR(255), "
			+ "PRIMARY KEY(local_identity_id, identity_id)"
			+ ")";

		final String createRequestHistoryTable = "CREATE TABLE IF NOT EXISTS request_history("
			+ "identity_id INTEGER NOT NULL, "
			+ "last_identity_date DATE, "
			+ "last_identity_index INTEGER, "
			+ "last_trustlist_date DATE, "
			+ "last_trustlist_index INTEGER, "
			+ "last_messagelist_date DATE, "
			+ "last_messagelist_index INTEGER, "
			+ "PRIMARY KEY(identity_id)"
			+")";

		try {
			connection.setAutoCommit(false);

			try (Statement statement = connection.createStatement()) {

				statement.addBatch(createIdentityTable);
				statement.addBatch(createLocalIdentityTable);
				statement.addBatch(createInsertHistoryTable);
				statement.addBatch(createLocalIdentityInsertTable);
				statement.addBatch(createMessageListInsertTable);
				statement.addBatch(createLocalMessageTable);
				statement.addBatch(createTrustTable);
				statement.addBatch(createMessageTable);
				statement.addBatch(createInReplyToTable);
				statement.addBatch(createAttachmentTable);
				statement.addBatch(createBoardTable);
				statement.addBatch(createBoardMessageTable);
				statement.addBatch(createLocalTrustTable);
				statement.addBatch(createRequestHistoryTable);

				statement.executeBatch();
			}
		} catch (SQLException e) {
			try {
				logSqlException("creating tables failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			connection.setAutoCommit(true);
		}
	}

	private void addSeedIdentities(List<String> seedIdentities)
		throws SQLException {

		final String identityCountQuery = "SELECT COUNT(*) FROM identity "
				+ "WHERE ssk=?";
		final String insertSeedIdentity =
			"INSERT INTO identity(ssk, date_added, added_by) VALUES(?,?,?)";
		final String now = LocalDate.now()
			.format(DateTimeFormatter.ISO_LOCAL_DATE);

		try {
			LOG.log(Level.INFO, "Populating database with seed identities");

			connection.setAutoCommit(false);
			for (String ssk : seedIdentities) {
				try (PreparedStatement pstmt =
						connection.prepareStatement(identityCountQuery)) {

					pstmt.setString(1, ssk);

					ResultSet rs = pstmt.executeQuery();
					if (rs.next()) {
						int identityCount = rs.getInt(1);
						if (identityCount > 0) {
							continue;
						}
					}
				}

				try (PreparedStatement pstmt =
						connection.prepareStatement(insertSeedIdentity)) {

					pstmt.setString(1, ssk);
					pstmt.setString(2, now);
					pstmt.setInt(3, Constants.ADD_SEED_IDENTITY);
					pstmt.executeUpdate();
				}
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("creating tables failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			connection.setAutoCommit(true);
		}
	}

	private void addSeedBoards() throws SQLException {
		final String boardCountQuery = "SELECT COUNT(*) FROM board";
		final String insertSeedBoard = "INSERT INTO board(name, subscribed) VALUES(?,1)";

		try {
			try (Statement stmt = connection.createStatement()) {
				ResultSet resultSet = stmt.executeQuery(boardCountQuery);
				if (resultSet.next()) {
					int boardCount = resultSet.getInt(1);
					if (boardCount > 0) {
						return;
					}
				}
			}

			LOG.log(Level.INFO, "Populating database with seed boards");
			final String[] seedBoardNames = {
				"fms", "freenet", "public", "test"
			};

			connection.setAutoCommit(false);
			try (PreparedStatement statement =
					connection.prepareStatement(insertSeedBoard)) {
				for (String boardName : seedBoardNames) {
					statement.setString(1, boardName);
					statement.addBatch();
				}

				statement.executeBatch();
			}

			connection.commit();
		} catch (SQLException e) {
			try {
				logSqlException("creating tables failed", e);
				connection.rollback();
			} catch (SQLException ex) {
				logSqlException("rollback failed", ex);
			}
		} finally {
			connection.setAutoCommit(true);
		}
	}

	private int addBoard(String boardName) throws SQLException {
		final String insertBoard = "INSERT INTO board(name, subscribed) "
			+ "VALUES(?,0)";

		int boardId = -1;

		LOG.log(Level.FINE, "Adding new board {0}", boardName);

		try (PreparedStatement pstmt = connection.prepareStatement(insertBoard)) {
			pstmt.setString(1, boardName);
			pstmt.executeUpdate();

			ResultSet rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				boardId = rs.getInt(1);
			}
		}

		if (boardId == -1) {
			throw new SQLException("failed to get board ID");
		}

		return boardId;
	}

	private List<Trust> getTrustListInternal(int identityId) throws SQLException{
		final String selectTrusts = "SELECT target_identity_id, "
			+ "trustlist_trust, message_trust, "
			+ "trustlist_trust_comment, message_trust_comment "
			+ "FROM peer_trust "
			+ "WHERE identity_id=?";

		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
		List<Trust> trustList = new ArrayList<>();
		try (PreparedStatement pstmt = connection.prepareStatement(selectTrusts)) {
			pstmt.setInt(1, identityId);

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Trust trust = new Trust();
				int targetIdentityId = rs.getInt(1);
				String ssk = identityManager.getSsk(targetIdentityId);
				if (ssk == null) {
					LOG.log(Level.WARNING, "skipping unknown ID {0}",
							targetIdentityId);
					continue;
				}
				trust.setIdentity(ssk);

				int trustListTrust = rs.getInt(2);
				if (!rs.wasNull()) {
					trust.setTrustListTrustLevel(trustListTrust);
				}

				int messageTrust = rs.getInt(3);
				if (!rs.wasNull()) {
					trust.setMessageTrustLevel(messageTrust);
				}

				trust.setTrustListTrustComment(rs.getString(4));
				trust.setMessageTrustComment(rs.getString(5));

				trustList.add(trust);
			}
		}

		return trustList;
	}
}
