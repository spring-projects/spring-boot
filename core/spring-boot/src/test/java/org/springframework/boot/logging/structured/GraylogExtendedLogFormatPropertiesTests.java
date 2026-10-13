/*
 * Copyright 2012-present the original author or authors.
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
			.onConstructorInvocation(
					GraylogExtendedLogFormatProperties.class.getConstructor(String.class, Service.class)))
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(Service.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onConstructorInvocation(GraylogExtendedLogFormatProperties.Service.class.getConstructor(String.class)))
			.accepts(hints);
	}

	@Test
	void graylogExtendedLogFormatPropertiesRuntimeHintsIsRegistered() {
		assertThat(AotServices.factories().load(RuntimeHintsRegistrar.class))
			.anyMatch(GraylogExtendedLogFormatPropertiesRuntimeHints.class::isInstance);
	}

}
