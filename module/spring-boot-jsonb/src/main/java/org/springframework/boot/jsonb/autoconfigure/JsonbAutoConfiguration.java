/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jsonb.autoconfigure;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JSON-B.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Jsonb.class)
@ConditionalOnResource(resources = { "classpath:META-INF/services/jakarta.json.bind.spi.JsonbProvider",
		"classpath:META-INF/services/jakarta.json.spi.JsonProvider" })
public final class JsonbAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Jsonb jsonb() {
		return JsonbBuilder.create();
	}

}
