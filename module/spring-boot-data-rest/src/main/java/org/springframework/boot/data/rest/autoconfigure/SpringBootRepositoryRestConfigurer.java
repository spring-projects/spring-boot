/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.rest.autoconfigure;

import java.util.List;

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * A {@code RepositoryRestConfigurer} that applies configuration items from the
 * {@code spring.data.rest} namespace to Spring Data REST. Also, if any
 * {@link JsonMapperBuilderCustomizer JsonMapperBuilderCustomizers} are available, they
 * are used to configure Spring Data REST's {@link JsonMapper JsonMappers}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@Order(0)
class SpringBootRepositoryRestConfigurer implements RepositoryRestConfigurer {

	private final List<JsonMapperBuilderCustomizer> jsonMapperBuilderCustomizers;

	private final DataRestProperties properties;

	SpringBootRepositoryRestConfigurer(List<JsonMapperBuilderCustomizer> jsonMapperBuilderCustomizers,
			DataRestProperties properties) {
		this.jsonMapperBuilderCustomizers = jsonMapperBuilderCustomizers;
		this.properties = properties;
	}

	@Override
	public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
		this.properties.applyTo(config);
	}

	@Override
	public void configureJacksonObjectMapper(MapperBuilder<?, ?> mapperBuilder) {
		if (mapperBuilder instanceof JsonMapper.Builder jsonMapperBuilder) {
			this.jsonMapperBuilderCustomizers.forEach((customizer) -> customizer.customize(jsonMapperBuilder));
		}
	}

}
