/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.webservices.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.CollectionUtils;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebServiceTemplate}.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
@Configuration
@ConditionalOnClass({ WebServiceTemplate.class, Unmarshaller.class, Marshaller.class })
public class WebServiceTemplateAutoConfiguration {

	private final ObjectProvider<List<WebServiceTemplateCustomizer>> webServiceTemplateCustomizers;

	public WebServiceTemplateAutoConfiguration(
			ObjectProvider<List<WebServiceTemplateCustomizer>> webServiceTemplateCustomizers) {
		this.webServiceTemplateCustomizers = webServiceTemplateCustomizers;
	}

	@Bean
	@ConditionalOnMissingBean
	public WebServiceTemplateBuilder webServiceTemplateBuilder() {
		WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder();
		List<WebServiceTemplateCustomizer> customizers = this.webServiceTemplateCustomizers
				.getIfAvailable();
		if (!CollectionUtils.isEmpty(customizers)) {
			customizers = new ArrayList<>(customizers);
			AnnotationAwareOrderComparator.sort(customizers);
			builder = builder.customizers(customizers);
		}
		return builder;
	}

}
