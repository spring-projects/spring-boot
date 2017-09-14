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

package org.springframework.boot.actuate.autoconfigure.trace;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.actuate.trace.WebRequestTraceFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebRequestTraceFilter
 * tracing}.
 *
 * @author Dave Syer
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, ServletRegistration.class })
@AutoConfigureAfter(TraceRepositoryAutoConfiguration.class)
@ConditionalOnProperty(prefix = "management.trace.filter", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TraceEndpointProperties.class)
public class TraceWebFilterAutoConfiguration {

	private final TraceRepository traceRepository;

	private final TraceEndpointProperties endpointProperties;

	private final ErrorAttributes errorAttributes;

	public TraceWebFilterAutoConfiguration(TraceRepository traceRepository,
			TraceEndpointProperties endpointProperties,
			ObjectProvider<ErrorAttributes> errorAttributes) {
		this.traceRepository = traceRepository;
		this.endpointProperties = endpointProperties;
		this.errorAttributes = errorAttributes.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	public WebRequestTraceFilter webRequestLoggingFilter() {
		WebRequestTraceFilter filter = new WebRequestTraceFilter(this.traceRepository,
				this.endpointProperties.getInclude());
		if (this.errorAttributes != null) {
			filter.setErrorAttributes(this.errorAttributes);
		}
		return filter;
	}

}
