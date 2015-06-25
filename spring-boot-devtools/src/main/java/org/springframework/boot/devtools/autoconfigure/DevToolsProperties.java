/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for developer tools.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.devtools")
public class DevToolsProperties {

	private static final String DEFAULT_RESTART_EXCLUDES = "META-INF/maven/**,"
			+ "META-INF/resources/**,resources/**,static/**,public/**,templates/**";

	private static final long DEFAULT_RESTART_POLL_INTERVAL = 1000;

	private static final long DEFAULT_RESTART_QUIET_PERIOD = 400;

	private Restart restart = new Restart();

	private Livereload livereload = new Livereload();

	@NestedConfigurationProperty
	private RemoteDevToolsProperties remote = new RemoteDevToolsProperties();

	public Restart getRestart() {
		return this.restart;
	}

	public Livereload getLivereload() {
		return this.livereload;
	}

	public RemoteDevToolsProperties getRemote() {
		return this.remote;
	}

	/**
	 * Restart properties
	 */
	public static class Restart {

		/**
		 * Enable automatic restart.
		 */
		private boolean enabled = true;

		/**
		 * Patterns that should be excluding for triggering a full restart.
		 */
		private String exclude = DEFAULT_RESTART_EXCLUDES;

		/**
		 * Amount of time (in milliseconds) to wait between polling for classpath changes.
		 */
		private long pollInterval = DEFAULT_RESTART_POLL_INTERVAL;

		/**
		 * Amount of quiet time (in milliseconds) required without any classpath changes
		 * before a restart is triggered.
		 */
		private long quietPeriod = DEFAULT_RESTART_QUIET_PERIOD;

		/**
		 * Name of a specific file that when changed will trigger the restart check. If
		 * not specified any classpath file change will trigger the restart.
		 */
		private String triggerFile;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getExclude() {
			return this.exclude;
		}

		public void setExclude(String exclude) {
			this.exclude = exclude;
		}

		public long getPollInterval() {
			return this.pollInterval;
		}

		public void setPollInterval(long pollInterval) {
			this.pollInterval = pollInterval;
		}

		public long getQuietPeriod() {
			return this.quietPeriod;
		}

		public void setQuietPeriod(long quietPeriod) {
			this.quietPeriod = quietPeriod;
		}

		public String getTriggerFile() {
			return this.triggerFile;
		}

		public void setTriggerFile(String triggerFile) {
			this.triggerFile = triggerFile;
		}

	}

	/**
	 * LiveReload properties
	 */
	public static class Livereload {

		/**
		 * Enable a livereload.com compatible server.
		 */
		private boolean enabled = true;

		/**
		 * Server port.
		 */
		private int port = 35729;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

	}

}
