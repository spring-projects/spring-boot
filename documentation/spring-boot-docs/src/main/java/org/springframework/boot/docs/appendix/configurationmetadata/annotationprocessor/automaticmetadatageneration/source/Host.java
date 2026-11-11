/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.annotationprocessor.automaticmetadatageneration.source;

import org.springframework.boot.context.properties.ConfigurationPropertiesSource;

@ConfigurationPropertiesSource
public class Host {

	/**
	 * IP address to listen to.
	 */
	private String ip = "127.0.0.1";

	/**
	 * Port to listener to.
	 */
	private int port = 9797;

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
