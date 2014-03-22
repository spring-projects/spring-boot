/*
 * Copyright 2010-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
