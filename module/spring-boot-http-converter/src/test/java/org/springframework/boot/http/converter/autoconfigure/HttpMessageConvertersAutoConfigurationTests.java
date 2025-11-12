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

package org.springframework.boot.http.converter.autoconfigure;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.http.converter.autoconfigure.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConverterConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpMessageConvertersAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author David Liu
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 * @author Dmitry Sulman
 */
class HttpMessageConvertersAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class));

	@Test
	void jacksonNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(JsonMapper.class);
			assertThat(context).doesNotHaveBean(JacksonJsonHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(JacksonXmlHttpMessageConverter.class);
		});
	}

	@Test
	void jacksonDefaultConverter() {
		this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class)
			.run(assertConverter(JacksonJsonHttpMessageConverter.class, "jacksonJsonHttpMessageConverter"));
	}

	@Test
	void jacksonConverterWithBuilder() {
		this.contextRunner.withUserConfiguration(JacksonJsonMapperBuilderConfig.class)
			.run(assertConverter(JacksonJsonHttpMessageConverter.class, "jacksonJsonHttpMessageConverter"));
	}

	@Test
	void jacksonXmlConverterWithBuilder() {
		this.contextRunner.withUserConfiguration(JacksonXmlMapperBuilderConfig.class)
			.run(assertConverter(JacksonXmlHttpMessageConverter.class, "jacksonXmlHttpMessageConverter"));
	}

	@Test
	void jacksonCustomConverter() {
		this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class, JacksonConverterConfig.class)
			.run(assertConverter(JacksonJsonHttpMessageConverter.class, "customJacksonMessageConverter"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2DefaultConverter() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperConfig.class)
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run(assertConverter(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class,
					"mappingJackson2HttpMessageConverter"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2ConverterWithBuilder() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperBuilderConfig.class)
			.run(assertConverter(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class,
					"mappingJackson2HttpMessageConverter"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2CustomConverter() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperConfig.class, Jackson2ConverterConfig.class)
			.run(assertConverter(org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class,
					"customJacksonMessageConverter"));
	}

	@Test
	void gsonNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(Gson.class);
			assertThat(context).doesNotHaveBean(GsonHttpMessageConverter.class);
		});
	}

	@Test
	void gsonDefaultConverter() {
		this.contextRunner.withBean(Gson.class)
			.run(assertConverter(GsonHttpMessageConverter.class, "gsonHttpMessageConverter"));
	}

	@Test
	void gsonCustomConverter() {
		this.contextRunner.withUserConfiguration(GsonConverterConfig.class)
			.withBean(Gson.class)
			.run(assertConverter(GsonHttpMessageConverter.class, "customGsonMessageConverter"));
	}

	@Test
	void gsonCanBePreferred() {
		allOptionsRunner().withPropertyValues("spring.http.converters.preferred-json-mapper:gson").run((context) -> {
			assertConverterBeanExists(context, GsonHttpMessageConverter.class, "gsonHttpMessageConverter");
			assertConverterBeanRegisteredWithHttpMessageConverters(context, GsonHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(JacksonJsonHttpMessageConverter.class);
		});
	}

	@Test
	void jsonbNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(Jsonb.class);
			assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
		});
	}

	@Test
	void jsonbDefaultConverter() {
		this.contextRunner.withBean(Jsonb.class, JsonbBuilder::create)
			.run(assertConverter(JsonbHttpMessageConverter.class, "jsonbHttpMessageConverter"));
	}

	@Test
	void jsonbCustomConverter() {
		this.contextRunner.withUserConfiguration(JsonbConverterConfig.class)
			.withBean(Jsonb.class, JsonbBuilder::create)
			.run(assertConverter(JsonbHttpMessageConverter.class, "customJsonbMessageConverter"));
	}

	@Test
	void jsonbCanBePreferred() {
		allOptionsRunner().withPropertyValues("spring.http.converters.preferred-json-mapper:jsonb").run((context) -> {
			assertConverterBeanExists(context, JsonbHttpMessageConverter.class, "jsonbHttpMessageConverter");
			assertConverterBeanRegisteredWithHttpMessageConverters(context, JsonbHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(GsonHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(JacksonJsonHttpMessageConverter.class);
		});
	}

	@Test
	void kotlinSerializationNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(Json.class);
			assertThat(context).doesNotHaveBean(KotlinSerializationJsonHttpMessageConverter.class);
		});
	}

	@Test
	void kotlinSerializationCustomConverter() {
		this.contextRunner.withUserConfiguration(KotlinSerializationConverterConfig.class)
			.withBean(Json.class, () -> Json.Default)
			.run(assertConverter(KotlinSerializationJsonHttpMessageConverter.class,
					"customKotlinSerializationJsonHttpMessageConverter"));
	}

	@Test
	void kotlinSerializationOrderedAheadOfJsonConverter() {
		allOptionsRunner().run((context) -> {
			assertConverterBeanExists(context, KotlinSerializationJsonHttpMessageConverter.class,
					"kotlinSerializationJsonHttpMessageConverter");
			assertConverterBeanRegisteredWithHttpMessageConverters(context,
					KotlinSerializationJsonHttpMessageConverter.class);
			assertConvertersBeanRegisteredWithHttpMessageConverters(context,
					List.of(KotlinSerializationJsonHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class));
		});
	}

	@Test
	void kotlinSerializationUsesLimitedPredicateWhenOtherJsonConverterIsAvailable() {
		allOptionsRunner().run((context) -> {
			KotlinSerializationJsonHttpMessageConverter converter = context
				.getBean(KotlinSerializationJsonHttpMessageConverter.class);
			assertThat(converter.canWrite(Map.class, MediaType.APPLICATION_JSON)).isFalse();
		});
	}

	@Test
	void kotlinSerializationUsesUnrestrictedPredicateWhenNoOtherJsonConverterIsAvailable() {
		this.contextRunner.withBean(Json.class, () -> Json.Default).run((context) -> {
			KotlinSerializationJsonHttpMessageConverter converter = context
				.getBean(KotlinSerializationJsonHttpMessageConverter.class);
			assertThat(converter.canWrite(Map.class, MediaType.APPLICATION_JSON)).isTrue();
		});
	}

	@Test
	void stringDefaultConverter() {
		this.contextRunner.run(assertConverter(StringHttpMessageConverter.class, "stringHttpMessageConverter"));
	}

	@Test
	void stringCustomConverter() {
		this.contextRunner.withUserConfiguration(StringConverterConfig.class)
			.run(assertConverter(StringHttpMessageConverter.class, "customStringMessageConverter"));
	}

	@Test
	void typeConstrainedConverterDoesNotPreventAutoConfigurationOfJacksonConverter() {
		this.contextRunner
			.withUserConfiguration(JacksonJsonMapperBuilderConfig.class, TypeConstrainedConverterConfiguration.class)
			.run((context) -> {
				BeanDefinition beanDefinition = ((GenericApplicationContext) context.getSourceApplicationContext())
					.getBeanDefinition("jacksonJsonHttpMessageConverter");
				assertThat(beanDefinition.getFactoryBeanName())
					.isEqualTo(JacksonJsonHttpMessageConverterConfiguration.class.getName());
			});
	}

	@Test
	void typeConstrainedConverterFromSpringDataDoesNotPreventAutoConfigurationOfJacksonConverter() {
		this.contextRunner
			.withUserConfiguration(JacksonJsonMapperBuilderConfig.class, RepositoryRestMvcConfiguration.class)
			.run((context) -> {
				BeanDefinition beanDefinition = ((GenericApplicationContext) context.getSourceApplicationContext())
					.getBeanDefinition("jacksonJsonHttpMessageConverter");
				assertThat(beanDefinition.getFactoryBeanName())
					.isEqualTo(JacksonJsonHttpMessageConverterConfiguration.class.getName());
			});
	}

	@Test
	void jacksonIsPreferredByDefault() {
		allOptionsRunner().run((context) -> {
			assertConverterBeanExists(context, JacksonJsonHttpMessageConverter.class,
					"jacksonJsonHttpMessageConverter");
			assertConverterBeanRegisteredWithHttpMessageConverters(context, JacksonJsonHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(GsonHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
		});
	}

	@Test
	@SuppressWarnings("removal")
	void jackson2IsPreferredIfJacksonIsNotAvailable() {
		allOptionsRunner().withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName()))
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> {
				assertConverterBeanExists(context,
						org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class,
						"mappingJackson2HttpMessageConverter");
				assertConverterBeanRegisteredWithHttpMessageConverters(context,
						org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class);
				assertThat(context).doesNotHaveBean(GsonHttpMessageConverter.class);
				assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
			});
	}

	@Test
	void gsonIsPreferredIfJacksonAndJackson2AreNotAvailable() {
		allOptionsRunner()
			.withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName(),
					ObjectMapper.class.getPackage().getName()))
			.run((context) -> {
				assertConverterBeanExists(context, GsonHttpMessageConverter.class, "gsonHttpMessageConverter");
				assertConverterBeanRegisteredWithHttpMessageConverters(context, GsonHttpMessageConverter.class);
				assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
			});
	}

	@Test
	void jsonbIsPreferredIfJacksonAndGsonAreNotAvailable() {
		allOptionsRunner()
			.withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName(),
					ObjectMapper.class.getPackage().getName(), Gson.class.getPackage().getName()))
			.run(assertConverter(JsonbHttpMessageConverter.class, "jsonbHttpMessageConverter"));
	}

	@Test
	void whenServletWebApplicationHttpMessageConvertersIsConfigured() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(ServerHttpMessageConvertersCustomizer.class)
				.hasSingleBean(ClientHttpMessageConvertersCustomizer.class));
	}

	@Test
	void whenReactiveWebApplicationHttpMessageConvertersIsNotConfigured() {
		new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ServerHttpMessageConvertersCustomizer.class)
				.doesNotHaveBean(ClientHttpMessageConvertersCustomizer.class));
	}

	@Test
	void whenEncodingCharsetIsNotConfiguredThenStringMessageConverterUsesUtf8() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(StringHttpMessageConverter.class);
				assertThat(context.getBean(StringHttpMessageConverter.class).getDefaultCharset())
					.isEqualTo(StandardCharsets.UTF_8);
			});
	}

	@Test
	void whenEncodingCharsetIsConfiguredThenStringMessageConverterUsesSpecificCharset() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.withPropertyValues("spring.http.converters.string-encoding-charset=UTF-16")
			.run((context) -> {
				assertThat(context).hasSingleBean(StringHttpMessageConverter.class);
				assertThat(context.getBean(StringHttpMessageConverter.class).getDefaultCharset())
					.isEqualTo(StandardCharsets.UTF_16);
			});
	}

	@Test
	void defaultServerConvertersCustomizerHasOrderZero() {
		defaultConvertersCustomizerHasOrderZero(DefaultServerHttpMessageConvertersCustomizer.class);
	}

	@Test
	void defaultClientConvertersCustomizerHasOrderZero() {
		defaultConvertersCustomizerHasOrderZero(DefaultClientHttpMessageConvertersCustomizer.class);
	}

	private <T> void defaultConvertersCustomizerHasOrderZero(Class<T> customizerType) {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> {
				Map<String, T> customizers = context.getBeansOfType(customizerType);
				assertThat(customizers).hasSize(1);
				DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
				customizers.keySet().forEach((beanName) -> assertThat(beanFactory.getOrder(beanName)).isZero());
			});
	}

	private ApplicationContextRunner allOptionsRunner() {
		return this.contextRunner.withBean(Gson.class)
			.withBean(JsonMapper.class)
			.withBean(ObjectMapper.class, ObjectMapper::new)
			.withBean(Jsonb.class, JsonbBuilder::create)
			.withBean(Json.class, () -> Json.Default);
	}

	private ContextConsumer<AssertableApplicationContext> assertConverter(
			Class<? extends HttpMessageConverter<?>> converterType, String beanName) {
		return (context) -> {
			assertConverterBeanExists(context, converterType, beanName);
			assertConverterBeanRegisteredWithHttpMessageConverters(context, converterType);
		};
	}

	private void assertConverterBeanExists(AssertableApplicationContext context, Class<?> type, String beanName) {
		assertThat(context).hasSingleBean(type);
		assertThat(context).hasBean(beanName);
	}

	private void assertConverterBeanRegisteredWithHttpMessageConverters(AssertableApplicationContext context,
			Class<? extends HttpMessageConverter<?>> type) {
		HttpMessageConverter<?> converter = context.getBean(type);
		ClientHttpMessageConvertersCustomizer clientCustomizer = context
			.getBean(ClientHttpMessageConvertersCustomizer.class);
		ClientBuilder clientBuilder = HttpMessageConverters.forClient().registerDefaults();
		clientCustomizer.customize(clientBuilder);
		HttpMessageConverters clientConverters = clientBuilder.build();
		assertThat(clientConverters).contains(converter);
		assertThat(clientConverters).filteredOn((c) -> type.isAssignableFrom(c.getClass())).hasSize(1);

		ServerHttpMessageConvertersCustomizer serverCustomizer = context
			.getBean(ServerHttpMessageConvertersCustomizer.class);
		ServerBuilder serverBuilder = HttpMessageConverters.forServer().registerDefaults();
		serverCustomizer.customize(serverBuilder);
		HttpMessageConverters serverConverters = serverBuilder.build();
		assertThat(serverConverters).contains(converter);
		assertThat(serverConverters).filteredOn((c) -> type.isAssignableFrom(c.getClass())).hasSize(1);
	}

	private void assertConvertersBeanRegisteredWithHttpMessageConverters(AssertableApplicationContext context,
			List<Class<? extends HttpMessageConverter<?>>> types) {
		List<? extends HttpMessageConverter<?>> converterInstances = types.stream().map(context::getBean).toList();
		ClientHttpMessageConvertersCustomizer clientCustomizer = context
			.getBean(ClientHttpMessageConvertersCustomizer.class);
		ClientBuilder clientBuilder = HttpMessageConverters.forClient().registerDefaults();
		clientCustomizer.customize(clientBuilder);
		assertThat(clientBuilder.build()).containsSubsequence(converterInstances);

		ServerHttpMessageConvertersCustomizer serverCustomizer = context
			.getBean(ServerHttpMessageConvertersCustomizer.class);
		ServerBuilder serverBuilder = HttpMessageConverters.forServer().registerDefaults();
		serverCustomizer.customize(serverBuilder);
		assertThat(serverBuilder.build()).containsSubsequence(converterInstances);
	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonJsonMapperConfig {

		@Bean
		JsonMapper jsonMapper() {
			return new JsonMapper();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonJsonMapperBuilderConfig {

		@Bean
		JsonMapper jsonMapper() {
			return new JsonMapper();
		}

		@Bean
		JsonMapper.Builder builder() {
			return JsonMapper.builder();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonXmlMapperBuilderConfig {

		@Bean
		XmlMapper xmlMapper() {
			return new XmlMapper();
		}

		@Bean
		XmlMapper.Builder builder() {
			return XmlMapper.builder();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonConverterConfig {

		@Bean
		JacksonJsonHttpMessageConverter customJacksonMessageConverter(JsonMapper jsonMapperMapper) {
			JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapperMapper);
			return converter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Jackson2ObjectMapperConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	static class Jackson2ObjectMapperBuilderConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder() {
			return new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
		}

	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	@Configuration(proxyBeanMethods = false)
	@SuppressWarnings("removal")
	static class Jackson2ConverterConfig {

		@Bean
		org.springframework.http.converter.json.MappingJackson2HttpMessageConverter customJacksonMessageConverter(
				ObjectMapper objectMapper) {
			org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter = new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter();
			converter.setObjectMapper(objectMapper);
			return converter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GsonConverterConfig {

		@Bean
		GsonHttpMessageConverter customGsonMessageConverter(Gson gson) {
			GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
			converter.setGson(gson);
			return converter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JsonbConverterConfig {

		@Bean
		JsonbHttpMessageConverter customJsonbMessageConverter(Jsonb jsonb) {
			JsonbHttpMessageConverter converter = new JsonbHttpMessageConverter();
			converter.setJsonb(jsonb);
			return converter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class KotlinSerializationConverterConfig {

		@Bean
		KotlinSerializationJsonHttpMessageConverter customKotlinSerializationJsonHttpMessageConverter(Json json) {
			return new KotlinSerializationJsonHttpMessageConverter(json);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StringConverterConfig {

		@Bean
		StringHttpMessageConverter customStringMessageConverter() {
			return new StringHttpMessageConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeConstrainedConverterConfiguration {

		@Bean
		TypeConstrainedJacksonJsonHttpMessageConverter typeConstrainedConverter() {
			return new TypeConstrainedJacksonJsonHttpMessageConverter(RepresentationModel.class);
		}

	}

}
