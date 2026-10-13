/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.annotationprocessor.automaticmetadatageneration.nestedproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("my.server")
public class MyServerProperties {

	private String name;

	private Host host;

	// @fold:on // getters/setters ...
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Host getHost() {
		return this.host;
	}

	public void setHost(Host host) {
		this.host = host;
	}
	// @fold:off

	public static class Host {

		private String ip;

		private int port;

		// @fold:on // getters/setters ...
		public String getIp() {
			return this.ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}
		// @fold:off

	}

}
