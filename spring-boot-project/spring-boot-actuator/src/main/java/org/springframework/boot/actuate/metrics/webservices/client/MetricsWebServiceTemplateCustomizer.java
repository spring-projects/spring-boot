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
package org.springframework.boot.actuate.metrics.webservices.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

/**
 * {@link WebServiceTemplateCustomizer} that configures the {@link WebServiceTemplate} to
 * record request metrics.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
public class MetricsWebServiceTemplateCustomizer implements WebServiceTemplateCustomizer {

	private final MetricsClientInterceptor interceptor;

	/**
	 * Creates a new {@code MetricsWebServiceTemplateInterceptor} that will record metrics
	 * using the given {@code meterRegistry} with tags provided by the given
	 * {@code tagProvider}.
	 * @param meterRegistry the meter registry
	 * @param tagProvider the tag provider
	 * @param metricName the name of the recorded metric
	 */
	public MetricsWebServiceTemplateCustomizer(MeterRegistry meterRegistry,
			WebServiceTemplateExchangeTagsProvider tagProvider, String metricName) {
		this.interceptor = new MetricsClientInterceptor(meterRegistry, tagProvider,
				metricName);
	}

	@Override
	public void customize(WebServiceTemplate webServiceTemplate) {
		List<ClientInterceptor> existingInterceptors = getInterceptors(
				webServiceTemplate);
		if (!existingInterceptors.contains(this.interceptor)) {
			List<ClientInterceptor> interceptors = new ArrayList<>();
			interceptors.add(this.interceptor);
			interceptors.addAll(existingInterceptors);
			webServiceTemplate
					.setInterceptors(interceptors.toArray(new ClientInterceptor[0]));
		}
	}

	private List<ClientInterceptor> getInterceptors(
			WebServiceTemplate webServiceTemplate) {
		return (webServiceTemplate.getInterceptors() != null
				? Arrays.asList(webServiceTemplate.getInterceptors())
				: Collections.emptyList());
	}

}
