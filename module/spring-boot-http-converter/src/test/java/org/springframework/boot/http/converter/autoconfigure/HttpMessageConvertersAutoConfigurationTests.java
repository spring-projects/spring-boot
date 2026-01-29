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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.http.converter.autoconfigure.GsonHttpMessageConvertersConfiguration.GsonHttpConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.JacksonHttpMessageConvertersConfiguration.JacksonXmlHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.JsonbHttpMessageConvertersConfiguration.JsonbHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.KotlinSerializationHttpMessageConvertersConfiguration.KotlinSerializationJsonConvertersCustomizer;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.alps.AlpsJacksonJsonHttpMessageConverter;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsHttpMessageConverter;
import org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
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
 * @author Brian Clozel
 */
class HttpMessageConvertersAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class));

	@Test
	void jacksonNotAvailable() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName()))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(JsonMapper.class);
				assertThat(context).doesNotHaveBean(JacksonJsonHttpMessageConvertersCustomizer.class);
				assertThat(context).doesNotHaveBean(JacksonXmlHttpMessageConvertersCustomizer.class);
			});
	}

	@Test
	void jacksonDefaultConverter() {
		this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
			assertConverterIsRegistered(context, JacksonJsonHttpMessageConverter.class);
		});
	}

	@Test
	void jacksonServerCustomizer() {
		this.contextRunner.withUserConfiguration(CustomJsonConverterConfig.class).run((context) -> {
			assertConverterIsNotRegistered(context, JacksonJsonHttpMessageConverter.class);
			assertConverterIsRegistered(context, CustomConverter.class);
		});
	}

	@Test
	void jacksonConverterWithBuilder() {
		this.contextRunner.withUserConfiguration(JacksonJsonMapperBuilderConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
			assertConverterIsRegistered(context, JacksonJsonHttpMessageConverter.class);
		});
	}

	@Test
	void jacksonXmlConverterWithBuilder() {
		this.contextRunner.withUserConfiguration(JacksonXmlMapperBuilderConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(JacksonXmlHttpMessageConvertersCustomizer.class);
			assertConverterIsRegistered(context, JacksonXmlHttpMessageConverter.class);
		});
	}

	@Test
	void jacksonCustomConverter() {
		this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class, JacksonConverterConfig.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(JacksonJsonHttpMessageConvertersCustomizer.class);
				HttpMessageConverters serverConverters = getServerConverters(context);
				assertThat(serverConverters)
					.contains(context.getBean("customJacksonMessageConverter", JacksonJsonHttpMessageConverter.class));
			});
	}

	@Test
	void jacksonServerAndClientConvertersShouldBeDifferent() {
		this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
			JacksonJsonHttpMessageConverter serverConverter = findConverter(getServerConverters(context),
					JacksonJsonHttpMessageConverter.class);
			JacksonJsonHttpMessageConverter clientConverter = findConverter(getClientConverters(context),
					JacksonJsonHttpMessageConverter.class);
			assertThat(serverConverter).isNotEqualTo(clientConverter);
		});
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2DefaultConverter() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperConfig.class)
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> assertConverterIsRegistered(context,
					org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2ConverterWithBuilder() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperBuilderConfig.class)
			.run((context) -> assertConverterIsRegistered(context,
					org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2CustomConverter() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperConfig.class, Jackson2ConverterConfig.class)
			.run((context) -> assertConverterIsRegistered(context,
					org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2ServerAndClientJsonConvertersShouldBeDifferent() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperConfig.class)
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> {
				assertThat(context).hasSingleBean(
						Jackson2HttpMessageConvertersConfiguration.Jackson2JsonMessageConvertersCustomizer.class);
				HttpMessageConverter<?> serverConverter = findConverter(getServerConverters(context),
						org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class);
				HttpMessageConverter<?> clientConverter = findConverter(getClientConverters(context),
						org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class);
				assertThat(serverConverter).isNotEqualTo(clientConverter);
			});
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void jackson2ServerAndClientXmlConvertersShouldBeDifferent() {
		this.contextRunner.withUserConfiguration(Jackson2ObjectMapperConfig.class)
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> {
				assertThat(context).hasSingleBean(
						Jackson2HttpMessageConvertersConfiguration.Jackson2XmlMessageConvertersCustomizer.class);
				HttpMessageConverter<?> serverConverter = findConverter(getServerConverters(context),
						org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter.class);
				HttpMessageConverter<?> clientConverter = findConverter(getClientConverters(context),
						org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter.class);
				assertThat(serverConverter).isNotEqualTo(clientConverter);
			});
	}

	@Test
	void gsonNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(Gson.class);
			assertConverterIsNotRegistered(context, GsonHttpMessageConverter.class);
		});
	}

	@Test
	void gsonDefaultConverter() {
		this.contextRunner.withBean(Gson.class)
			.run((context) -> assertConverterIsRegistered(context, GsonHttpMessageConverter.class));
	}

	@Test
	void gsonCustomConverter() {
		this.contextRunner.withUserConfiguration(GsonConverterConfig.class)
			.withBean(Gson.class)
			.run((context) -> assertThat(getServerConverters(context))
				.contains(context.getBean("customGsonMessageConverter", GsonHttpMessageConverter.class)));
	}

	@Test
	void gsonCanBePreferred() {
		allOptionsRunner().withPropertyValues("spring.http.converters.preferred-json-mapper:gson").run((context) -> {
			assertConverterIsRegistered(context, GsonHttpMessageConverter.class);
			assertConverterIsNotRegistered(context, JsonbHttpMessageConverter.class);
			assertConverterIsNotRegistered(context, JacksonJsonHttpMessageConverter.class);
		});
	}

	@Test
	void jsonbNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(Jsonb.class);
			assertConverterIsNotRegistered(context, JsonbHttpMessageConverter.class);
		});
	}

	@Test
	void jsonbDefaultConverter() {
		this.contextRunner.withBean(Jsonb.class, JsonbBuilder::create)
			.run((context) -> assertConverterIsRegistered(context, JsonbHttpMessageConverter.class));
	}

	@Test
	void jsonbCustomConverter() {
		this.contextRunner.withUserConfiguration(JsonbConverterConfig.class)
			.withBean(Jsonb.class, JsonbBuilder::create)
			.run((context) -> assertThat(getServerConverters(context))
				.contains(context.getBean("customJsonbMessageConverter", JsonbHttpMessageConverter.class)));
	}

	@Test
	void jsonbCanBePreferred() {
		allOptionsRunner().withPropertyValues("spring.http.converters.preferred-json-mapper:jsonb").run((context) -> {
			assertConverterIsRegistered(context, JsonbHttpMessageConverter.class);
			assertConverterIsNotRegistered(context, GsonHttpMessageConverter.class);
			assertConverterIsNotRegistered(context, JacksonJsonHttpMessageConverter.class);
		});
	}

	@Test
	void kotlinSerializationNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(Json.class);
			assertThat(context).doesNotHaveBean(KotlinSerializationJsonConvertersCustomizer.class);
		});
	}

	@Test
	void kotlinSerializationCustomConverter() {
		this.contextRunner.withUserConfiguration(KotlinSerializationConverterConfig.class)
			.withBean(Json.class, () -> Json.Default)
			.run((context) -> assertConverterIsRegistered(context, KotlinSerializationJsonHttpMessageConverter.class));
	}

	@Test
	void kotlinSerializationOrderedAheadOfJsonConverter() {
		allOptionsRunner().run((context) -> {
			assertConverterIsRegistered(context, KotlinSerializationJsonHttpMessageConverter.class);
			assertConvertersRegisteredWithHttpMessageConverters(context,
					List.of(KotlinSerializationJsonHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class));
		});
	}

	@Test
	void kotlinSerializationUsesLimitedPredicateWhenOtherJsonConverterIsAvailable() {
		allOptionsRunner().run((context) -> {
			KotlinSerializationJsonHttpMessageConverter converter = findConverter(getServerConverters(context),
					KotlinSerializationJsonHttpMessageConverter.class);
			assertThat(converter.canWrite(Map.class, MediaType.APPLICATION_JSON)).isFalse();
		});
	}

	@Test
	void stringDefaultConverter() {
		this.contextRunner.run((context) -> assertConverterIsRegistered(context, StringHttpMessageConverter.class));
	}

	@Test
	void stringCustomConverter() {
		this.contextRunner.withUserConfiguration(StringConverterConfig.class).run((context) -> {
			assertThat(getClientConverters(context))
				.filteredOn((converter) -> converter instanceof StringHttpMessageConverter)
				.hasSize(2);
			assertThat(getServerConverters(context))
				.filteredOn((converter) -> converter instanceof StringHttpMessageConverter)
				.hasSize(2);
		});
	}

	@Test
	void typeConstrainedConverterDoesNotPreventAutoConfigurationOfJacksonConverter() {
		this.contextRunner
			.withUserConfiguration(JacksonJsonMapperBuilderConfig.class, TypeConstrainedConverterConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
				assertConvertersRegisteredWithHttpMessageConverters(context, List
					.of(TypeConstrainedJacksonJsonHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class));
			});
	}

	@Test
	void typeConstrainedConverterFromSpringDataDoesNotPreventAutoConfigurationOfJacksonConverter() {
		this.contextRunner
			.withUserConfiguration(JacksonJsonMapperBuilderConfig.class, RepositoryRestMvcConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
				assertConvertersRegisteredWithHttpMessageConverters(context, List.of(HalFormsHttpMessageConverter.class,
						AlpsJacksonJsonHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class));
			});
	}

	@Test
	void jacksonIsPreferredByDefault() {
		allOptionsRunner().run((context) -> {
			assertBeanExists(context, JacksonJsonHttpMessageConvertersCustomizer.class,
					"jacksonJsonHttpMessageConvertersCustomizer");
			assertConverterIsRegistered(context, JacksonJsonHttpMessageConverter.class);
			assertThat(context).doesNotHaveBean(GsonHttpConvertersCustomizer.class);
			assertThat(context).doesNotHaveBean(JsonbHttpMessageConvertersCustomizer.class);
		});
	}

	@Test
	@SuppressWarnings("removal")
	void jackson2IsPreferredIfJacksonIsNotAvailable() {
		allOptionsRunner().withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName()))
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.run((context) -> {
				assertConverterIsRegistered(context,
						org.springframework.http.converter.json.MappingJackson2HttpMessageConverter.class);
				assertConverterIsNotRegistered(context, GsonHttpMessageConverter.class);
				assertConverterIsNotRegistered(context, JsonbHttpMessageConverter.class);
			});
	}

	@Test
	void gsonIsPreferredIfJacksonAndJackson2AreNotAvailable() {
		allOptionsRunner()
			.withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName(),
					ObjectMapper.class.getPackage().getName()))
			.run((context) -> {
				assertConverterIsRegistered(context, GsonHttpMessageConverter.class);
				assertConverterIsNotRegistered(context, JsonbHttpMessageConverter.class);
			});
	}

	@Test
	void jsonbIsPreferredIfJacksonAndGsonAreNotAvailable() {
		allOptionsRunner()
			.withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName(),
					ObjectMapper.class.getPackage().getName(), Gson.class.getPackage().getName()))
			.run((context) -> assertConverterIsRegistered(context, JsonbHttpMessageConverter.class));
	}

	@Test
	void whenServletWebApplicationHttpMessageConvertersIsConfigured() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(DefaultClientHttpMessageConvertersCustomizer.class)
				.hasSingleBean(DefaultClientHttpMessageConvertersCustomizer.class));
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
				StringHttpMessageConverter converter = findConverter(getServerConverters(context),
						StringHttpMessageConverter.class);
				assertThat(converter.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_8);
			});
	}

	@Test
	void whenEncodingCharsetIsConfiguredThenStringMessageConverterUsesSpecificCharset() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.withPropertyValues("spring.http.converters.string-encoding-charset=UTF-16")
			.run((context) -> {
				StringHttpMessageConverter serverConverter = findConverter(getServerConverters(context),
						StringHttpMessageConverter.class);
				assertThat(serverConverter.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_16);
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

	private void assertConverterIsRegistered(AssertableApplicationContext context,
			Class<? extends HttpMessageConverter<?>> converterType) {
		assertThat(getClientConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
			.hasSize(1);
		assertThat(getServerConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
			.hasSize(1);
	}

	private void assertConverterIsNotRegistered(AssertableApplicationContext context,
			Class<? extends HttpMessageConverter<?>> converterType) {
		assertThat(getClientConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
			.isEmpty();
		assertThat(getServerConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
			.isEmpty();
	}

	private void assertBeanExists(AssertableApplicationContext context, Class<?> type, String beanName) {
		assertThat(context).getBean(beanName).isInstanceOf(type);
		assertThat(context).hasBean(beanName);
	}

	private HttpMessageConverters getClientConverters(ApplicationContext context) {
		ClientBuilder clientBuilder = HttpMessageConverters.forClient().registerDefaults();
		context.getBeanProvider(ClientHttpMessageConvertersCustomizer.class)
			.orderedStream()
			.forEach((customizer) -> customizer.customize(clientBuilder));
		return clientBuilder.build();
	}

	private HttpMessageConverters getServerConverters(ApplicationContext context) {
		ServerBuilder serverBuilder = HttpMessageConverters.forServer().registerDefaults();
		context.getBeanProvider(ServerHttpMessageConvertersCustomizer.class)
			.orderedStream()
			.forEach((customizer) -> customizer.customize(serverBuilder));
		return serverBuilder.build();
	}

	@SuppressWarnings("unchecked")
	private <T extends HttpMessageConverter<?>> T findConverter(HttpMessageConverters converters,
			Class<? extends HttpMessageConverter<?>> type) {
		for (HttpMessageConverter<?> converter : converters) {
			if (type.isAssignableFrom(converter.getClass())) {
				return (T) converter;
			}
		}
		throw new IllegalStateException("Could not find converter of type " + type);
	}

	private void assertConvertersRegisteredWithHttpMessageConverters(AssertableApplicationContext context,
			List<Class<? extends HttpMessageConverter<?>>> types) {
		HttpMessageConverters clientConverters = getClientConverters(context);
		List<Class<?>> clientConverterTypes = new ArrayList<>();
		clientConverters.forEach((converter) -> clientConverterTypes.add(converter.getClass()));
		assertThat(clientConverterTypes).containsSubsequence(types);

		HttpMessageConverters serverConverters = getServerConverters(context);
		List<Class<?>> serverConverterTypes = new ArrayList<>();
		serverConverters.forEach((converter) -> serverConverterTypes.add(converter.getClass()));
		assertThat(serverConverterTypes).containsSubsequence(types);
	}

	@Configuration(proxyBeanMethods = false)
	static class JacksonJsonMapperConfig {

		@Bean
		JsonMapper jsonMapper() {
			return new JsonMapper();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJsonConverterConfig {

		@Bean
		JsonMapper jsonMapper() {
			return new JsonMapper();
		}

		@Bean
		ServerHttpMessageConvertersCustomizer jsonServerCustomizer() {
			return (configurer) -> configurer.withJsonConverter(new CustomConverter(MediaType.APPLICATION_JSON));
		}

		@Bean
		ClientHttpMessageConvertersCustomizer jsonClientCustomizer() {
			return (configurer) -> configurer.withJsonConverter(new CustomConverter(MediaType.APPLICATION_JSON));
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
	@SuppressWarnings("removal")
	static class Jackson2ObjectMapperConfig {

		@Bean
		org.springframework.http.converter.json.Jackson2ObjectMapperBuilder objectMapperBuilder() {
			return new org.springframework.http.converter.json.Jackson2ObjectMapperBuilder();
		}

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

	@SuppressWarnings("NullAway")
	static class CustomConverter extends AbstractHttpMessageConverter<Object> {

		CustomConverter(MediaType supportedMediaType) {
			super(supportedMediaType);
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			return null;
		}

		@Override
		protected void writeInternal(Object o, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {

		}

	}

}
