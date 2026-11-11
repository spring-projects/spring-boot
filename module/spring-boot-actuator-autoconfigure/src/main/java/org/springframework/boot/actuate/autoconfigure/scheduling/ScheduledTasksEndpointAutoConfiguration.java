/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.scheduling;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ScheduledTasksEndpoint}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(ScheduledTasksEndpoint.class)
public final class ScheduledTasksEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ScheduledTasksEndpoint scheduledTasksEndpoint(ObjectProvider<ScheduledTaskHolder> holders) {
		return new ScheduledTasksEndpoint(holders.orderedStream().toList());
	}

}
