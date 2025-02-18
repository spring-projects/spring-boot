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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to ensure {@link SecurityFilterAutoConfiguration} doesn't cause early
 * initialization.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class SecurityFilterAutoConfigurationEarlyInitializationTests {

	private static final Pattern PASSWORD_PATTERN = Pattern.compile("^Using generated security password: (.*)$",
			Pattern.MULTILINE);

	@Test
	@DirtiesUrlFactories
	@ClassPathExclusions({ "spring-security-oauth2-client-*.jar", "spring-security-oauth2-resource-server-*.jar",
			"spring-security-saml2-service-provider-*.jar" })
	void testSecurityFilterDoesNotCauseEarlyInitialization(CapturedOutput output) {
		try (AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext()) {
			TestPropertyValues.of("server.port:0").applyTo(context);
			context.register(Config.class);
			context.refresh();
			int port = context.getWebServer().getPort();
			Matcher password = PASSWORD_PATTERN.matcher(output);
			assertThat(password.find()).isTrue();
			new TestRestTemplate("user", password.group(1)).getForEntity("http://localhost:" + port, Object.class);
			// If early initialization occurred a ConverterNotFoundException is thrown
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ DeserializerBean.class, JacksonModuleBean.class, ExampleController.class, ConverterBean.class })
	@ImportAutoConfiguration({ WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class,
			SecurityFilterAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	static class Config {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
			factory.setPort(0);
			return factory;
		}

	}

	static class SourceType {

		public String foo;

	}

	static class DestinationType {

		public String bar;

	}

	@Component
	static class JacksonModuleBean extends SimpleModule {

		private static final long serialVersionUID = 1L;

		JacksonModuleBean(DeserializerBean myDeser) {
			addDeserializer(SourceType.class, myDeser);
		}

	}

	@Component
	static class DeserializerBean extends StdDeserializer<SourceType> {

		@Autowired
		ConversionService conversionService;

		DeserializerBean() {
			super(SourceType.class);
		}

		@Override
		public SourceType deserialize(JsonParser p, DeserializationContext ctxt) {
			return new SourceType();
		}

	}

	@RestController
	static class ExampleController {

		@Autowired
		private ConversionService conversionService;

		@RequestMapping("/")
		void convert() {
			this.conversionService.convert(new SourceType(), DestinationType.class);
		}

	}

	@Component
	static class ConverterBean implements Converter<SourceType, DestinationType> {

		@Override
		public DestinationType convert(SourceType source) {
			return new DestinationType();
		}

	}

}
