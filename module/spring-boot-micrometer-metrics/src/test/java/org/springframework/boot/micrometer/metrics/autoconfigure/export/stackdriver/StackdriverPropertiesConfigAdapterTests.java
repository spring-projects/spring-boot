/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.stackdriver;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StackdriverPropertiesConfigAdapter}.
 *
 * @author Johannes Graf
 */
class StackdriverPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<StackdriverProperties, StackdriverPropertiesConfigAdapter> {

	StackdriverPropertiesConfigAdapterTests() {
		super(StackdriverPropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesProjectIdIsSetAdapterProjectIdReturnsIt() {
		StackdriverProperties properties = new StackdriverProperties();
		properties.setProjectId("my-gcp-project-id");
		assertThat(new StackdriverPropertiesConfigAdapter(properties).projectId()).isEqualTo("my-gcp-project-id");
	}

	@Test
	void whenPropertiesResourceTypeIsSetAdapterResourceTypeReturnsIt() {
		StackdriverProperties properties = new StackdriverProperties();
		properties.setResourceType("my-resource-type");
		assertThat(new StackdriverPropertiesConfigAdapter(properties).resourceType()).isEqualTo("my-resource-type");
	}

	@Test
	void whenPropertiesResourceLabelsAreSetAdapterResourceLabelsReturnsThem() {
		final Map<String, String> labels = new HashMap<>();
		labels.put("labelOne", "valueOne");
		labels.put("labelTwo", "valueTwo");
		StackdriverProperties properties = new StackdriverProperties();
		properties.setResourceLabels(labels);
		assertThat(new StackdriverPropertiesConfigAdapter(properties).resourceLabels())
			.containsExactlyInAnyOrderEntriesOf(labels);
	}

	@Test
	void whenPropertiesUseSemanticMetricTypesIsSetAdapterUseSemanticMetricTypesReturnsIt() {
		StackdriverProperties properties = new StackdriverProperties();
		properties.setUseSemanticMetricTypes(true);
		assertThat(new StackdriverPropertiesConfigAdapter(properties).useSemanticMetricTypes()).isTrue();
	}

	@Test
	void whenPropertiesMetricTypePrefixIsSetAdapterMetricTypePrefixReturnsIt() {
		StackdriverProperties properties = new StackdriverProperties();
		properties.setMetricTypePrefix("external.googleapis.com/prometheus");
		assertThat(new StackdriverPropertiesConfigAdapter(properties).metricTypePrefix())
			.isEqualTo("external.googleapis.com/prometheus");
	}

	@Test
	void whenPropertiesAutoCreateMetricDescriptorsIsSetAdapterAutoCreateMetricDescriptorsReturnsIt() {
		StackdriverProperties properties = new StackdriverProperties();
		properties.setAutoCreateMetricDescriptors(false);
		assertThat(new StackdriverPropertiesConfigAdapter(properties).autoCreateMetricDescriptors()).isFalse();
	}

	@Test
	@Override
	protected void adapterOverridesAllConfigMethods() {
		adapterOverridesAllConfigMethodsExcept("credentials");
	}

}
