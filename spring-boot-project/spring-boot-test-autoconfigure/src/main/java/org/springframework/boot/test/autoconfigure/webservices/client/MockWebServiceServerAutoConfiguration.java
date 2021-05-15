/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.webservices.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceMessageSender;
import org.springframework.ws.test.client.MockWebServiceServer;

/**
 * Auto-configuration for {@link MockWebServiceServer} support.
 *
 * @author Dmytro Nosan
 * @see AutoConfigureMockWebServiceServer
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.test.webservice.client.mockserver", name = "enabled")
@ConditionalOnClass({ MockWebServiceServer.class, WebServiceTemplate.class })
public class MockWebServiceServerAutoConfiguration {

	@Bean
	public TestMockWebServiceServer mockWebServiceServer() {
		return new TestMockWebServiceServer(new MockWebServiceMessageSender());
	}

	@Bean
	public MockWebServiceServerWebServiceTemplateCustomizer mockWebServiceServerWebServiceTemplateCustomizer(
			TestMockWebServiceServer mockWebServiceServer) {
		return new MockWebServiceServerWebServiceTemplateCustomizer(mockWebServiceServer);
	}

}
