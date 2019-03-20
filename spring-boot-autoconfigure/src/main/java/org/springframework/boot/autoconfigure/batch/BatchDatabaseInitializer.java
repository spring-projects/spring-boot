/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AbstractDatabaseInitializer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Initialize the Spring Batch schema (ignoring errors, so should be idempotent).
 *
 * @author Dave Syer
 * @author Vedran Pavic
 */
public class BatchDatabaseInitializer extends AbstractDatabaseInitializer {

	private final BatchProperties properties;

	public BatchDatabaseInitializer(DataSource dataSource, ResourceLoader resourceLoader,
			BatchProperties properties) {
		super(dataSource, resourceLoader);
		Assert.notNull(properties, "BatchProperties must not be null");
		this.properties = properties;
	}

	@Override
	protected boolean isEnabled() {
		return this.properties.getInitializer().isEnabled();
	}

	@Override
	protected String getSchemaLocation() {
		return this.properties.getSchema();
	}

	@Override
	protected String getDatabaseName() {
		String databaseName = super.getDatabaseName();
		if ("oracle".equals(databaseName)) {
			return "oracle10g";
		}
		return databaseName;
	}

}
