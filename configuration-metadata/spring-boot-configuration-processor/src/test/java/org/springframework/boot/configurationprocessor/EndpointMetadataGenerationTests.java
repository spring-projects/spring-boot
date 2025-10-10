/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.TestAccess;
import org.springframework.boot.configurationsample.endpoint.CamelCaseEndpoint;
import org.springframework.boot.configurationsample.endpoint.CustomPropertiesEndpoint;
import org.springframework.boot.configurationsample.endpoint.EnabledEndpoint;
import org.springframework.boot.configurationsample.endpoint.NoAccessEndpoint;
import org.springframework.boot.configurationsample.endpoint.NullableParameterEndpoint;
import org.springframework.boot.configurationsample.endpoint.ReadOnlyAccessEndpoint;
import org.springframework.boot.configurationsample.endpoint.SimpleEndpoint;
import org.springframework.boot.configurationsample.endpoint.SimpleEndpoint2;
import org.springframework.boot.configurationsample.endpoint.SimpleEndpoint3;
import org.springframework.boot.configurationsample.endpoint.SpecificEndpoint;
import org.springframework.boot.configurationsample.endpoint.UnrestrictedAccessEndpoint;
import org.springframework.boot.configurationsample.endpoint.incremental.IncrementalEndpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Metadata generation tests for Actuator endpoints.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Wonyong Hwang
 */
class EndpointMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void simpleEndpoint() {
		ConfigurationMetadata metadata = compile(SimpleEndpoint.class);
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.simple").fromSource(SimpleEndpoint.class));
		assertThat(metadata).has(access("simple", TestAccess.UNRESTRICTED));
		assertThat(metadata).has(cacheTtl("simple"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void enabledEndpoint() {
		ConfigurationMetadata metadata = compile(EnabledEndpoint.class);
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.enabled").fromSource(EnabledEndpoint.class));
		assertThat(metadata).has(access("enabled", TestAccess.UNRESTRICTED));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void noAccessEndpoint() {
		ConfigurationMetadata metadata = compile(NoAccessEndpoint.class);
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.noaccess").fromSource(NoAccessEndpoint.class));
		assertThat(metadata).has(access("noaccess", TestAccess.NONE));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void readOnlyAccessEndpoint() {
		ConfigurationMetadata metadata = compile(ReadOnlyAccessEndpoint.class);
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.readonlyaccess").fromSource(ReadOnlyAccessEndpoint.class));
		assertThat(metadata).has(access("readonlyaccess", TestAccess.READ_ONLY));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void unrestrictedAccessEndpoint() {
		ConfigurationMetadata metadata = compile(UnrestrictedAccessEndpoint.class);
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.unrestrictedaccess")
			.fromSource(UnrestrictedAccessEndpoint.class));
		assertThat(metadata).has(access("unrestrictedaccess", TestAccess.UNRESTRICTED));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void customPropertiesEndpoint() {
		ConfigurationMetadata metadata = compile(CustomPropertiesEndpoint.class);
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.customprops").fromSource(CustomPropertiesEndpoint.class));
		assertThat(metadata).has(Metadata.withProperty("management.endpoint.customprops.name")
			.ofType(String.class)
			.withDefaultValue("test"));
		assertThat(metadata).has(access("customprops", TestAccess.UNRESTRICTED));
		assertThat(metadata).has(cacheTtl("customprops"));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	void specificEndpoint() {
		ConfigurationMetadata metadata = compile(SpecificEndpoint.class);
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.specific").fromSource(SpecificEndpoint.class));
		assertThat(metadata).has(access("specific", TestAccess.READ_ONLY));
		assertThat(metadata).has(cacheTtl("specific"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void camelCaseEndpoint() {
		ConfigurationMetadata metadata = compile(CamelCaseEndpoint.class);
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.pascal-case").fromSource(CamelCaseEndpoint.class));
		assertThat(metadata).has(defaultAccess("PascalCase", "pascal-case", TestAccess.UNRESTRICTED));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void incrementalEndpointBuildChangeDefaultAccess() {
		TestProject project = new TestProject(IncrementalEndpoint.class);
		ConfigurationMetadata metadata = project.compile();
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
		assertThat(metadata).has(access("incremental", TestAccess.UNRESTRICTED));
		assertThat(metadata).has(cacheTtl("incremental"));
		assertThat(metadata.getItems()).hasSize(3);
		project.replaceText(IncrementalEndpoint.class, "id = \"incremental\"",
				"id = \"incremental\", defaultAccess = org.springframework.boot.configurationsample.TestAccess.NONE");
		metadata = project.compile();
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
		assertThat(metadata).has(access("incremental", TestAccess.NONE));
		assertThat(metadata).has(cacheTtl("incremental"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void incrementalEndpointBuildChangeCacheFlag() {
		TestProject project = new TestProject(IncrementalEndpoint.class);
		ConfigurationMetadata metadata = project.compile();
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
		assertThat(metadata).has(access("incremental", TestAccess.UNRESTRICTED));
		assertThat(metadata).has(cacheTtl("incremental"));
		assertThat(metadata.getItems()).hasSize(3);
		project.replaceText(IncrementalEndpoint.class, "@Nullable String param", "String param");
		metadata = project.compile();
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
		assertThat(metadata).has(access("incremental", TestAccess.UNRESTRICTED));
		assertThat(metadata.getItems()).hasSize(2);
	}

	@Test
	void incrementalEndpointBuildChangeAccessOfSpecificEndpoint() {
		TestProject project = new TestProject(SpecificEndpoint.class);
		ConfigurationMetadata metadata = project.compile();
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.specific").fromSource(SpecificEndpoint.class));
		assertThat(metadata).has(access("specific", TestAccess.READ_ONLY));
		assertThat(metadata).has(cacheTtl("specific"));
		assertThat(metadata.getItems()).hasSize(3);
		project.replaceText(SpecificEndpoint.class, "defaultAccess = TestAccess.READ_ONLY",
				"defaultAccess = TestAccess.NONE");
		metadata = project.compile();
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.specific").fromSource(SpecificEndpoint.class));
		assertThat(metadata).has(access("specific", TestAccess.NONE));
		assertThat(metadata).has(cacheTtl("specific"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void shouldTolerateEndpointWithSameId() {
		ConfigurationMetadata metadata = compile(SimpleEndpoint.class, SimpleEndpoint2.class);
		assertThat(metadata).has(Metadata.withGroup("management.endpoint.simple").fromSource(SimpleEndpoint.class));
		assertThat(metadata).has(defaultAccess("simple", "simple", TestAccess.UNRESTRICTED));
		assertThat(metadata).has(cacheTtl("simple"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void shouldFailIfEndpointWithSameIdButWithConflictingEnabledByDefaultSetting() {
		assertThatRuntimeException().isThrownBy(() -> compile(SimpleEndpoint.class, SimpleEndpoint3.class))
			.havingRootCause()
			.isInstanceOf(IllegalStateException.class)
			.withMessage(
					"Existing property 'management.endpoint.simple.access' from type org.springframework.boot.configurationsample.endpoint.SimpleEndpoint has a conflicting value. Existing value: unrestricted, new value from type org.springframework.boot.configurationsample.endpoint.SimpleEndpoint3: none");
	}

	@Test
	void endpointWithNullableParameter() {
		ConfigurationMetadata metadata = compile(NullableParameterEndpoint.class);
		assertThat(metadata)
			.has(Metadata.withGroup("management.endpoint.nullable").fromSource(NullableParameterEndpoint.class));
		assertThat(metadata).has(access("nullable", TestAccess.UNRESTRICTED));
		assertThat(metadata).has(cacheTtl("nullable"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	private Metadata.MetadataItemCondition access(String endpointId, TestAccess defaultValue) {
		return defaultAccess(endpointId, endpointId, defaultValue);
	}

	private Metadata.MetadataItemCondition defaultAccess(String endpointId, String endpointSuffix,
			TestAccess defaultValue) {
		return Metadata.withAccess("management.endpoint." + endpointSuffix + ".access")
			.withDefaultValue(defaultValue.name().toLowerCase(Locale.ENGLISH))
			.withDescription("Permitted level of access for the %s endpoint.".formatted(endpointId));
	}

	private Metadata.MetadataItemCondition cacheTtl(String endpointId) {
		return Metadata.withProperty("management.endpoint." + endpointId + ".cache.time-to-live")
			.ofType(Duration.class)
			.withDefaultValue("0ms")
			.withDescription("Maximum time that a response can be cached.");
	}

}
