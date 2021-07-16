/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace;

import java.util.HashMap;

import io.micrometer.dynatrace.DynatraceApiVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynatracePropertiesConfigAdapter}.
 *
 * @author Andy Wilkinson
 * @author Georg Pirklbauer
 */
class DynatracePropertiesConfigAdapterTests {

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setUri("https://dynatrace.example.com");
		assertThat(new DynatracePropertiesConfigAdapter(properties).uri()).isEqualTo("https://dynatrace.example.com");
	}

	@Test
	void whenPropertiesApiTokenIsSetAdapterApiTokenReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setApiToken("123ABC");
		assertThat(new DynatracePropertiesConfigAdapter(properties).apiToken()).isEqualTo("123ABC");
	}

	@Test
	void whenPropertiesDeviceIdIsSetAdapterDeviceIdReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setDeviceId("dev-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).deviceId()).isEqualTo("dev-1");
	}

	@Test
	void whenPropertiesTechnologyTypeIsSetAdapterTechnologyTypeReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setTechnologyType("tech-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).technologyType()).isEqualTo("tech-1");
	}

	@Test
	void whenPropertiesGroupIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setGroup("group-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).group()).isEqualTo("group-1");
	}

	@Test
	void whenPropertiesApiVersionIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setApiVersion(DynatraceApiVersion.V1);
		assertThat(new DynatracePropertiesConfigAdapter(properties).apiVersion()).isSameAs(DynatraceApiVersion.V1);
	}

	@Test
	void whenPropertiesMetricKeyPrefixIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setMetricKeyPrefix("my.prefix");
		assertThat(new DynatracePropertiesConfigAdapter(properties).metricKeyPrefix()).isEqualTo("my.prefix");
	}

	@Test
	void whenPropertiesEnrichWithOneAgentMetadataIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setEnrichWithDynatraceMetadata(true);
		assertThat(new DynatracePropertiesConfigAdapter(properties).enrichWithDynatraceMetadata()).isTrue();
	}

	@Test
	void whenPropertiesDefaultDimensionsIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		HashMap<String, String> defaultDimensions = new HashMap<String, String>() {
			{
				put("dim1", "value1");
				put("dim2", "value2");
			}
		};

		properties.setDefaultDimensions(defaultDimensions);
		assertThat(new DynatracePropertiesConfigAdapter(properties).defaultDimensions())
				.containsExactlyEntriesOf(defaultDimensions);
	}

	@Test
	void defaultValues() {
		DynatraceProperties properties = new DynatraceProperties();
		assertThat(properties.getApiToken()).isNull();
		assertThat(properties.getDeviceId()).isNull();
		assertThat(properties.getTechnologyType()).isEqualTo("java");
		assertThat(properties.getUri()).isNull();
		assertThat(properties.getGroup()).isNull();
		assertThat(properties.getApiVersion()).isSameAs(DynatraceApiVersion.V1);
		assertThat(properties.getMetricKeyPrefix()).isNull();
		assertThat(properties.getEnrichWithDynatraceMetadata()).isTrue();
		assertThat(properties.getDefaultDimensions()).isNull();
	}

}
