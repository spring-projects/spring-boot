/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RestTemplateCustomizer} that configures the {@link RestTemplate} to log requests
 * and responses.
 *
 * @author Mark Hobson
 * @since 2.0.0
 */
public class LoggingCustomizer implements RestTemplateCustomizer {

	private final Log log;

	public LoggingCustomizer() {
		this(LogFactory.getLog(LoggingCustomizer.class));
	}

	public LoggingCustomizer(Log log) {
		this.log = log;
	}

	@Override
	public void customize(RestTemplate restTemplate) {
		restTemplate.setRequestFactory(
				new BufferingClientHttpRequestFactory(restTemplate.getRequestFactory()));
		restTemplate.getInterceptors().add(new LoggingInterceptor(this.log));
	}
}
