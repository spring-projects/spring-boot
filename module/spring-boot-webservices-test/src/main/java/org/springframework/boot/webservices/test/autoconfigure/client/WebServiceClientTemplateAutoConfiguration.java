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

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.webservices.autoconfigure.client.WebServiceTemplateAutoConfiguration;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Auto-configuration for a web-client {@link WebServiceTemplate}. Used when
 * {@link AutoConfigureWebServiceClient#registerWebServiceTemplate()} is {@code true}.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 * @see AutoConfigureWebServiceClient
 */
@AutoConfiguration(after = WebServiceTemplateAutoConfiguration.class)
@ConditionalOnBooleanProperty("spring.test.webservice.client.register-web-service-template")
@ConditionalOnClass(WebServiceTemplate.class)
@ConditionalOnBean(WebServiceTemplateBuilder.class)
public final class WebServiceClientTemplateAutoConfiguration {

	@Bean
	WebServiceTemplate webServiceTemplate(WebServiceTemplateBuilder builder) {
		return builder.build();
	}

}
