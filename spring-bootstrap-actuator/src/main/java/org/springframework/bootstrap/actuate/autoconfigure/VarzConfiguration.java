/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.autoconfigure;

import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.metrics.MetricRepository;
import org.springframework.bootstrap.actuate.varz.PublicMetrics;
import org.springframework.bootstrap.actuate.varz.VanillaPublicMetrics;
import org.springframework.bootstrap.actuate.varz.VarzEndpoint;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for /varz endpoint.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnMissingBean({ VarzEndpoint.class })
public class VarzConfiguration {

	@Autowired
	private MetricRepository repository;

	@Autowired(required = false)
	private PublicMetrics metrics;

	@Bean
	public VarzEndpoint varzEndpoint() {
		if (this.metrics == null) {
			this.metrics = new VanillaPublicMetrics(this.repository);
		}
		return new VarzEndpoint(this.metrics);
	}

}
