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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.webservices.client.WebServiceTemplateAutoConfiguration;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Auto-configuration for a web-client {@link WebServiceTemplate}. Used when
 * {@link AutoConfigureWebServiceClient#registerWebServiceTemplate()} is {@code true}.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 * @see AutoConfigureWebServiceClient
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.test.webservice.client", name = "register-web-service-template")
@AutoConfigureAfter(WebServiceTemplateAutoConfiguration.class)
@ConditionalOnClass(WebServiceTemplate.class)
@ConditionalOnBean(WebServiceTemplateBuilder.class)
public class WebServiceClientTemplateAutoConfiguration {

	@Bean
	public WebServiceTemplate webServiceTemplate(WebServiceTemplateBuilder builder) {
		return builder.build();
	}

}
