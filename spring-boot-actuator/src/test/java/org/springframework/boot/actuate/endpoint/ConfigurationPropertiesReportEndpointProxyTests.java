/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint} when used against a proxy
 * class.
 *
 * @author Phillip Webb
 */
public class ConfigurationPropertiesReportEndpointProxyTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testWithProxyClass() throws Exception {
		this.context.register(Config.class, SqlExecutor.class);
		this.context.refresh();
		Map<String, Object> report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class).invoke();
		assertThat(report.toString(), containsString("prefix=executor.sql"));
	}

	@Configuration
	@EnableTransactionManagement(proxyTargetClass = false)
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
					.build();
		}

	}

	public interface Executor {

		void execute();

	}

	public static abstract class AbstractExecutor implements Executor {

	}

	@Component
	@ConfigurationProperties("executor.sql")
	public static class SqlExecutor extends AbstractExecutor {

		@Override
		@Transactional(propagation = Propagation.REQUIRES_NEW)
		public void execute() {
		}

	}

}
