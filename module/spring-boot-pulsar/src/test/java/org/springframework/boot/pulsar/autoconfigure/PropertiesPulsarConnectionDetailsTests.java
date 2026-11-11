/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesPulsarConnectionDetails}.
 *
 * @author Chris Bono
 */
class PropertiesPulsarConnectionDetailsTests {

	@Test
	void getClientServiceUrlReturnsValueFromProperties() {
		PulsarProperties properties = new PulsarProperties();
		properties.getClient().setServiceUrl("foo");
		PulsarConnectionDetails connectionDetails = new PropertiesPulsarConnectionDetails(properties);
		assertThat(connectionDetails.getBrokerUrl()).isEqualTo("foo");
	}

	@Test
	void getAdminServiceHttpUrlReturnsValueFromProperties() {
		PulsarProperties properties = new PulsarProperties();
		properties.getAdmin().setServiceUrl("foo");
		PulsarConnectionDetails connectionDetails = new PropertiesPulsarConnectionDetails(properties);
		assertThat(connectionDetails.getAdminUrl()).isEqualTo("foo");
	}

}
