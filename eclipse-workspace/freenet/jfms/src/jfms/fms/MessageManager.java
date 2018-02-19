package jfms.fms;

public class MessageManager {
	private MessageListener listener;

	public static class MessageAdder implements Runnable {
		private final MessageListener messageListener;
		private final Message message;

		public MessageAdder(MessageListener listener, Message message) {
			this.messageListener = listener;
			this.message = message;
		}

		@Override
		public void run() {
			messageListener.newMessage(message);
		}
	}


	public void addMessage(Message message) {
		if (listener != null) {
			listener.newMessage(message);
		}
	}

	public void setListener(MessageListener listener) {
		this.listener = listener;
	}
}
