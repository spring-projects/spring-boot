/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure.actuate.web.mappings;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.servlet.actuate.web.mappings.FiltersMappingDescriptionProvider;
import org.springframework.boot.servlet.actuate.web.mappings.ServletsMappingDescriptionProvider;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to describe Servlet-related
 * {@link MappingDescriptionProvider mappings}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ConditionalOnAvailableEndpoint.class, MappingsEndpoint.class })
@ConditionalOnAvailableEndpoint(MappingsEndpoint.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public final class ServletMappingsAutoConfiguration {

	@Bean
	ServletsMappingDescriptionProvider servletMappingDescriptionProvider() {
		return new ServletsMappingDescriptionProvider();
	}

	@Bean
	FiltersMappingDescriptionProvider filterMappingDescriptionProvider() {
		return new FiltersMappingDescriptionProvider();
	}

}
