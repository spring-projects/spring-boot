/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JdbcClient}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnSingleCandidate(NamedParameterJdbcTemplate.class)
@ConditionalOnMissingBean(JdbcClient.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
public final class JdbcClientAutoConfiguration {

	@Bean
	JdbcClient jdbcClient(NamedParameterJdbcTemplate jdbcTemplate) {
		return JdbcClient.create(jdbcTemplate);
	}

}
