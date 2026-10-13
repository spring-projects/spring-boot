/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.quartz.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.init.DatabaseInitializationProperties;

/**
 * Configuration properties for the Quartz Scheduler integration when using a JDBC job
 * store.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("spring.quartz.jdbc")
public class QuartzJdbcProperties extends DatabaseInitializationProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/quartz/impl/"
			+ "jdbcjobstore/tables_@@platform@@.sql";

	/**
	 * Prefixes for single-line comments in SQL initialization scripts.
	 */
	private List<String> commentPrefix = new ArrayList<>(Arrays.asList("#", "--"));

	public List<String> getCommentPrefix() {
		return this.commentPrefix;
	}

	public void setCommentPrefix(List<String> commentPrefix) {
		this.commentPrefix = commentPrefix;
	}

	@Override
	public String getDefaultSchemaLocation() {
		return DEFAULT_SCHEMA_LOCATION;
	}

}
