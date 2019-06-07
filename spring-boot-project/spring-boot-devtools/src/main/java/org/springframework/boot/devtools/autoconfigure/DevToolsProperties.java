/*
 * Copyright 2012-2019 the original author or authors.
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

	private Restart restart = new Restart();

	private Livereload livereload = new Livereload();

	@NestedConfigurationProperty
	private final RemoteDevToolsProperties remote = new RemoteDevToolsProperties();

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
		 * Name of a specific file that, when changed, triggers the restart check. If not
		 * specified, any classpath file change triggers the restart.
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

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

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

		public String getExclude() {
			return this.exclude;
		}

		public void setExclude(String exclude) {
			this.exclude = exclude;
		}

		public String getAdditionalExclude() {
			return this.additionalExclude;
		}

		public void setAdditionalExclude(String additionalExclude) {
			this.additionalExclude = additionalExclude;
		}

		public Duration getPollInterval() {
			return this.pollInterval;
		}

		public void setPollInterval(Duration pollInterval) {
			this.pollInterval = pollInterval;
		}

		public Duration getQuietPeriod() {
			return this.quietPeriod;
		}

		public void setQuietPeriod(Duration quietPeriod) {
			this.quietPeriod = quietPeriod;
		}

		public String getTriggerFile() {
			return this.triggerFile;
		}

		public void setTriggerFile(String triggerFile) {
			this.triggerFile = triggerFile;
		}

		public List<File> getAdditionalPaths() {
			return this.additionalPaths;
		}

		public void setAdditionalPaths(List<File> additionalPaths) {
			this.additionalPaths = additionalPaths;
		}

		public boolean isLogConditionEvaluationDelta() {
			return this.logConditionEvaluationDelta;
		}

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
