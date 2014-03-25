package org.springframework.boot.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Class containing methods related to system utilities
 *
 * @author Jakub Kubrynski
 */
public class SystemUtils {

	private static final Log LOG = LogFactory.getLog(SystemUtils.class);

	/**
	 * Looks for application PID
	 * @return application PID
	 * @throws java.lang.IllegalStateException if PID could not be determined
	 */
	public static String getApplicationPid() {
		String pid = null;
		try {
			RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
			String jvmName = runtimeBean.getName();
			if (StringUtils.isEmpty(jvmName)) {
				LOG.warn("Cannot get JVM name");
			}
			if (!jvmName.contains("@")) {
				LOG.warn("JVM name doesn't contain process id");
			}
			pid = jvmName.split("@")[0];
		} catch (Throwable e) {
			LOG.warn("Cannot get RuntimeMXBean", e);
		}

		if (pid == null) {
			throw new IllegalStateException("Application PID not found");
		}

		return pid;
	}

}
