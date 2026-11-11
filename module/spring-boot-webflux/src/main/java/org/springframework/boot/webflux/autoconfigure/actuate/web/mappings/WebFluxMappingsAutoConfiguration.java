/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure.actuate.web.mappings;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.webflux.actuate.web.mappings.DispatcherHandlersMappingDescriptionProvider;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to {@link MappingDescriptionProvider
 * describe} WebFlux mappings.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(after = WebFluxAutoConfiguration.class)
@ConditionalOnClass({ ConditionalOnAvailableEndpoint.class, DispatcherHandler.class, MappingsEndpoint.class })
@ConditionalOnAvailableEndpoint(MappingsEndpoint.class)
@ConditionalOnBean(DispatcherHandler.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
public final class WebFluxMappingsAutoConfiguration {

	@Bean
	DispatcherHandlersMappingDescriptionProvider dispatcherHandlersMappingDescriptionProvider() {
		return new DispatcherHandlersMappingDescriptionProvider();
	}

}
