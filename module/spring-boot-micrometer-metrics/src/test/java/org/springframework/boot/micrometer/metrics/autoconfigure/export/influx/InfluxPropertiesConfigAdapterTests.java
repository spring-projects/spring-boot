/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.influx;

import io.micrometer.influx.InfluxApiVersion;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxPropertiesConfigAdapter}.
 *
 * @author Stephane Nicoll
 */
class InfluxPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<InfluxProperties, InfluxPropertiesConfigAdapter> {

	InfluxPropertiesConfigAdapterTests() {
		super(InfluxPropertiesConfigAdapter.class);
	}

	@Test
	void adaptInfluxV1BasicConfig() {
		InfluxProperties properties = new InfluxProperties();
		properties.setDb("test-db");
		properties.setUri("https://influx.example.com:8086");
		properties.setUserName("user");
		properties.setPassword("secret");
		InfluxPropertiesConfigAdapter adapter = new InfluxPropertiesConfigAdapter(properties);
		assertThat(adapter.apiVersion()).isEqualTo(InfluxApiVersion.V1);
		assertThat(adapter.db()).isEqualTo("test-db");
		assertThat(adapter.uri()).isEqualTo("https://influx.example.com:8086");
		assertThat(adapter.userName()).isEqualTo("user");
		assertThat(adapter.password()).isEqualTo("secret");
	}

	@Test
	void adaptInfluxV2BasicConfig() {
		InfluxProperties properties = new InfluxProperties();
		properties.setOrg("test-org");
		properties.setBucket("test-bucket");
		properties.setUri("https://influx.example.com:8086");
		properties.setToken("token");
		InfluxPropertiesConfigAdapter adapter = new InfluxPropertiesConfigAdapter(properties);
		assertThat(adapter.apiVersion()).isEqualTo(InfluxApiVersion.V2);
		assertThat(adapter.org()).isEqualTo("test-org");
		assertThat(adapter.bucket()).isEqualTo("test-bucket");
		assertThat(adapter.uri()).isEqualTo("https://influx.example.com:8086");
		assertThat(adapter.token()).isEqualTo("token");
	}

}
