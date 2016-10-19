/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.h2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for H2's TCP server.
 * Created by lvasek on 19/10/2016.
 */
@ConfigurationProperties(prefix = "spring.h2.tcp")
public class H2TcpServerProperties {

	/**
	 * Enable the tcp server.
	 */
	private boolean enabled = false;
	/**
	 * Allow other computers to connect.
	 */
	private boolean allowOthers = false;

	/**
	 * Use a daemon thread.
	 */
	private boolean daemonThread = false;

	/**
	 * The port to use for tcp server.
	 */
	private String port = "9092";

	/**
	 * Use encrypted (SSL) connections.
	 */
	private boolean useSsl = false;

	/**
	 * The password for shutting down a TCP server.
	 */
	private String shutdownPassword;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isAllowOthers() {
		return this.allowOthers;
	}

	public void setAllowOthers(boolean allowOthers) {
		this.allowOthers = allowOthers;
	}

	public boolean isDaemonThread() {
		return this.daemonThread;
	}

	public void setDaemonThread(boolean daemonThread) {
		this.daemonThread = daemonThread;
	}

	public String getPort() {
		return this.port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public boolean isUseSsl() {
		return this.useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	public String getShutdownPassword() {
		return this.shutdownPassword;
	}

	public void setShutdownPassword(String shutdownPassword) {
		this.shutdownPassword = shutdownPassword;
	}
}
