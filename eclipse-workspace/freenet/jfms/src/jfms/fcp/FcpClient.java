package jfms.fcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.config.Constants;

public class FcpClient extends Thread {
	private static final Logger LOG = Logger.getLogger(FcpClient.class.getName());

	private final String name;
	private final String host;
	private final int port;
	private final AtomicBoolean stop = new AtomicBoolean(false);
	private Socket socket;
	private BufferedOutputStream out;
	private BufferedInputStream in;

	// TODO merge FcpListener and FcpStatusListener?
	private final Map<String, FcpListener> listenerMap = new ConcurrentHashMap<>();
	private FcpStatusListener statusListener;

	public enum Status {
		CONNECTED,
		DISCONNECTED,
		CONNECT_FAILED
	}

	public FcpClient() {
		name = "none";
		host = null;
		port = -1;
	}

	public FcpClient(String name, String host, int port) {
		this.name = name;
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		LOG.log(Level.INFO, "Starting FCP receiver thread with ID {0}",
				Thread.currentThread().getId());

		try {
			while (!stop.get()) {
				receiveAndHandleResponse();
			}
		} catch (IOException|FcpException e) {
			LOG.log(Level.WARNING, "Exception in FCP thread", e);

			final String message = "FCP error: " + e.getMessage();
			listenerMap.values().stream().distinct()
				.forEach(l -> l.fatalError(message));
			statusListener.statusChanged(Status.DISCONNECTED);
		}

		LOG.log(Level.INFO, "Finished FCP receiver thread");
	}

	public void setStatusListener(FcpStatusListener listener) {
		statusListener = listener;
	}

