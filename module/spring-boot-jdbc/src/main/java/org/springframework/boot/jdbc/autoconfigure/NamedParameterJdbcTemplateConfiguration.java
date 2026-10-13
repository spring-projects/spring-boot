/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Configuration for {@link NamedParameterJdbcTemplate}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnSingleCandidate(JdbcTemplate.class)
@ConditionalOnMissingBean(NamedParameterJdbcOperations.class)
class NamedParameterJdbcTemplateConfiguration {

	@Bean
	@Primary
	NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
		return new NamedParameterJdbcTemplate(jdbcTemplate);
	}

}
