/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
