/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.autoconfigure.batch;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Initialize the Spring Batch schema (ignoring errors, so should be idempotent).
 * 
 * @author Dave Syer
 */
@Component
public class BatchDatabaseInitializer {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${spring.batch.schema:classpath:org/springframework/batch/core/schema-@@platform@@.sql}")
	private String schemaLocation = "classpath:org/springframework/batch/core/schema-@@platform@@.sql";

	@PostConstruct
	protected void initialize() throws Exception {
		String platform = DatabaseType.fromMetaData(this.dataSource).toString()
				.toLowerCase();
		if ("hsql".equals(platform)) {
			platform = "hsqldb";
		}
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.addScript(this.resourceLoader.getResource(this.schemaLocation.replace(
				"@@platform@@", platform)));
		populator.setContinueOnError(true);
		DatabasePopulatorUtils.execute(populator, this.dataSource);
	}
}
