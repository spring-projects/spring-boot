/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.autoconfigure;

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
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceJmxConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Tadaya Tsuyukubo
 */
class DataSourceJmxConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.url=jdbc:hsqldb:mem:test-" + UUID.randomUUID())
		.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class, DataSourceAutoConfiguration.class));

	@Test
	void hikariAutoConfiguredCanUseRegisterMBeans() {
		String poolName = UUID.randomUUID().toString();
		this.contextRunner
			.withPropertyValues("spring.jmx.enabled=true", "spring.datasource.type=" + HikariDataSource.class.getName(),
					"spring.datasource.name=" + poolName, "spring.datasource.hikari.register-mbeans=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(HikariDataSource.class);
				HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
				assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
				// Ensure that the pool has been initialized, triggering MBean
				// registration
				hikariDataSource.getConnection().close();
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				validateHikariMBeansRegistration(mBeanServer, poolName, true);
			});
	}

	@Test
	void hikariAutoConfiguredWithoutDataSourceName() throws MalformedObjectNameException {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectInstance> existingInstances = mBeanServer.queryMBeans(new ObjectName("com.zaxxer.hikari:type=*"),
				null);
		this.contextRunner
			.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName(),
					"spring.datasource.hikari.register-mbeans=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(HikariDataSource.class);
				HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
				assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
				// Ensure that the pool has been initialized, triggering MBean
				// registration
				hikariDataSource.getConnection().close();
				// We can't rely on the number of MBeans so we're checking that the
				// pool and pool config MBeans were registered
				assertThat(mBeanServer.queryMBeans(new ObjectName("com.zaxxer.hikari:type=*"), null))
					.hasSize(existingInstances.size() + 2);
			});
	}

	@Test
	void hikariAutoConfiguredUsesJmxFlag() {
		String poolName = UUID.randomUUID().toString();
		this.contextRunner
			.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName(),
					"spring.jmx.enabled=false", "spring.datasource.name=" + poolName,
					"spring.datasource.hikari.register-mbeans=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(HikariDataSource.class);
				HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
				assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
				// Ensure that the pool has been initialized, triggering MBean
				// registration
				hikariDataSource.getConnection().close();
				// Hikari can still register mBeans
				validateHikariMBeansRegistration(ManagementFactory.getPlatformMBeanServer(), poolName, true);
			});
	}

	@Test
	void hikariProxiedCanUseRegisterMBeans() {
		String poolName = UUID.randomUUID().toString();
		this.contextRunner.withUserConfiguration(DataSourceProxyConfiguration.class)
			.withPropertyValues("spring.jmx.enabled=true", "spring.datasource.type=" + HikariDataSource.class.getName(),
					"spring.datasource.name=" + poolName, "spring.datasource.hikari.register-mbeans=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(javax.sql.DataSource.class);
				HikariDataSource hikariDataSource = context.getBean(javax.sql.DataSource.class)
					.unwrap(HikariDataSource.class);
				assertThat(hikariDataSource.isRegisterMbeans()).isTrue();
				// Ensure that the pool has been initialized, triggering MBean
				// registration
				hikariDataSource.getConnection().close();
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				validateHikariMBeansRegistration(mBeanServer, poolName, true);
			});
	}

	private void validateHikariMBeansRegistration(MBeanServer mBeanServer, String poolName, boolean expected)
			throws MalformedObjectNameException {
		assertThat(mBeanServer.isRegistered(new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")")))
			.isEqualTo(expected);
		assertThat(mBeanServer.isRegistered(new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")")))
			.isEqualTo(expected);
	}

	@Test
	void tomcatDoesNotExposeMBeanPoolByDefault() {
		this.contextRunner.withPropertyValues("spring.datasource.type=" + DataSource.class.getName())
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionPool.class));
	}

	@Test
	void tomcatAutoConfiguredCanExposeMBeanPool() {
		this.contextRunner
			.withPropertyValues("spring.jmx.enabled=true", "spring.datasource.type=" + DataSource.class.getName(),
					"spring.datasource.tomcat.jmx-enabled=true")
			.run((context) -> {
				assertThat(context).hasBean("dataSourceMBean");
				assertThat(context).hasSingleBean(ConnectionPool.class);
				assertThat(context.getBean(DataSourceProxy.class).createPool().getJmxPool())
					.isSameAs(context.getBean(ConnectionPool.class));
			});
	}

	@Test
	void tomcatProxiedCanExposeMBeanPool() {
		this.contextRunner.withUserConfiguration(DataSourceProxyConfiguration.class)
			.withPropertyValues("spring.jmx.enabled=true", "spring.datasource.type=" + DataSource.class.getName(),
					"spring.datasource.tomcat.jmx-enabled=true")
			.run((context) -> {
				assertThat(context).hasBean("dataSourceMBean");
				assertThat(context).getBean("dataSourceMBean").isInstanceOf(ConnectionPool.class);
			});
	}

	@Test
	void tomcatDelegateCanExposeMBeanPool() {
		this.contextRunner.withUserConfiguration(DataSourceDelegateConfiguration.class)
			.withPropertyValues("spring.jmx.enabled=true", "spring.datasource.type=" + DataSource.class.getName(),
					"spring.datasource.tomcat.jmx-enabled=true")
			.run((context) -> {
				assertThat(context).hasBean("dataSourceMBean");
				assertThat(context).getBean("dataSourceMBean").isInstanceOf(ConnectionPool.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceProxyConfiguration {

		@Bean
		static DataSourceBeanPostProcessor dataSourceBeanPostProcessor() {
			return new DataSourceBeanPostProcessor();
		}

	}

	static class DataSourceBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof javax.sql.DataSource) {
				return new ProxyFactory(bean).getProxy();
			}
			return bean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceDelegateConfiguration {

		@Bean
		static DataSourceBeanPostProcessor dataSourceBeanPostProcessor() {
			return new DataSourceBeanPostProcessor() {
				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					return (bean instanceof javax.sql.DataSource)
							? new DelegatingDataSource((javax.sql.DataSource) bean) : bean;
				}
			};
		}

	}

}
