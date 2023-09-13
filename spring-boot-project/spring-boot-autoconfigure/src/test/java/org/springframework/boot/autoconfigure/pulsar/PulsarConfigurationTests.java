/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.common.schema.KeyValueEncodingType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.pulsar.core.DefaultPulsarClientFactory;
import org.springframework.pulsar.core.DefaultSchemaResolver;
import org.springframework.pulsar.core.DefaultTopicResolver;
import org.springframework.pulsar.core.PulsarAdminBuilderCustomizer;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.pulsar.core.PulsarClientFactory;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.SchemaResolver.SchemaResolverCustomizer;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.function.PulsarFunctionAdministration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarConfiguration}.
 *
 * @author Chris Bono
 * @author Alexander Preuß
 * @author Soby Chacko
 * @author Phillip Webb
 */
class PulsarConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
		.withBean(PulsarClient.class, () -> mock(PulsarClient.class));

	@Test
	void whenHasUserDefinedConnectionDetailsBeanDoesNotAutoConfigureBean() {
		PulsarConnectionDetails customConnectionDetails = mock(PulsarConnectionDetails.class);
		this.contextRunner
			.withBean("customPulsarConnectionDetails", PulsarConnectionDetails.class, () -> customConnectionDetails)
			.run((context) -> assertThat(context).getBean(PulsarConnectionDetails.class)
				.isSameAs(customConnectionDetails));
	}

	@Nested
	class ClientTests {

		@Test
		void whenHasUserDefinedClientFactoryBeanDoesNotAutoConfigureBean() {
			PulsarClientFactory customFactory = mock(PulsarClientFactory.class);
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
				.withBean("customPulsarClientFactory", PulsarClientFactory.class, () -> customFactory)
				.run((context) -> assertThat(context).getBean(PulsarClientFactory.class).isSameAs(customFactory));
		}

		@Test
		void whenHasUserDefinedClientBeanDoesNotAutoConfigureBean() {
			PulsarClient customClient = mock(PulsarClient.class);
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
				.withBean("customPulsarClient", PulsarClient.class, () -> customClient)
				.run((context) -> assertThat(context).getBean(PulsarClient.class).isSameAs(customClient));
		}

		@Test
		void whenHasUserDefinedCustomizersAppliesInCorrectOrder() {
			PulsarConnectionDetails connectionDetails = mock(PulsarConnectionDetails.class);
			given(connectionDetails.getBrokerUrl()).willReturn("connectiondetails");
			PulsarConfigurationTests.this.contextRunner
				.withUserConfiguration(PulsarClientBuilderCustomizersConfig.class)
				.withBean(PulsarConnectionDetails.class, () -> connectionDetails)
				.withPropertyValues("spring.pulsar.client.service-url=properties")
				.run((context) -> {
					DefaultPulsarClientFactory clientFactory = context.getBean(DefaultPulsarClientFactory.class);
					Customizers<PulsarClientBuilderCustomizer, ClientBuilder> customizers = Customizers
						.of(ClientBuilder.class, PulsarClientBuilderCustomizer::customize);
					assertThat(customizers.fromField(clientFactory, "customizer")).callsInOrder(
							ClientBuilder::serviceUrl, "connectiondetails", "fromCustomizer1", "fromCustomizer2");
				});
		}

		@TestConfiguration(proxyBeanMethods = false)
		static class PulsarClientBuilderCustomizersConfig {

			@Bean
			@Order(200)
			PulsarClientBuilderCustomizer customizerFoo() {
				return (builder) -> builder.serviceUrl("fromCustomizer2");
			}

			@Bean
			@Order(100)
			PulsarClientBuilderCustomizer customizerBar() {
				return (builder) -> builder.serviceUrl("fromCustomizer1");
			}

		}

	}

	@Nested
	class AdministrationTests {

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			PulsarAdministration pulsarAdministration = mock(PulsarAdministration.class);
			this.contextRunner
				.withBean("customPulsarAdministration", PulsarAdministration.class, () -> pulsarAdministration)
				.run((context) -> assertThat(context).getBean(PulsarAdministration.class)
					.isSameAs(pulsarAdministration));
		}

		@Test
		void whenHasUserDefinedCustomizersAppliesInCorrectOrder() {
			PulsarConnectionDetails connectionDetails = mock(PulsarConnectionDetails.class);
			given(connectionDetails.getAdminUrl()).willReturn("connectiondetails");
			this.contextRunner.withUserConfiguration(PulsarAdminBuilderCustomizersConfig.class)
				.withBean(PulsarConnectionDetails.class, () -> connectionDetails)
				.withPropertyValues("spring.pulsar.admin.service-url=property")
				.run((context) -> {
					PulsarAdministration pulsarAdmin = context.getBean(PulsarAdministration.class);
					Customizers<PulsarAdminBuilderCustomizer, PulsarAdminBuilder> customizers = Customizers
						.of(PulsarAdminBuilder.class, PulsarAdminBuilderCustomizer::customize);
					assertThat(customizers.fromField(pulsarAdmin, "adminCustomizers")).callsInOrder(
							PulsarAdminBuilder::serviceHttpUrl, "connectiondetails", "fromCustomizer1",
							"fromCustomizer2");
				});
		}

		@TestConfiguration(proxyBeanMethods = false)
		static class PulsarAdminBuilderCustomizersConfig {

			@Bean
			@Order(200)
			PulsarAdminBuilderCustomizer customizerFoo() {
				return (builder) -> builder.serviceHttpUrl("fromCustomizer2");
			}

			@Bean
			@Order(100)
			PulsarAdminBuilderCustomizer customizerBar() {
				return (builder) -> builder.serviceHttpUrl("fromCustomizer1");
			}

		}

	}

	@Nested
	class SchemaResolverTests {

		@SuppressWarnings("rawtypes")
		private static final InstanceOfAssertFactory<Map, MapAssert<Class, Schema>> CLASS_SCHEMA_MAP = InstanceOfAssertFactories
			.map(Class.class, Schema.class);

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			SchemaResolver schemaResolver = mock(SchemaResolver.class);
			this.contextRunner.withBean("customSchemaResolver", SchemaResolver.class, () -> schemaResolver)
				.run((context) -> assertThat(context).getBean(SchemaResolver.class).isSameAs(schemaResolver));
		}

		@Test
		void whenHasUserDefinedSchemaResolverCustomizer() {
			SchemaResolverCustomizer<DefaultSchemaResolver> customizer = (schemaResolver) -> schemaResolver
				.addCustomSchemaMapping(TestRecord.class, Schema.STRING);
			this.contextRunner.withBean("schemaResolverCustomizer", SchemaResolverCustomizer.class, () -> customizer)
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, InstanceOfAssertFactories.MAP)
					.containsEntry(TestRecord.class, Schema.STRING));
		}

		@Test
		void whenHasDefaultsTypeMappingForPrimitiveAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type=STRING");
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, InstanceOfAssertFactories.MAP)
					.containsOnly(entry(TestRecord.class, Schema.STRING)));
		}

		@Test
		void whenHasDefaultsTypeMappingForStructAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type=JSON");
			Schema<?> expectedSchema = Schema.JSON(TestRecord.class);
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, CLASS_SCHEMA_MAP)
					.hasEntrySatisfying(TestRecord.class, schemaEqualTo(expectedSchema)));
		}

		@Test
		void whenHasDefaultsTypeMappingForKeyValueAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type=key-value");
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type=java.lang.String");
			Schema<?> expectedSchema = Schema.KeyValue(Schema.STRING, Schema.JSON(TestRecord.class),
					KeyValueEncodingType.INLINE);
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, CLASS_SCHEMA_MAP)
					.hasEntrySatisfying(TestRecord.class, schemaEqualTo(expectedSchema)));
		}

		@SuppressWarnings("rawtypes")
		private Consumer<Schema> schemaEqualTo(Schema<?> expected) {
			return (actual) -> assertThat(actual.getSchemaInfo()).isEqualTo(expected.getSchemaInfo());
		}

	}

	@Nested
	class TopicResolverTests {

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			TopicResolver topicResolver = mock(TopicResolver.class);
			this.contextRunner.withBean("customTopicResolver", TopicResolver.class, () -> topicResolver)
				.run((context) -> assertThat(context).getBean(TopicResolver.class).isSameAs(topicResolver));
		}

		@Test
		void whenHasDefaultsTypeMappingAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].topic-name=foo-topic");
			properties.add("spring.pulsar.defaults.type-mappings[1].message-type=java.lang.String");
			properties.add("spring.pulsar.defaults.type-mappings[1].topic-name=string-topic");
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(TopicResolver.class)
					.asInstanceOf(InstanceOfAssertFactories.type(DefaultTopicResolver.class))
					.extracting(DefaultTopicResolver::getCustomTopicMappings, InstanceOfAssertFactories.MAP)
					.containsOnly(entry(TestRecord.class, "foo-topic"), entry(String.class, "string-topic")));
		}

	}

	@Nested
	class FunctionAdministrationTests {

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenNoPropertiesAddsFunctionAdministrationBean() {
			this.contextRunner.run((context) -> assertThat(context).getBean(PulsarFunctionAdministration.class)
				.hasFieldOrPropertyWithValue("failFast", Boolean.TRUE)
				.hasFieldOrPropertyWithValue("propagateFailures", Boolean.TRUE)
				.hasFieldOrPropertyWithValue("propagateStopFailures", Boolean.FALSE)
				.hasNoNullFieldsOrProperties() // ensures object providers set
				.extracting("pulsarAdministration")
				.isSameAs(context.getBean(PulsarAdministration.class)));
		}

		@Test
		void whenHasFunctionPropertiesAppliesPropertiesToBean() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.function.fail-fast=false");
			properties.add("spring.pulsar.function.propagate-failures=false");
			properties.add("spring.pulsar.function.propagate-stop-failures=true");
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(PulsarFunctionAdministration.class)
					.hasFieldOrPropertyWithValue("failFast", Boolean.FALSE)
					.hasFieldOrPropertyWithValue("propagateFailures", Boolean.FALSE)
					.hasFieldOrPropertyWithValue("propagateStopFailures", Boolean.TRUE));
		}

		@Test
		void whenHasFunctionDisabledPropertyDoesNotCreateBean() {
			this.contextRunner.withPropertyValues("spring.pulsar.function.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(PulsarFunctionAdministration.class));
		}

		@Test
		void whenHasCustomFunctionAdministrationBean() {
			PulsarFunctionAdministration functionAdministration = mock(PulsarFunctionAdministration.class);
			this.contextRunner.withBean(PulsarFunctionAdministration.class, () -> functionAdministration)
				.run((context) -> assertThat(context).getBean(PulsarFunctionAdministration.class)
					.isSameAs(functionAdministration));
		}

	}

	record TestRecord() {

		private static final String CLASS_NAME = TestRecord.class.getName();

	}

}
