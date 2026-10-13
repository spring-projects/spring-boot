/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web.mappings;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.web.mappings.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.webmvc.actuate.web.mappings.DispatcherServletsMappingDescriptionProvider;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to describe Web MVC
 * {@link MappingDescriptionProvider mappings}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(after = DispatcherServletAutoConfiguration.class)
@ConditionalOnClass({ ConditionalOnAvailableEndpoint.class, DispatcherServlet.class, MappingsEndpoint.class })
@ConditionalOnAvailableEndpoint(MappingsEndpoint.class)
@ConditionalOnBean(DispatcherServlet.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public final class WebMvcMappingsAutoConfiguration {

	@Bean
	DispatcherServletsMappingDescriptionProvider dispatcherServletMappingDescriptionProvider() {
		return new DispatcherServletsMappingDescriptionProvider();
	}

}
