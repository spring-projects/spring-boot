/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Configuration for {@link JdbcTemplateConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(JdbcOperations.class)
class JdbcTemplateConfiguration {

	@Bean
	@Primary
	JdbcTemplate jdbcTemplate(DataSource dataSource, JdbcProperties properties,
			ObjectProvider<SQLExceptionTranslator> sqlExceptionTranslator) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		JdbcProperties.Template template = properties.getTemplate();
		jdbcTemplate.setIgnoreWarnings(template.isIgnoreWarnings());
		jdbcTemplate.setFetchSize(template.getFetchSize());
		jdbcTemplate.setMaxRows(template.getMaxRows());
		if (template.getQueryTimeout() != null) {
			jdbcTemplate.setQueryTimeout((int) template.getQueryTimeout().getSeconds());
		}
		jdbcTemplate.setSkipResultsProcessing(template.isSkipResultsProcessing());
		jdbcTemplate.setSkipUndeclaredResults(template.isSkipUndeclaredResults());
		jdbcTemplate.setResultsMapCaseInsensitive(template.isResultsMapCaseInsensitive());
		sqlExceptionTranslator.ifUnique(jdbcTemplate::setExceptionTranslator);
		return jdbcTemplate;
	}

}
