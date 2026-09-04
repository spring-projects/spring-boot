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

package smoketest.tomcat;

import org.junit.jupiter.api.Test;
import smoketest.tomcat.service.HelloWorldService;
import smoketest.tomcat.web.SampleController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class NonAutoConfigurationSampleTomcatApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testHome() {
		this.restTestClient.get().uri("/").exchangeSuccessfully().expectBody(String.class).isEqualTo("Hello World");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ TomcatServletWebServerAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	@ComponentScan(basePackageClasses = { SampleController.class, HelloWorldService.class })
	public static class NonAutoConfigurationSampleTomcatApplication {

		public static void main(String[] args) {
			SpringApplication.run(SampleTomcatApplication.class, args);
		}

	}

}
