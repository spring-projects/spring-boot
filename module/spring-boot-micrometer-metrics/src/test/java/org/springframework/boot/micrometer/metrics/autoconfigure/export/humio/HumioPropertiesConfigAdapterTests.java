/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.humio;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HumioPropertiesConfigAdapter}.
 *
 * @author Andy Wilkinson
 */
class HumioPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<HumioProperties, HumioPropertiesConfigAdapter> {

	HumioPropertiesConfigAdapterTests() {
		super(HumioPropertiesConfigAdapter.class);
	}

	@Test
	void whenApiTokenIsSetAdapterApiTokenReturnsIt() {
		HumioProperties properties = new HumioProperties();
		properties.setApiToken("ABC123");
		assertThat(new HumioPropertiesConfigAdapter(properties).apiToken()).isEqualTo("ABC123");
	}

	@Test
	void whenPropertiesTagsIsSetAdapterTagsReturnsIt() {
		HumioProperties properties = new HumioProperties();
		properties.setTags(Collections.singletonMap("name", "test"));
		assertThat(new HumioPropertiesConfigAdapter(properties).tags())
			.isEqualTo(Collections.singletonMap("name", "test"));
	}

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		HumioProperties properties = new HumioProperties();
		properties.setUri("https://humio.example.com");
		assertThat(new HumioPropertiesConfigAdapter(properties).uri()).isEqualTo("https://humio.example.com");
	}

}
