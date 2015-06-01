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

/**
 * Configuration properties for remote Spring Boot applications.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 * @see DeveloperToolsProperties
 */
public class RemoteDeveloperToolsProperties {

	public static final String DEFAULT_CONTEXT_PATH = "/.~~spring-boot!~";

	/**
	 * Context path used to handle the remote connection.
	 */
	private String contextPath = DEFAULT_CONTEXT_PATH;

	private Restart restart = new Restart();

	private Debug debug = new Debug();

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public Restart getRestart() {
		return this.restart;
	}

	public Debug getDebug() {
		return this.debug;
	}

	public static class Restart {

		/**
		 * Enable remote restart
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Debug {

		public static final Integer DEFAULT_LOCAL_PORT = 8000;

		/**
		 * Enable remote debug support.
		 */
		private boolean enabled = true;

		/**
		 * Local remote debug server port.
		 */
		private int localPort = DEFAULT_LOCAL_PORT;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getLocalPort() {
			return this.localPort;
		}

		public void setLocalPort(int localPort) {
			this.localPort = localPort;
		}

	}

}
