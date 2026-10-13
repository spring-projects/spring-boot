/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.info;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link InfoEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@AutoConfiguration(after = InfoContributorAutoConfiguration.class)
@ConditionalOnAvailableEndpoint(InfoEndpoint.class)
public final class InfoEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	InfoEndpoint infoEndpoint(ObjectProvider<InfoContributor> infoContributors) {
		return new InfoEndpoint(infoContributors.orderedStream().toList());
	}

}
