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

package org.springframework.boot.actuate.autoconfigure.metrics.webservices.client;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.webservices.client.DefaultWebServiceTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.webservices.client.MetricsWebServiceTemplateCustomizer;
import org.springframework.boot.actuate.metrics.webservices.client.WebServiceTemplateExchangeTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.webservices.client.WebServiceTemplateAutoConfiguration;
import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link WebServiceTemplate}-related metrics.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class,
		WebServiceTemplateAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass(WebServiceTemplate.class)
@ConditionalOnBean(MeterRegistry.class)
public class WebServiceTemplateMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(WebServiceTemplateExchangeTagsProvider.class)
	public DefaultWebServiceTemplateExchangeTagsProvider webServiceTemplateTagsProvider() {
		return new DefaultWebServiceTemplateExchangeTagsProvider();
	}

	@Bean
	public WebServiceTemplateCustomizer metricsWebServiceTemplateCustomizer(
			MeterRegistry meterRegistry,
			WebServiceTemplateExchangeTagsProvider webServiceTemplateExchangeTagsProvider,
			MetricsProperties properties) {
		return new MetricsWebServiceTemplateCustomizer(meterRegistry,
				webServiceTemplateExchangeTagsProvider,
				properties.getWebServices().getClient().getRequestsMetricName());
	}

}
