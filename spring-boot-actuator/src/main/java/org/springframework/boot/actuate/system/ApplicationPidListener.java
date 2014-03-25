package org.springframework.boot.actuate.system;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.util.SystemUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link org.springframework.context.ApplicationListener} that saves
 * application PID into file
 *
 * @author Jakub Kubrynski
 */
public class ApplicationPidListener implements SmartApplicationListener {

	private static Class<?>[] EVENT_TYPES = {ApplicationStartedEvent.class};

	private static final String DEFAULT_PID_FILE_NAME = "application.pid";

	private static final AtomicBoolean pidFileCreated = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE + 13;

	private final Log log = LogFactory.getLog(getClass());

	private String pidFileName = DEFAULT_PID_FILE_NAME;

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		for (Class<?> type : EVENT_TYPES) {
			if (type.isAssignableFrom(eventType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return SpringApplication.class.isAssignableFrom(sourceType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if (pidFileCreated.get()) {
			return;
		}

		String applicationPid;
		try {
			applicationPid = SystemUtils.getApplicationPid();
		} catch (IllegalStateException ignore) {
			return;
		}

		if (pidFileCreated.compareAndSet(false, true)) {
			File file = new File(pidFileName);
			FileOutputStream fileOutputStream = null;
			try {
				fileOutputStream = new FileOutputStream(file);
				fileOutputStream.write(applicationPid.getBytes());
			} catch (FileNotFoundException e) {
				log.warn(String.format("Cannot create pid file %s !", pidFileName));
			} catch (IOException e) {
				log.warn(String.format("Cannot write to pid file %s!", pidFileName));
			} finally {
				if (fileOutputStream != null) {
					try {
						fileOutputStream.close();
					} catch (IOException e) {
						log.warn(String.format("Cannot close pid file %s!", pidFileName));
					}
				}
			}
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return order;
	}

	/**
	 * Sets the pid file name. This file will contains current process id.
	 *
	 * @param pidFileName the name of file containing pid
	 */
	public void setPidFileName(String pidFileName) {
		this.pidFileName = pidFileName;
	}

}
