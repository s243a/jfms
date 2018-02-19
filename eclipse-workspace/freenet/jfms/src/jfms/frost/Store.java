package jfms.frost;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class Store {
	private static final Logger LOG = Logger.getLogger(Store.class.getName());

	private final String baseDirectory;

	Store() {
		StringBuilder str = new StringBuilder(System.getProperty("user.dir"));
		str.append(File.separatorChar);
		str.append("store");
		str.append(File.separatorChar);
		str.append("frost");

		baseDirectory = str.toString();
	}

	public void initialize() {
		(new File(baseDirectory)).mkdirs();

		retrieveMessages();
	}

	public boolean exists(RequestInfo requestInfo) {
		File file = new File(getFilePath(requestInfo));
		return file.exists();
	}

	public void retrieveMessages() {
		LOG.log(Level.FINEST, "getting messages");
		Path root = new File(baseDirectory).toPath();
		try (Stream<Path> files = Files.walk(root, 3, FileVisitOption.FOLLOW_LINKS)) {
			files.filter(f -> Files.isRegularFile(f))
				.forEach(f -> handleFile(root, f));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to read messages from store", e);
		}
	}

	public String retrieve(RequestInfo requestInfo) {
		try {
			File file = new File(getFilePath(requestInfo));

			byte[] data = Files.readAllBytes(file.toPath());
			return new String(data, StandardCharsets.UTF_8);

		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to retrive frost message", e);
			return null;
		}
	}

	public Message retrieveMessage(Path path) throws IOException {
			byte[] data = Files.readAllBytes(path);

			MessageParser parser = new MessageParser();
			Message msg = parser.parse(new ByteArrayInputStream(data));

			return msg;
	}

	public String getBody(String boardName, String slotDate, int index) {
		try {
			File file = new File(getFilePath(boardName, slotDate, index));

			byte[] data = Files.readAllBytes(file.toPath());

			MessageParser parser = new MessageParser();
			Message msg = parser.parse(new ByteArrayInputStream(data));

			return msg.getBody();

		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to get message body", e);
			return null;
		}
	}

	public void store(RequestInfo requestInfo, String data) {
		try {
			File file = new File(getFilePath(requestInfo));
			file.getParentFile().mkdirs();

			try (FileWriter writer = new FileWriter(file)) {
				writer.write(data);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to store message", e);
		}
	}

	private void handleFile(Path root, Path file) {
		try {
			String relativePath = root.relativize(file).toString();
			char[] separator = { File.separatorChar };
			String[] fields = relativePath.split(new String(separator));
			if (fields.length != 3) {
				return;
			}

			final String boardName = fields[0];
			final String date = fields[1];
			final String index = fields[2].substring(0,fields[2].length() - 4);
			final int numericIndex = Integer.parseInt(index);

			Message message = retrieveMessage(file);
			message.setSlotDate(date);
			message.setIndex(numericIndex);

			final BoardManager BoardManager =
				FrostManager.getInstance().getBoardManager();
			Board board = BoardManager.getBoard(boardName);
			if (board == null) {
				board = new Board(boardName);
				BoardManager.add(board);
			}
			board.addMessage(message);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to read messages from store", e);
		}
	}

	private String getFilePath(RequestInfo requestInfo) {
		StringBuilder path = new StringBuilder(baseDirectory);
		path.append(File.separatorChar);
		path.append(requestInfo.getBoardName());
		path.append(File.separatorChar);
		path.append(requestInfo.getDate());
		path.append(File.separatorChar);
		path.append(requestInfo.getIndex());
		path.append(".xml");

		return path.toString();
	}

	private String getFilePath(String boardName, String slotDate, int index) {
		StringBuilder path = new StringBuilder(baseDirectory);
		path.append(File.separatorChar);
		path.append(boardName);
		path.append(File.separatorChar);
		path.append(slotDate);
		path.append(File.separatorChar);
		path.append(index);
		path.append(".xml");

		return path.toString();
	}
}