	public synchronized void connect() throws FcpException {
		if (socket != null) {
			LOG.log(Level.FINEST, "already connected");
			return;
		}

		LOG.log(Level.FINEST,
				"Trying to connect to {0}:{1,number,0}", new Object[]{
				host, port});

		try {
			socket = new Socket(InetAddress.getByName(host), port);
			out = new BufferedOutputStream(socket.getOutputStream());
			in = new BufferedInputStream(socket.getInputStream());

			sendClientHello();

			FcpResponse helloResponse = receiveResponse();
			if (!helloResponse.getName().equals("NodeHello")) {
				throw new FcpException("invalid response to ClientHello");
			}
			LOG.log(Level.INFO, "Node available. Version={0}",
					helloResponse.getField("Revision"));
			statusListener.statusChanged(Status.CONNECTED);
			start();
		} catch (IOException e) {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
				}
				socket = null;
			}
			statusListener.statusChanged(Status.CONNECT_FAILED);
			throw new FcpException("connect failed: " + e.getMessage(), e);
		}
	}

	public synchronized void close() {
		stop.set(true);

		if (socket == null) {
			LOG.log(Level.FINEST, "already closed");
			return;
		}

		try {
			socket.close();
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to close FCP socket", e);
		}

		try {
			join();
		} catch (InterruptedException e) {
			LOG.log(Level.WARNING, "Failed to join thread", e);
		}
		socket = null;
	}

	public synchronized void requestKey(String identifier, String key,
			FcpListener listener) throws FcpException {
		try {
			checkConnection();
			listenerMap.put(identifier, listener);
			sendClientGet(identifier, key);
		} catch (IOException e) {
			throw new FcpException("ClientGet failed", e);
		}
	}

	public synchronized void insertKey(String identifier, String key,
			byte[] data, FcpListener listener) throws FcpException {
		try {
			checkConnection();
			listenerMap.put(identifier, listener);
			sendClientPut(identifier, key, data);
		} catch (IOException e) {
			throw new FcpException("ClientPut failed", e);
		}
	}

	public synchronized void insertDirectory(String identifier, String key,
			FcpDirectoryEntry[] files, String defaultName,
			FcpListener listener) throws FcpException {
		try {
			checkConnection();
			listenerMap.put(identifier, listener);
			sendClientPutComplexDir(identifier, key, files, defaultName);
		} catch (IOException e) {
			throw new FcpException("ClientPutComplexDir failed", e);
		}
	}

	protected Map<String, FcpListener> getListenerMap() {
		return listenerMap;
	}

	protected FcpListener getListener(String identifier) {
		return listenerMap.get(identifier);
	}

	protected void sendClientGet(String identifier, String key) throws IOException {
		LOG.log(Level.FINEST,
				"[FCP] ClientGet Identifer={0} URI={1}", new Object[]{
				identifier, key});

		StringBuilder str = new StringBuilder("ClientGet\n");
		str.append("URI=");
		str.append(key);
		str.append('\n');
		str.append("Identifier=");
		str.append(identifier);
		str.append('\n');
		str.append("Verbosity=0\n");
		str.append("MaxSize=");
		str.append(Constants.MAX_REQUEST_SIZE);
		str.append('\n');
		str.append("ReturnType=direct\n");
		str.append("EndMessage\n");

		byte[] header = str.toString().getBytes(StandardCharsets.US_ASCII);
		out.write(header);
		out.flush();
	}

	protected void sendClientPut(String identifier, String key, byte[] data)
	throws IOException {
		LOG.log(Level.FINEST,
				"[FCP] ClientPut Identifer={0} URI={1}", new Object[]{
				identifier, key});

		StringBuilder str = new StringBuilder("ClientPut\n");
		str.append("URI=");
		str.append(key);
		str.append('\n');
		str.append("Identifier=");
		str.append(identifier);
		str.append('\n');
		str.append("Verbosity=0\n");
		str.append("UploadFrom=direct\n");
		str.append("DataLength=");
		str.append(data.length);
		str.append('\n');
		str.append("EndMessage\n");

		byte[] header = str.toString().getBytes(StandardCharsets.US_ASCII);
		out.write(header);
		out.write(data);
		out.flush();
	}

	protected void sendClientPutComplexDir(String identifier, String key,
			FcpDirectoryEntry[] files, String defaultName)
	throws IOException {
		LOG.log(Level.FINEST,
				"[FCP] ClientPutComplexDir Identifer={0} URI={1}", new Object[]{
				identifier, key});

		StringBuilder str = new StringBuilder("ClientPutComplexDir\n");
		str.append("URI=");
		str.append(key);
		str.append('\n');

		str.append("Identifier=");
		str.append(identifier);
		str.append('\n');

		str.append("Verbosity=0\n");

		if (defaultName != null) {
			str.append("DefaultName=");
			str.append(defaultName);
			str.append('\n');
		}

		str.append("UploadFrom=direct\n");

		ByteArrayOutputStream data = new ByteArrayOutputStream();

		for (int i=0; i<files.length; i++) {
			final FcpDirectoryEntry e = files[i];
			str.append("Files.");
			str.append(i);
			str.append(".Name=");
			str.append(e.getName());
			str.append('\n');

			str.append("Files.");
			str.append(i);
			str.append(".UploadFrom=direct\n");

			str.append("Files.");
			str.append(i);
			str.append(".DataLength=");
			str.append(e.getData().length);
			str.append('\n');

			data.write(e.getData());
		}

		str.append("EndMessage\n");

		byte[] header = str.toString().getBytes(StandardCharsets.US_ASCII);
		out.write(header);
		out.write(data.toByteArray());
		out.flush();
	}

	private void receiveAndHandleResponse() throws IOException, FcpException {
		FcpResponse response = receiveResponse();
		String id = response.getField("Identifier");

		FcpListener listener = listenerMap.get(id);
		if (listener == null) {
			LOG.log(Level.WARNING, "No listener found for request {0}", id);
			return;
		}

		switch (response.getName()) {
		case "AllData":
			LOG.log(Level.FINEST, "[FCP] AllData response Identifer={0}", id);
			listener.finished(id, response.getData());
			listenerMap.remove(id);
			break;
		case "GetFailed":
			String redirectURI = response.getField("RedirectURI");
			if (redirectURI == null) {
				int code = Integer.parseInt(response.getField("Code"));
				listener.error(id, code);
				listenerMap.remove(id);
			} else {
				listener.redirect(id, redirectURI);
			}
			break;
		case "PutFailed":
			int code = Integer.parseInt(response.getField("Code"));
			listener.error(id, code);
			listenerMap.remove(id);
			break;
		case "PutSuccessful":
			listener.putSuccessful(id, response.getField("URI"));
			listenerMap.remove(id);
			break;
		default:
			LOG.log(Level.FINEST, "Unhandled {0}", response.getName());
			break;
		}
	}


	private void checkConnection() throws FcpException
	{
		if (socket == null) {
			connect();
		}
	}

	private void sendClientHello() throws FcpException, IOException {
		LOG.log(Level.FINEST, "[FCP] ClientHello Name={0}", name);

		StringBuilder str = new StringBuilder("ClientHello\n");
		str.append("Name=");
		str.append(name);
		str.append('\n');
		str.append("ExpectedVersion=2.0\n");
		str.append("EndMessage\n");

		byte[] header = str.toString().getBytes(StandardCharsets.US_ASCII);
		out.write(header);
		out.flush();
	}

	private String readLine() throws FcpException, IOException {
		byte[] buf = new byte[1024];
		int length = 0;

		// TODO optimize bytewise read
		for (int i=0; i<buf.length; i++) {
			int c = in.read();
			if (c <= 0) {
				throw new FcpException("connection closed");
			}
			if (c == '\n') {
				length = i;
				break;
			}
			buf[i] = (byte)c;
		}

		String line = new String(buf, 0, length, StandardCharsets.US_ASCII);
		return line;
	}

	private FcpResponse receiveResponse() throws IOException, FcpException {
		LOG.log(Level.FINEST, "Entering receiveResponse");
		String type;
		do {
			type = readLine();
		} while(type.isEmpty());
		LOG.log(Level.FINEST, "received {0} response", type);
		FcpResponse response = new FcpResponse(type);

		while (true) {
			String line = readLine();
			if (line.equals("EndMessage")) {
				return response;
			}

			if (line.equals("Data")) {
				break;
			}

			String[] fields = line.split("=", 2);
			if (fields.length != 2) {
				continue;
			}

			response.addField(fields[0], fields[1]);
			LOG.log(Level.FINEST, "\t{0}: {1}", new Object[]{fields[0], fields[1]});
		}

		String dataLength = response.getField("DataLength");
		if (dataLength == null) {
			throw new FcpException("DataLength missing in response");
		}

		int len = Integer.parseInt(dataLength);
		byte[] data = new byte[len];

		int bytesToRead = len;
		int bytesRead = 0;
		while (bytesRead < bytesToRead) {
			int read = in.read(data, bytesRead, bytesToRead-bytesRead);
			if (read <= 0) {
				throw new FcpException("connection closed");
			}
			bytesRead += read;
		}

		response.setData(data);

		return response;
	}
}
