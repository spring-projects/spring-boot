/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.logging.structured;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.GraylogExtendedLogFormatProperties.GraylogExtendedLogFormatPropertiesRuntimeHints;
import org.springframework.boot.logging.structured.GraylogExtendedLogFormatProperties.Service;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraylogExtendedLogFormatProperties}.
 *
 * @author Samuel Lissner
 * @author Phillip Webb
 */
class GraylogExtendedLogFormatPropertiesTests {

	@Test
	void getBindsFromEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.gelf.host", "spring");
		environment.setProperty("logging.structured.gelf.service.version", "1.2.3");
		GraylogExtendedLogFormatProperties properties = GraylogExtendedLogFormatProperties.get(environment);
		assertThat(properties).isEqualTo(new GraylogExtendedLogFormatProperties("spring", new Service("1.2.3")));
	}

	@Test
	void getBindsFromEnvironmentWhenHostIsPresentAndServiceIsMissing() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.gelf.host", "spring");
		GraylogExtendedLogFormatProperties properties = GraylogExtendedLogFormatProperties.get(environment);
		assertThat(properties).isEqualTo(new GraylogExtendedLogFormatProperties("spring", new Service(null)));
	}

	@Test
	void getBindsFromEnvironmentWhenHostIsPresentAndServiceIsMissingUsesApplicationVersion() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.gelf.host", "spring");
		environment.setProperty("spring.application.version", "1.2.3");
		GraylogExtendedLogFormatProperties properties = GraylogExtendedLogFormatProperties.get(environment);
		assertThat(properties).isEqualTo(new GraylogExtendedLogFormatProperties("spring", new Service("1.2.3")));
	}

	@Test
	void getBindsFromEnvironmentWhenVersionIsPresentAndHostIsMissingUsesApplicationName() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.name", "spring");
		environment.setProperty("logging.structured.gelf.service.version", "1.2.3");
		GraylogExtendedLogFormatProperties properties = GraylogExtendedLogFormatProperties.get(environment);
		assertThat(properties).isEqualTo(new GraylogExtendedLogFormatProperties("spring", new Service("1.2.3")));
	}

	@Test
	void getWhenNoServiceNameUsesApplicationName() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.name", "spring");
		GraylogExtendedLogFormatProperties properties = GraylogExtendedLogFormatProperties.get(environment);
		assertThat(properties).isEqualTo(new GraylogExtendedLogFormatProperties("spring", new Service(null)));
	}

	@Test
	void getWhenNoServiceVersionUsesApplicationVersion() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.application.version", "1.2.3");
		GraylogExtendedLogFormatProperties properties = GraylogExtendedLogFormatProperties.get(environment);
		assertThat(properties).isEqualTo(new GraylogExtendedLogFormatProperties(null, new Service("1.2.3")));
	}

	@Test
	void getWhenNoPropertiesToBind() {
		MockEnvironment environment = new MockEnvironment();
		GraylogExtendedLogFormatProperties properties = GraylogExtendedLogFormatProperties.get(environment);
		assertThat(properties).isEqualTo(new GraylogExtendedLogFormatProperties(null, new Service(null)));
	}

	@Test
	void addToJsonMembersCreatesValidJson() {
		GraylogExtendedLogFormatProperties properties = new GraylogExtendedLogFormatProperties("spring",
				new Service("1.2.3"));
		JsonWriter<GraylogExtendedLogFormatProperties> writer = JsonWriter.of(properties::jsonMembers);
		assertThat(writer.writeToString(properties)).isEqualTo("{\"host\":\"spring\",\"_service_version\":\"1.2.3\"}");
	}

	@Test
	void shouldRegisterRuntimeHints() throws Exception {
		RuntimeHints hints = new RuntimeHints();
		new GraylogExtendedLogFormatPropertiesRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(GraylogExtendedLogFormatProperties.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onConstructor(GraylogExtendedLogFormatProperties.class.getConstructor(String.class, Service.class))
			.invoke()).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(Service.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onConstructor(GraylogExtendedLogFormatProperties.Service.class.getConstructor(String.class))
			.invoke()).accepts(hints);
	}

	@Test
	void graylogExtendedLogFormatPropertiesRuntimeHintsIsRegistered() {
		assertThat(AotServices.factories().load(RuntimeHintsRegistrar.class))
			.anyMatch(GraylogExtendedLogFormatPropertiesRuntimeHints.class::isInstance);
	}

}
