/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.lifecycle;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogLevel;

/**
 * Configuration properties for Docker Compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
@ConfigurationProperties(DockerComposeProperties.NAME)
public class DockerComposeProperties {

	static final String NAME = "spring.docker.compose";

	/**
	 * Whether docker compose support is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Path to a specific docker compose configuration file.
	 */
	private File file;

	/**
	 * Docker compose lifecycle management.
	 */
	private LifecycleManagement lifecycleManagement = LifecycleManagement.START_AND_STOP;

	/**
	 * Hostname or IP of the machine where the docker containers are started.
	 */
	private String host;

	/**
	 * Start configuration.
	 */
	private final Start start = new Start();

	/**
	 * Stop configuration.
	 */
	private final Stop stop = new Stop();

	/**
	 * Profiles configuration.
	 */
	private final Profiles profiles = new Profiles();

	private final Skip skip = new Skip();

	private final Readiness readiness = new Readiness();

	/**
     * Returns the current status of the Docker Compose properties.
     * 
     * @return true if the Docker Compose properties are enabled, false otherwise.
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the DockerComposeProperties.
     * 
     * @param enabled the enabled status to be set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Returns the file associated with this DockerComposeProperties object.
     *
     * @return the file associated with this DockerComposeProperties object
     */
    public File getFile() {
		return this.file;
	}

	/**
     * Sets the file for the DockerComposeProperties.
     * 
     * @param file the file to be set
     */
    public void setFile(File file) {
		this.file = file;
	}

	/**
     * Returns the LifecycleManagement object associated with this DockerComposeProperties instance.
     *
     * @return the LifecycleManagement object
     */
    public LifecycleManagement getLifecycleManagement() {
		return this.lifecycleManagement;
	}

	/**
     * Sets the lifecycle management for the DockerComposeProperties.
     * 
     * @param lifecycleManagement the lifecycle management to be set
     */
    public void setLifecycleManagement(LifecycleManagement lifecycleManagement) {
		this.lifecycleManagement = lifecycleManagement;
	}

	/**
     * Returns the host value.
     *
     * @return the host value
     */
    public String getHost() {
		return this.host;
	}

	/**
     * Sets the host for the DockerComposeProperties.
     * 
     * @param host the host to be set
     */
    public void setHost(String host) {
		this.host = host;
	}

	/**
     * Returns the start property of the DockerComposeProperties class.
     *
     * @return the start property of the DockerComposeProperties class
     */
    public Start getStart() {
		return this.start;
	}

	/**
     * Returns the stop object.
     *
     * @return the stop object
     */
    public Stop getStop() {
		return this.stop;
	}

	/**
     * Returns the profiles object.
     *
     * @return the profiles object
     */
    public Profiles getProfiles() {
		return this.profiles;
	}

	/**
     * Returns the skip property of the DockerComposeProperties object.
     * 
     * @return the skip property of the DockerComposeProperties object
     */
    public Skip getSkip() {
		return this.skip;
	}

	/**
     * Returns the readiness of the DockerComposeProperties.
     *
     * @return the readiness of the DockerComposeProperties
     */
    public Readiness getReadiness() {
		return this.readiness;
	}

	/**
     * Retrieves the DockerComposeProperties object from the given Binder.
     * If the DockerComposeProperties object is not found in the Binder, a new instance is created.
     * 
     * @param binder the Binder object used for retrieving the DockerComposeProperties object
     * @return the DockerComposeProperties object retrieved from the Binder, or a new instance if not found
     */
    static DockerComposeProperties get(Binder binder) {
		return binder.bind(NAME, DockerComposeProperties.class).orElseGet(DockerComposeProperties::new);
	}

	/**
	 * Start properties.
	 */
	public static class Start {

		/**
		 * Command used to start docker compose.
		 */
		private StartCommand command = StartCommand.UP;

		/**
		 * Log level for output.
		 */
		private LogLevel logLevel = LogLevel.INFO;

		/**
         * Returns the command associated with the StartCommand object.
         *
         * @return the command associated with the StartCommand object
         */
        public StartCommand getCommand() {
			return this.command;
		}

		/**
         * Sets the command for the Start class.
         * 
         * @param command the StartCommand object to be set
         */
        public void setCommand(StartCommand command) {
			this.command = command;
		}

		/**
         * Returns the log level of the Start class.
         * 
         * @return the log level of the Start class
         */
        public LogLevel getLogLevel() {
			return this.logLevel;
		}

		/**
         * Sets the log level for the application.
         * 
         * @param logLevel the log level to be set
         */
        public void setLogLevel(LogLevel logLevel) {
			this.logLevel = logLevel;
		}

	}

	/**
	 * Stop properties.
	 */
	public static class Stop {

		/**
		 * Command used to stop docker compose.
		 */
		private StopCommand command = StopCommand.STOP;

		/**
		 * Timeout for stopping Docker Compose. Use '0' for forced stop.
		 */
		private Duration timeout = Duration.ofSeconds(10);

		/**
         * Returns the StopCommand associated with this Stop object.
         *
         * @return the StopCommand associated with this Stop object
         */
        public StopCommand getCommand() {
			return this.command;
		}

		/**
         * Sets the command for stopping.
         * 
         * @param command the StopCommand object to be set
         */
        public void setCommand(StopCommand command) {
			this.command = command;
		}

		/**
         * Returns the timeout duration.
         *
         * @return the timeout duration
         */
        public Duration getTimeout() {
			return this.timeout;
		}

		/**
         * Sets the timeout for the Stop class.
         * 
         * @param timeout the duration of the timeout
         */
        public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

	/**
	 * Profiles properties.
	 */
	public static class Profiles {

		/**
		 * Docker compose profiles that should be active.
		 */
		private Set<String> active = new LinkedHashSet<>();

		/**
         * Returns the set of active profiles.
         *
         * @return the set of active profiles
         */
        public Set<String> getActive() {
			return this.active;
		}

		/**
         * Sets the active profiles.
         * 
         * @param active the active profiles to be set
         */
        public void setActive(Set<String> active) {
			this.active = active;
		}

	}

	/**
	 * Skip options.
	 */
	public static class Skip {

		/**
		 * Whether to skip in tests.
		 */
		private boolean inTests = true;

		/**
         * Returns a boolean value indicating whether the object is in tests.
         * 
         * @return true if the object is in tests, false otherwise
         */
        public boolean isInTests() {
			return this.inTests;
		}

		/**
         * Sets the value indicating whether the code is running in a test environment.
         * 
         * @param inTests the value indicating whether the code is running in a test environment
         */
        public void setInTests(boolean inTests) {
			this.inTests = inTests;
		}

	}

	/**
	 * Readiness properties.
	 */
	public static class Readiness {

		/**
		 * Wait strategy to use.
		 */
		private Wait wait = Wait.ALWAYS;

		/**
		 * Timeout of the readiness checks.
		 */
		private Duration timeout = Duration.ofMinutes(2);

		/**
		 * TCP properties.
		 */
		private final Tcp tcp = new Tcp();

		/**
         * Returns the wait object associated with this Readiness instance.
         *
         * @return the wait object
         */
        public Wait getWait() {
			return this.wait;
		}

		/**
         * Sets the wait object for the Readiness class.
         * 
         * @param wait the wait object to be set
         */
        public void setWait(Wait wait) {
			this.wait = wait;
		}

		/**
         * Returns the timeout duration for the readiness check.
         *
         * @return the timeout duration
         */
        public Duration getTimeout() {
			return this.timeout;
		}

		/**
         * Sets the timeout for the Readiness class.
         * 
         * @param timeout the duration of the timeout
         */
        public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		/**
         * Returns the Tcp object associated with this Readiness object.
         *
         * @return the Tcp object associated with this Readiness object
         */
        public Tcp getTcp() {
			return this.tcp;
		}

		/**
		 * Readiness wait strategies.
		 */
		public enum Wait {

			/**
			 * Always perform readiness checks.
			 */
			ALWAYS,

			/**
			 * Never perform readiness checks.
			 */
			NEVER,

			/**
			 * Only perform readiness checks if docker was started with lifecycle
			 * management.
			 */
			ONLY_IF_STARTED

		}

		/**
		 * TCP properties.
		 */
		public static class Tcp {

			/**
			 * Timeout for connections.
			 */
			private Duration connectTimeout = Duration.ofMillis(200);

			/**
			 * Timeout for reads.
			 */
			private Duration readTimeout = Duration.ofMillis(200);

			/**
             * Returns the connect timeout duration.
             *
             * @return the connect timeout duration
             */
            public Duration getConnectTimeout() {
				return this.connectTimeout;
			}

			/**
             * Sets the connection timeout for the TCP connection.
             * 
             * @param connectTimeout the duration of the connection timeout
             */
            public void setConnectTimeout(Duration connectTimeout) {
				this.connectTimeout = connectTimeout;
			}

			/**
             * Returns the read timeout for the TCP connection.
             *
             * @return the read timeout duration
             */
            public Duration getReadTimeout() {
				return this.readTimeout;
			}

			/**
             * Sets the read timeout for the TCP connection.
             * 
             * @param readTimeout the duration of the read timeout
             */
            public void setReadTimeout(Duration readTimeout) {
				this.readTimeout = readTimeout;
			}

		}

	}

}
