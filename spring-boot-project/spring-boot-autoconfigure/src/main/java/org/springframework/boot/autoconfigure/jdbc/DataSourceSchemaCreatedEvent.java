/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.springframework.context.ApplicationEvent;

/**
 * {@link ApplicationEvent} used internally to indicate that the schema of a new
 * {@link DataSource} has been created. This happens when {@literal schema-*.sql} files
 * are executed or when Hibernate initializes the database.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@SuppressWarnings("serial")
public class DataSourceSchemaCreatedEvent extends ApplicationEvent {

	/**
	 * Create a new {@link DataSourceSchemaCreatedEvent}.
	 * @param source the source {@link DataSource}.
	 */
	public DataSourceSchemaCreatedEvent(DataSource source) {
		super(source);
	}

}
