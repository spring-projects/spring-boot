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

import org.junit.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfigurationTests.WebSecurity;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfigurationEarlyInitializationTests.ConverterBean;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfigurationEarlyInitializationTests.DeserializerBean;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfigurationEarlyInitializationTests.ExampleController;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfigurationEarlyInitializationTests.JacksonModuleBean;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Tests for {@link SecurityFilterAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class SecurityFilterAutoConfigurationTests {

	@Test
	public void filterAutoConfigurationWorksWithoutSecurityAutoConfiguration()
			throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		try {
			context.register(Config.class);
			context.refresh();
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
			SecurityFilterAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	static class Config {

	}

}
