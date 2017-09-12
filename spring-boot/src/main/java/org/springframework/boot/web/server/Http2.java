/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.server;

import java.net.InetAddress;

/**
 * Simple server-independent abstraction for HTTP/2 configuration.
 *
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class Http2 {

	/**
	 * Enable HTTP/2 support.
	 */
	private boolean enabled = false;

	/**
	 * HTTP/2 port.
	 */
	private Integer port;

	/**
	 * Network address to which the HTTP/2 connector should be bind to.
	 */
	private InetAddress address;

	/**
	 * Activate alpn debug.
	 */
	private Boolean alpnDebug;

	/**
	 * Ssl options for HTTP/2 connector.
	 */
	private Ssl ssl;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public Boolean getAlpnDebug() {
		return this.alpnDebug;
	}

	public void setAlpnDebug(Boolean alpnDebug) {
		this.alpnDebug = alpnDebug;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

}
