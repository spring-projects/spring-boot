/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Rabbit.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitProperties {

	private String host = "localhost";

	private int port = 5672;

	private String username;

	private String password;

	private String virtualHost;

	private String addresses;

	private boolean dynamic = true;

	public String getHost() {
		if (this.addresses == null) {
			return this.host;
		}
		String[] hosts = StringUtils.delimitedListToStringArray(this.addresses, ":");
		if (hosts.length == 2) {
			return hosts[0];
		}
		return null;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		if (this.addresses == null) {
			return this.port;
		}
		String[] hosts = StringUtils.delimitedListToStringArray(this.addresses, ":");
		if (hosts.length >= 2) {
			return Integer
					.valueOf(StringUtils.commaDelimitedListToStringArray(hosts[1])[0]);
		}
		return this.port;
	}

	public void setAddresses(String addresses) {
		this.addresses = addresses;
	}

	public String getAddresses() {
		return (this.addresses == null ? this.host + ":" + this.port : this.addresses);
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isDynamic() {
		return this.dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public String getVirtualHost() {
		return this.virtualHost;
	}

	public void setVirtualHost(String virtualHost) {
		while (virtualHost.startsWith("/") && virtualHost.length() > 0) {
			virtualHost = virtualHost.substring(1);
		}
		this.virtualHost = ("".equals(virtualHost) ? "/" : virtualHost);

	}

}
