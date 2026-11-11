/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.availability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for
 * {@link ApplicationAvailabilityBean}.
 *
 * @author Brian Clozel
 * @author Taeik Lim
 * @since 2.3.0
 */
@AutoConfiguration
public final class ApplicationAvailabilityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ApplicationAvailability.class)
	ApplicationAvailabilityBean applicationAvailability() {
		return new ApplicationAvailabilityBean();
	}

}
