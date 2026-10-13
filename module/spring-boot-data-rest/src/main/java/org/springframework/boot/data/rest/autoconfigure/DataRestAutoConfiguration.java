/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.rest.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data REST.
 * <p>
 * Activates when the application is a web application and no
 * {@link RepositoryRestMvcConfiguration} is found.
 * <p>
 * Once in effect, the auto-configuration allows to configure any property of
 * {@link RepositoryRestConfiguration} using the {@code spring.data.rest} prefix.
 *
 * @author Rob Winch
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(before = DataWebAutoConfiguration.class, after = JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnMissingBean(RepositoryRestMvcConfiguration.class)
@ConditionalOnClass(RepositoryRestMvcConfiguration.class)
@EnableConfigurationProperties(DataRestProperties.class)
@Import(RepositoryRestMvcConfiguration.class)
public final class DataRestAutoConfiguration {

	@Bean
	SpringBootRepositoryRestConfigurer springBootRepositoryRestConfigurer(
			ObjectProvider<JsonMapperBuilderCustomizer> jsonMapperBuilderCustomizers, DataRestProperties properties) {
		return new SpringBootRepositoryRestConfigurer(jsonMapperBuilderCustomizers.orderedStream().toList(),
				properties);
	}

}
