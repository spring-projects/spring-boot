/*
 * Copyright 2012-2022 the original author or authors.
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
	@Deprecated
	void whenPropertiesDeviceIdIsSetAdapterDeviceIdReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setDeviceId("dev-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).deviceId()).isEqualTo("dev-1");
	}

	@Test
	void whenPropertiesV1DeviceIdIsSetAdapterDeviceIdReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.getV1().setDeviceId("dev-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).deviceId()).isEqualTo("dev-1");
	}

	@Test
	@Deprecated
	void whenPropertiesTechnologyTypeIsSetAdapterTechnologyTypeReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setTechnologyType("tech-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).technologyType()).isEqualTo("tech-1");
	}

	@Test
	void whenPropertiesV1TechnologyTypeIsSetAdapterTechnologyTypeReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.getV1().setTechnologyType("tech-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).technologyType()).isEqualTo("tech-1");
	}

	@Test
	@Deprecated
	void whenPropertiesGroupIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setGroup("group-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).group()).isEqualTo("group-1");
	}

	@Test
	void whenPropertiesV1GroupIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.getV1().setGroup("group-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).group()).isEqualTo("group-1");
	}

	@Test
	@SuppressWarnings("deprecation")
	void whenDeviceIdIsSetThenAdapterApiVersionIsV1() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setDeviceId("dev-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).apiVersion()).isSameAs(DynatraceApiVersion.V1);
	}

	@Test
	void whenV1DeviceIdIsSetThenAdapterApiVersionIsV1() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.getV1().setDeviceId("dev-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).apiVersion()).isSameAs(DynatraceApiVersion.V1);
	}

	@Test
	void whenDeviceIdIsNotSetThenAdapterApiVersionIsV2() {
		DynatraceProperties properties = new DynatraceProperties();
		assertThat(new DynatracePropertiesConfigAdapter(properties).apiVersion()).isSameAs(DynatraceApiVersion.V2);
	}

	@Test
	void whenPropertiesMetricKeyPrefixIsSetAdapterMetricKeyPrefixReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.getV2().setMetricKeyPrefix("my.prefix");
		assertThat(new DynatracePropertiesConfigAdapter(properties).metricKeyPrefix()).isEqualTo("my.prefix");
	}

	@Test
	void whenPropertiesEnrichWithOneAgentMetadataIsSetAdapterEnrichWithOneAgentMetadataReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.getV2().setEnrichWithDynatraceMetadata(true);
		assertThat(new DynatracePropertiesConfigAdapter(properties).enrichWithDynatraceMetadata()).isTrue();
	}

	@Test
	void whenPropertiesUseDynatraceInstrumentsIsSetAdapterUseDynatraceInstrumentsReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.getV2().setUseDynatraceSummaryInstruments(false);
		assertThat(new DynatracePropertiesConfigAdapter(properties).useDynatraceSummaryInstruments()).isFalse();
	}

	@Test
	void whenPropertiesDefaultDimensionsIsSetAdapterDefaultDimensionsReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		HashMap<String, String> defaultDimensions = new HashMap<>();
		defaultDimensions.put("dim1", "value1");
		defaultDimensions.put("dim2", "value2");
		properties.getV2().setDefaultDimensions(defaultDimensions);
		assertThat(new DynatracePropertiesConfigAdapter(properties).defaultDimensions())
				.containsExactlyEntriesOf(defaultDimensions);
	}

	@Test
	@SuppressWarnings("deprecation")
	void defaultValues() {
		DynatraceProperties properties = new DynatraceProperties();
		assertThat(properties.getApiToken()).isNull();
		assertThat(properties.getUri()).isNull();
		assertThat(properties.getV1().getDeviceId()).isNull();
		assertThat(properties.getV1().getTechnologyType()).isEqualTo("java");
		assertThat(properties.getV1().getGroup()).isNull();
		assertThat(properties.getV2().getMetricKeyPrefix()).isNull();
		assertThat(properties.getV2().isEnrichWithDynatraceMetadata()).isTrue();
		assertThat(properties.getV2().getDefaultDimensions()).isNull();
		assertThat(properties.getV2().isUseDynatraceSummaryInstruments()).isTrue();
		assertThat(properties.getDeviceId()).isNull();
		assertThat(properties.getTechnologyType()).isEqualTo("java");
		assertThat(properties.getGroup()).isNull();
	}

}
