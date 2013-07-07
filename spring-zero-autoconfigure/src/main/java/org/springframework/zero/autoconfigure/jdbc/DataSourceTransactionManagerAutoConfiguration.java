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

package org.springframework.zero.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.zero.context.annotation.EnableAutoConfiguration;
import org.springframework.zero.context.condition.ConditionalOnBean;
import org.springframework.zero.context.condition.ConditionalOnClass;
import org.springframework.zero.context.condition.ConditionalOnMissingBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataSourceTransactionManager}.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ JdbcTemplate.class, PlatformTransactionManager.class })
public class DataSourceTransactionManagerAutoConfiguration implements Ordered {

	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	@Autowired(required = false)
	private DataSource dataSource;

	@Bean
	@ConditionalOnMissingBean(name = "transactionManager")
	@ConditionalOnBean(DataSource.class)
	public PlatformTransactionManager transactionManager() {
		return new DataSourceTransactionManager(this.dataSource);
	}

}
