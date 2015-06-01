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

package org.springframework.boot.developertools.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for developer tools.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.developertools")
public class DeveloperToolsProperties {

	private static final String DEFAULT_RESTART_EXCLUDES = "META-INF/resources/**,resource/**,static/**,public/**,templates/**";

	private Restart restart = new Restart();

	private Livereload livereload = new Livereload();

	public Restart getRestart() {
		return this.restart;
	}

	public Livereload getLivereload() {
		return this.livereload;
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
