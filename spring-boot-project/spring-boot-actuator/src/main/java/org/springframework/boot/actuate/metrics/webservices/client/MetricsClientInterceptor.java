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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.util.Assert;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;

/**
 * {@link ClientInterceptor} applied via a {@link MetricsWebServiceTemplateCustomizer} to
 * record metrics.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
class MetricsClientInterceptor extends ClientInterceptorAdapter {

	private static final String METRICS_ATTRIBUTE = MetricsClientInterceptor.class
			.getName() + ".TIMER";

	private final MeterRegistry meterRegistry;

	private final WebServiceTemplateExchangeTagsProvider tagProvider;

	private final String metricName;

	MetricsClientInterceptor(MeterRegistry meterRegistry,
			WebServiceTemplateExchangeTagsProvider tagProvider, String metricName) {
		this.tagProvider = tagProvider;
		this.meterRegistry = meterRegistry;
		this.metricName = metricName;
	}

	@Override
	public boolean handleRequest(MessageContext messageContext)
			throws WebServiceClientException {
		messageContext.setProperty(METRICS_ATTRIBUTE, Timer.start(this.meterRegistry));
		return true;
	}

	@Override
	public void afterCompletion(MessageContext messageContext, Exception ex)
			throws WebServiceClientException {

		Timer.Sample sample = (Timer.Sample) messageContext
				.getProperty(METRICS_ATTRIBUTE);

		Assert.state(sample != null, "Timer sample must not be null");

		try {
			sample.stop(Timer.builder(this.metricName)
					.tags(this.tagProvider.getTags(messageContext, ex))
					.description("Timer of WebServiceTemplate.")
					.register(this.meterRegistry));
		}
		finally {
			messageContext.removeProperty(METRICS_ATTRIBUTE);
		}
	}

}
