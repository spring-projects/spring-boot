/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.quartz;

import org.quartz.Scheduler;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.actuate.quartz.QuartzEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.5.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Scheduler.class)
@AutoConfigureAfter(QuartzAutoConfiguration.class)
@ConditionalOnAvailableEndpoint(endpoint = QuartzEndpoint.class)
public class QuartzEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(Scheduler.class)
	@ConditionalOnMissingBean
	public QuartzEndpoint quartzEndpoint(Scheduler scheduler) {
		return new QuartzEndpoint(scheduler);
	}

	@Bean
	@ConditionalOnBean(QuartzEndpoint.class)
	@ConditionalOnMissingBean
	public QuartzEndpointWebExtension quartzEndpointWebExtension(QuartzEndpoint endpoint) {
		return new QuartzEndpointWebExtension(endpoint);
	}

}
