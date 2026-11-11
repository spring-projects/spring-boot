/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

/**
 * Adapts {@link PulsarProperties} to {@link PulsarConnectionDetails}.
 *
 * @author Chris Bono
 */
class PropertiesPulsarConnectionDetails implements PulsarConnectionDetails {

	private final PulsarProperties pulsarProperties;

	PropertiesPulsarConnectionDetails(PulsarProperties pulsarProperties) {
		this.pulsarProperties = pulsarProperties;
	}

	@Override
	public String getBrokerUrl() {
		return this.pulsarProperties.getClient().getServiceUrl();
	}

	@Override
	public String getAdminUrl() {
		return this.pulsarProperties.getAdmin().getServiceUrl();
	}

}
