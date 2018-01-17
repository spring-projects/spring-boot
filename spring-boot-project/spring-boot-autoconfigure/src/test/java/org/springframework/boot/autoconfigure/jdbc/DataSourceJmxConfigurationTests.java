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

import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.UUID;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.jmx.ConnectionPool;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceJmxConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class DataSourceJmxConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void hikariAutoConfiguredCanUseRegisterMBeans()
			throws MalformedObjectNameException {
		String poolName = UUID.randomUUID().toString();
		load("spring.datasource.type=" + HikariDataSource.class.getName(),
				"spring.datasource.name=" + poolName,
				"spring.datasource.hikari.register-mbeans=true");
		assertThat(this.context.getBeansOfType(HikariDataSource.class)).hasSize(1);
		assertThat(this.context.getBean(HikariDataSource.class).isRegisterMbeans())
				.isTrue();
		MBeanServer mBeanServer = this.context.getBean(MBeanServer.class);
		validateHikariMBeansRegistration(mBeanServer, poolName, true);
	}

	@Test
	public void hikariAutoConfiguredUsesJmsFlag() throws MalformedObjectNameException {
		String poolName = UUID.randomUUID().toString();
		load("spring.datasource.type=" + HikariDataSource.class.getName(),
				"spring.jmx.enabled=false", "spring.datasource.name=" + poolName,
				"spring.datasource.hikari.register-mbeans=true");
		assertThat(this.context.getBeansOfType(HikariDataSource.class)).hasSize(1);
		assertThat(this.context.getBean(HikariDataSource.class).isRegisterMbeans())
				.isTrue();
		// Hikari can still register mBeans
		validateHikariMBeansRegistration(ManagementFactory.getPlatformMBeanServer(),
				poolName, true);
	}

	private void validateHikariMBeansRegistration(MBeanServer mBeanServer,
			String poolName, boolean expected) throws MalformedObjectNameException {
		assertThat(mBeanServer.isRegistered(
				new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")")))
						.isEqualTo(expected);
		assertThat(mBeanServer.isRegistered(
				new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")")))
						.isEqualTo(expected);
	}

	@Test
	public void tomcatDoesNotExposeMBeanPoolByDefault() {
		load("spring.datasource.type=" + DataSource.class.getName());
		assertThat(this.context.getBeansOfType(ConnectionPool.class)).isEmpty();
	}

	@Test
	public void tomcatAutoConfiguredCanExposeMBeanPool() throws SQLException {
		load("spring.datasource.type=" + DataSource.class.getName(),
				"spring.datasource.jmx-enabled=true");
		assertThat(this.context.getBeansOfType(ConnectionPool.class)).hasSize(1);
		assertThat(this.context.getBean(DataSourceProxy.class).createPool().getJmxPool())
				.isSameAs(this.context.getBean(ConnectionPool.class));
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		String jdbcUrl = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
		TestPropertyValues.of(environment).and("spring.datasource.url=" + jdbcUrl)
				.applyTo(context);
		if (config != null) {
			context.register(config);
		}
		context.register(JmxAutoConfiguration.class, DataSourceAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

}
