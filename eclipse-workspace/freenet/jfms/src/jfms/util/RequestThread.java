package jfms.util;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;


// TODO use semaphore instead of lock with cond?
public abstract class RequestThread extends Task<Integer> {
	private static final Logger LOG = Logger.getLogger(RequestThread.class.getName());

	private final Lock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();
	private volatile int pendingRequests = 0;

	public void addRequest() {
		lock.lock();
		try {
			pendingRequests++;
		} finally {
			lock.unlock();
		}
	}

	public void waitUntilReady(int maxRequests) throws InterruptedException {
		lock.lock();
		try {
			while (pendingRequests >= maxRequests) {
				LOG.log(Level.FINEST, "requests pending: {0}/{1}", new Object[]{
						pendingRequests, maxRequests});

				cond.await();
				if (isCancelled()) {
					LOG.log(Level.INFO, "download thread cancelled");
					throw new InterruptedException("thread cancelled");
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public void requestDone() {
		lock.lock();
		try {
			pendingRequests--;
			cond.signal();
		} finally {
			lock.unlock();
		}
	}
}
