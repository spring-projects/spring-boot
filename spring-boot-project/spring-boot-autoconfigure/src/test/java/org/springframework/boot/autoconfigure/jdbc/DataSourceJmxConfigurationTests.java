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

package org.springframework.boot.autoconfigure.jdbc;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.UUID;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.jmx.ConnectionPool;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceJmxConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class DataSourceJmxConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.datasource.url="
					+ "jdbc:hsqldb:mem:test-" + UUID.randomUUID())
			.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class,
					DataSourceAutoConfiguration.class));

	@Test
	public void hikariAutoConfiguredCanUseRegisterMBeans() {
		String poolName = UUID.randomUUID().toString();
		this.contextRunner.withPropertyValues(
				"spring.datasource.type=" + HikariDataSource.class.getName(),
				"spring.datasource.name=" + poolName,
				"spring.datasource.hikari.register-mbeans=true").run((context) -> {
			assertThat(context).hasSingleBean(HikariDataSource.class);
			assertThat(context.getBean(HikariDataSource.class).isRegisterMbeans())
					.isTrue();
			MBeanServer mBeanServer = context.getBean(MBeanServer.class);
			validateHikariMBeansRegistration(mBeanServer, poolName, true);
		});
	}

	@Test
	public void hikariAutoConfiguredWithoutDataSourceName()
			throws MalformedObjectNameException {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectInstance> existingInstances = mBeanServer
				.queryMBeans(new ObjectName("com.zaxxer.hikari:type=*"), null);
		this.contextRunner.withPropertyValues(
				"spring.datasource.type=" + HikariDataSource.class.getName(),
				"spring.datasource.hikari.register-mbeans=true").run((context) -> {
			assertThat(context).hasSingleBean(HikariDataSource.class);
			assertThat(context.getBean(HikariDataSource.class).isRegisterMbeans())
					.isTrue();
			// We can't rely on the number of MBeans so we're checking that the pool and pool
			// config MBeans were registered
			assertThat(mBeanServer
					.queryMBeans(new ObjectName("com.zaxxer.hikari:type=*"), null).size())
					.isEqualTo(existingInstances.size() + 2);
		});
	}

	@Test
	public void hikariAutoConfiguredUsesJmsFlag() {
		String poolName = UUID.randomUUID().toString();
		this.contextRunner.withPropertyValues(
				"spring.datasource.type=" + HikariDataSource.class.getName(),
				"spring.jmx.enabled=false", "spring.datasource.name=" + poolName,
				"spring.datasource.hikari.register-mbeans=true").run((context) -> {
			assertThat(context).hasSingleBean(HikariDataSource.class);
			assertThat(context.getBean(HikariDataSource.class).isRegisterMbeans())
					.isTrue();
			// Hikari can still register mBeans
			validateHikariMBeansRegistration(ManagementFactory.getPlatformMBeanServer(),
					poolName, true);
		});
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
		this.contextRunner
				.withPropertyValues("spring.datasource.type=" + DataSource.class.getName())
				.run((context) ->
						assertThat(context).doesNotHaveBean(ConnectionPool.class));
	}

	@Test
	public void tomcatAutoConfiguredCanExposeMBeanPool() {
		this.contextRunner.withPropertyValues(
				"spring.datasource.type=" + DataSource.class.getName(),
				"spring.datasource.jmx-enabled=true").run((context) -> {
			assertThat(context).hasSingleBean(ConnectionPool.class);
			assertThat(context.getBean(DataSourceProxy.class).createPool().getJmxPool())
					.isSameAs(context.getBean(ConnectionPool.class));
		});
	}

}
