/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.actuate.quartz.QuartzEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.5.0
 */
@AutoConfiguration(after = QuartzAutoConfiguration.class)
@ConditionalOnClass(Scheduler.class)
@ConditionalOnAvailableEndpoint(endpoint = QuartzEndpoint.class)
@EnableConfigurationProperties(QuartzEndpointProperties.class)
public class QuartzEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(Scheduler.class)
	@ConditionalOnMissingBean
	public QuartzEndpoint quartzEndpoint(Scheduler scheduler, ObjectProvider<SanitizingFunction> sanitizingFunctions) {
		return new QuartzEndpoint(scheduler, sanitizingFunctions.orderedStream().toList());
	}

	@Bean
	@ConditionalOnBean(QuartzEndpoint.class)
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint(exposure = { EndpointExposure.WEB, EndpointExposure.CLOUD_FOUNDRY })
	public QuartzEndpointWebExtension quartzEndpointWebExtension(QuartzEndpoint endpoint,
			QuartzEndpointProperties properties) {
		return new QuartzEndpointWebExtension(endpoint, properties.getShowValues(), properties.getRoles());
	}

}
