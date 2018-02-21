/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.jdbc;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

/**
 * Strategy interface used to wrap an {@link XADataSource} enrolling it with a JTA
 * {@link TransactionManager}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface XADataSourceWrapper {

	/**
	 * Wrap the specific {@link XADataSource} and enroll it with a JTA
	 * {@link TransactionManager}.
	 * @param dataSource the data source to wrap
	 * @return the wrapped data source
	 * @throws Exception if the data source cannot be wrapped
	 */
	DataSource wrapDataSource(XADataSource dataSource) throws Exception;

}
