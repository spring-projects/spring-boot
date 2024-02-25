/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for developer tools.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.devtools")
public class DevToolsProperties {

	private final Restart restart = new Restart();

	private final Livereload livereload = new Livereload();

	@NestedConfigurationProperty
	private final RemoteDevToolsProperties remote = new RemoteDevToolsProperties();

	/**
	 * Returns the restart object.
	 * @return the restart object
	 */
	public Restart getRestart() {
		return this.restart;
	}

	/**
	 * Returns the Livereload object associated with this DevToolsProperties instance.
	 * @return the Livereload object
	 */
	public Livereload getLivereload() {
		return this.livereload;
	}

	/**
	 * Returns the remote development tools properties.
	 * @return the remote development tools properties
	 */
	public RemoteDevToolsProperties getRemote() {
		return this.remote;
	}

	/**
	 * Restart properties.
	 */
	public static class Restart {

		private static final String DEFAULT_RESTART_EXCLUDES = "META-INF/maven/**,"
				+ "META-INF/resources/**,resources/**,static/**,public/**,templates/**,"
				+ "**/*Test.class,**/*Tests.class,git.properties,META-INF/build-info.properties";

		/**
		 * Whether to enable automatic restart.
		 */
		private boolean enabled = true;

		/**
		 * Patterns that should be excluded from triggering a full restart.
		 */
		private String exclude = DEFAULT_RESTART_EXCLUDES;

		/**
		 * Additional patterns that should be excluded from triggering a full restart.
		 */
		private String additionalExclude;

		/**
		 * Amount of time to wait between polling for classpath changes.
		 */
		private Duration pollInterval = Duration.ofSeconds(1);

		/**
		 * Amount of quiet time required without any classpath changes before a restart is
		 * triggered.
		 */
		private Duration quietPeriod = Duration.ofMillis(400);

		/**
		 * Name of a specific file that, when changed, triggers the restart check. Must be
		 * a simple name (without any path) of a file that appears on your classpath. If
		 * not specified, any classpath file change triggers the restart.
		 */
		private String triggerFile;

		/**
		 * Additional paths to watch for changes.
		 */
		private List<File> additionalPaths = new ArrayList<>();

		/**
		 * Whether to log the condition evaluation delta upon restart.
		 */
		private boolean logConditionEvaluationDelta = true;

		/**
		 * Returns the current status of the enabled flag.
		 * @return true if the enabled flag is set, false otherwise.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of the Restart object.
		 * @param enabled the new enabled status to be set
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Retrieves all exclude values.
		 * @return an array of strings containing all exclude values
		 */
		public String[] getAllExclude() {
			List<String> allExclude = new ArrayList<>();
			if (StringUtils.hasText(this.exclude)) {
				allExclude.addAll(StringUtils.commaDelimitedListToSet(this.exclude));
			}
			if (StringUtils.hasText(this.additionalExclude)) {
				allExclude.addAll(StringUtils.commaDelimitedListToSet(this.additionalExclude));
			}
			return StringUtils.toStringArray(allExclude);
		}

		/**
		 * Returns the value of the exclude property.
		 * @return the value of the exclude property
		 */
		public String getExclude() {
			return this.exclude;
		}

		/**
		 * Sets the exclude value for the Restart class.
		 * @param exclude the exclude value to be set
		 */
		public void setExclude(String exclude) {
			this.exclude = exclude;
		}

		/**
		 * Returns the additional exclude value.
		 * @return the additional exclude value
		 */
		public String getAdditionalExclude() {
			return this.additionalExclude;
		}

		/**
		 * Sets the additional exclude value.
		 * @param additionalExclude the additional exclude value to be set
		 */
		public void setAdditionalExclude(String additionalExclude) {
			this.additionalExclude = additionalExclude;
		}

		/**
		 * Returns the poll interval for restarting.
		 * @return the poll interval for restarting
		 */
		public Duration getPollInterval() {
			return this.pollInterval;
		}

		/**
		 * Sets the poll interval for restarting.
		 * @param pollInterval the duration between each restart attempt
		 */
		public void setPollInterval(Duration pollInterval) {
			this.pollInterval = pollInterval;
		}

		/**
		 * Returns the quiet period duration.
		 * @return the quiet period duration
		 */
		public Duration getQuietPeriod() {
			return this.quietPeriod;
		}

		/**
		 * Sets the quiet period for restarting.
		 * @param quietPeriod the duration of the quiet period
		 */
		public void setQuietPeriod(Duration quietPeriod) {
			this.quietPeriod = quietPeriod;
		}

		/**
		 * Returns the trigger file path.
		 * @return the trigger file path
		 */
		public String getTriggerFile() {
			return this.triggerFile;
		}

		/**
		 * Sets the trigger file for restarting the application.
		 * @param triggerFile the path of the trigger file
		 */
		public void setTriggerFile(String triggerFile) {
			this.triggerFile = triggerFile;
		}

		/**
		 * Returns the additional paths.
		 * @return the additional paths as a List of File objects
		 */
		public List<File> getAdditionalPaths() {
			return this.additionalPaths;
		}

		/**
		 * Sets the additional paths for the Restart class.
		 * @param additionalPaths the list of additional paths to be set
		 */
		public void setAdditionalPaths(List<File> additionalPaths) {
			this.additionalPaths = additionalPaths;
		}

		/**
		 * Returns the value of the logConditionEvaluationDelta field.
		 * @return true if condition evaluation delta is logged, false otherwise
		 */
		public boolean isLogConditionEvaluationDelta() {
			return this.logConditionEvaluationDelta;
		}

		/**
		 * Sets the flag to enable or disable logging of condition evaluation delta.
		 * @param logConditionEvaluationDelta true to enable logging, false to disable
		 * logging
		 */
		public void setLogConditionEvaluationDelta(boolean logConditionEvaluationDelta) {
			this.logConditionEvaluationDelta = logConditionEvaluationDelta;
		}

	}

	/**
	 * LiveReload properties.
	 */
	public static class Livereload {

		/**
		 * Whether to enable a livereload.com-compatible server.
		 */
		private boolean enabled = true;

		/**
		 * Server port.
		 */
		private int port = 35729;

		/**
		 * Returns the current status of the Livereload feature.
		 * @return true if Livereload is enabled, false otherwise
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of Livereload.
		 * @param enabled the enabled status to be set
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Returns the port number used by the Livereload server.
		 * @return the port number
		 */
		public int getPort() {
			return this.port;
		}

		/**
		 * Sets the port number for the Livereload server.
		 * @param port the port number to set
		 */
		public void setPort(int port) {
			this.port = port;
		}

	}

}
