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

import com.google.gson.Gson;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConverterConfiguration;
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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
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
 */
class HttpMessageConvertersAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class));

	@Test
	void jacksonNotAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ObjectMapper.class);
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
	void gsonIsPreferredIfJacksonIsNotAvailable() {
		allOptionsRunner().withClassLoader(new FilteredClassLoader(ObjectMapper.class.getPackage().getName()))
			.run((context) -> {
				assertConverterBeanExists(context, GsonHttpMessageConverter.class, "gsonHttpMessageConverter");
				assertConverterBeanRegisteredWithHttpMessageConverters(context, GsonHttpMessageConverter.class);
				assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
			});
	}

	@Test
	void jsonbIsPreferredIfJacksonAndGsonAreNotAvailable() {
		allOptionsRunner()
			.withClassLoader(new FilteredClassLoader(ObjectMapper.class.getPackage().getName(),
					Gson.class.getPackage().getName()))
			.run(assertConverter(JsonbHttpMessageConverter.class, "jsonbHttpMessageConverter"));
	}

	@Test
	void whenServletWebApplicationHttpMessageConvertersIsConfigured() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(HttpMessageConverters.class));
	}

	@Test
	void whenReactiveWebApplicationHttpMessageConvertersIsNotConfigured() {
		new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(HttpMessageConverters.class));
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

	private ApplicationContextRunner allOptionsRunner() {
		return this.contextRunner.withBean(Gson.class)
			.withBean(JsonMapper.class)
			.withBean(Jsonb.class, JsonbBuilder::create);
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
		HttpMessageConverters converters = context.getBean(HttpMessageConverters.class);
		assertThat(converters.getConverters()).contains(converter);
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
