/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.jdbc;

import org.springframework.boot.sql.init.DatabaseInitializationMode;

/**
 * Supported {@link javax.sql.DataSource} initialization modes.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see AbstractDataSourceInitializer
 * @deprecated since 2.6.0 for removal in 3.0.0 in favor of
 * {@link DatabaseInitializationMode}
 */
@Deprecated
public enum DataSourceInitializationMode {

	/**
	 * Always initialize the datasource.
	 */
	ALWAYS,

	/**
	 * Only initialize an embedded datasource.
	 */
	EMBEDDED,

	/**
	 * Do not initialize the datasource.
	 */
	NEVER

}
