/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.jetty.jsp;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for JSP application.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { SampleWebJspApplicationTests.JettyCustomizerConfig.class, SampleJettyJspApplication.class })
class SampleWebJspApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testJspWithEl() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("/resources/text.txt");
	}

	@Configuration(proxyBeanMethods = false)
	static class JettyCustomizerConfig {

		// To allow aliased resources on Concourse Windows CI (See gh-15553) to be served
		// as static resources.
		@Bean
		JettyServerCustomizer jettyServerCustomizer() {
			return (server) -> {
				ContextHandler handler = (ContextHandler) server.getHandler();
				handler.addAliasCheck(new ContextHandler.ApproveAliases());
			};
		}

	}

}
