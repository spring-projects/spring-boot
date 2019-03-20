/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfigurationTests.WebSecurity;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration test to ensure {@link SecurityFilterAutoConfiguration} doesn't cause early
 * initialization.
 *
 * @author Phillip Webb
 */
public class SecurityFilterAutoConfigurationEarlyInitializationTests {

	// gh-4154

	@Test
	public void testSecurityFilterDoesNotCauseEarlyInitialization() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		try {
			EnvironmentTestUtils.addEnvironment(context, "server.port:0",
					"security.user.password:password");
			context.register(Config.class);
			context.refresh();
			int port = context.getEmbeddedServletContainer().getPort();
			new TestRestTemplate("user", "password")
					.getForEntity("http://localhost:" + port, Object.class);
			// If early initialization occurred a ConverterNotFoundException is thrown

		}
		finally {
			context.close();
		}

	}

	@Configuration
	@Import({ DeserializerBean.class, JacksonModuleBean.class, ExampleController.class,
			ConverterBean.class })
	@ImportAutoConfiguration({ WebMvcAutoConfiguration.class,
			JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebSecurity.class,
			SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	static class Config {

		@Bean
		public TomcatEmbeddedServletContainerFactory containerFactory() {
			TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
			factory.setPort(0);
			return factory;
		}

	}

	public static class SourceType {

		public String foo;

	}

	public static class DestinationType {

		public String bar;

	}

	@Component
	public static class JacksonModuleBean extends SimpleModule {

		private static final long serialVersionUID = 1L;

		public JacksonModuleBean(DeserializerBean myDeser) {
			addDeserializer(SourceType.class, myDeser);
		}

	}

	@Component
	public static class DeserializerBean extends StdDeserializer<SourceType> {

		@Autowired
		ConversionService conversionService;

		public DeserializerBean() {
			super(SourceType.class);
		}

		@Override
		public SourceType deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			return new SourceType();
		}

	}

	@RestController
	public static class ExampleController {

		@Autowired
		private ConversionService conversionService;

		@RequestMapping("/")
		public void convert() {
			this.conversionService.convert(new SourceType(), DestinationType.class);
		}

	}

	@Component
	public static class ConverterBean implements Converter<SourceType, DestinationType> {

		@Override
		public DestinationType convert(SourceType source) {
			return new DestinationType();
		}

	}

}
