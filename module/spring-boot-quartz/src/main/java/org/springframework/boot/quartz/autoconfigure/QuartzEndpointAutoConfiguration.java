/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.quartz.autoconfigure;

import org.quartz.Scheduler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.quartz.actuate.endpoint.QuartzEndpoint;
import org.springframework.boot.quartz.actuate.endpoint.QuartzEndpointWebExtension;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = QuartzAutoConfiguration.class)
@ConditionalOnClass({ Scheduler.class, QuartzEndpoint.class, ConditionalOnAvailableEndpoint.class })
@ConditionalOnAvailableEndpoint(QuartzEndpoint.class)
@EnableConfigurationProperties(QuartzEndpointProperties.class)
public final class QuartzEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(Scheduler.class)
	@ConditionalOnMissingBean
	QuartzEndpoint quartzEndpoint(Scheduler scheduler, ObjectProvider<SanitizingFunction> sanitizingFunctions) {
		return new QuartzEndpoint(scheduler, sanitizingFunctions.orderedStream().toList());
	}

	@Bean
	@ConditionalOnBean(QuartzEndpoint.class)
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint(exposure = EndpointExposure.WEB)
	QuartzEndpointWebExtension quartzEndpointWebExtension(QuartzEndpoint endpoint,
			QuartzEndpointProperties properties) {
		return new QuartzEndpointWebExtension(endpoint, properties.getShowValues(), properties.getRoles());
	}

}
