/*
 * Copyright 2012-2014 the original author or authors.
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
 * {@link ApplicationEvent} used internally to trigger {@link DataSource} initialization.
 * Initialization can occur when {@literal schema-*.sql} files are executed or when
 * external libraries (e.g. JPA) initialize the database.
 * 
 * @author Dave Syer
 * @see DataSourceInitializer
 * @since 1.1.0
 */
@SuppressWarnings("serial")
public class DataSourceInitializedEvent extends ApplicationEvent {

	/**
	 * Create a new {@link DataSourceInitializedEvent}.
	 * @param source the source {@link DataSource}.
	 */
	public DataSourceInitializedEvent(DataSource source) {
		super(source);
	}

}
