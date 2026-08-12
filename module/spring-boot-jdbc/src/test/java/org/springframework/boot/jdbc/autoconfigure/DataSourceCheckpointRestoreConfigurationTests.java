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

import java.util.Random;

import net.bytebuddy.ByteBuddy;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceCheckpointRestoreConfiguration.DataSourceCheckpointRestoreLifecycleRegistry;
import org.springframework.boot.jdbc.autoconfigure.DataSourceCheckpointRestoreConfiguration.Hikari.HikariCheckpointRestoreLifecycleRegistry;
import org.springframework.boot.jdbc.autoconfigure.DataSourceCheckpointRestoreConfiguration.OracleUcp.OracleUcpCheckpointRestoreLifecycleRegistry;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Fabio Grassi
 */
public class DataSourceCheckpointRestoreConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceCheckpointRestoreConfiguration.class))
		.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());

	@Test
	void whenCracIsNotAvailableNoLifeCycleIsInstrumented() {
		this.contextRunner.run((context) -> {
			final ClassLoader cl = context.getClassLoader();
			assertAll("When CRaC is not available, no life cycle is instrumented",
					() -> assertFalse(ClassUtils.isPresent("org.crac.Resource", cl)),
					() -> assertTrue(ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", cl)),
					() -> assertTrue(ClassUtils.isPresent("oracle.ucp.jdbc.PoolDataSourceImpl", cl)),
					() -> assertTrue(ClassUtils.isPresent("oracle.jdbc.OracleConnection", cl)),
					//
					() -> assertThat(context).doesNotHaveBean(DataSourceCheckpointRestoreLifecycleRegistry.class));
		});
	}

	@Test
	void whenPoolsAreNotAvailableNoLifeCycleIsInstrumented() {
		this.contextRunner
			.withClassLoader(cracEnabledClassLoader(
					new FilteredClassLoader("com.zaxxer.hikari", "oracle.ucp.jdbc", "oracle.jdbc")))
			.run((context) -> {
				final ClassLoader cl = context.getClassLoader();
				assertAll("When supported connection pools are not available, no life cycle is instrumented",
						() -> assertTrue(ClassUtils.isPresent("org.crac.Resource", cl)),
						() -> assertFalse(ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", cl)),
						() -> assertFalse(ClassUtils.isPresent("oracle.ucp.jdbc.PoolDataSourceImpl", cl)),
						() -> assertFalse(ClassUtils.isPresent("oracle.jdbc.OracleConnection", cl)),
						//
						() -> assertThat(context).doesNotHaveBean(DataSourceCheckpointRestoreLifecycleRegistry.class));
			});
	}

	@Test
	void whenCracAndHikariAreAvailableThenLifeCycleIsInstrumented() {
		this.contextRunner
			.withClassLoader(cracEnabledClassLoader(new FilteredClassLoader("oracle.ucp.jdbc", "oracle.jdbc")))
			.run((context) -> {
				final ClassLoader cl = context.getClassLoader();
				assertAll("When CRaC and Hikari are available, then life cycle is instrumented",
						() -> assertTrue(ClassUtils.isPresent("org.crac.Resource", cl)),
						() -> assertTrue(ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", cl)),
						() -> assertFalse(ClassUtils.isPresent("oracle.ucp.jdbc.PoolDataSourceImpl", cl)),
						() -> assertFalse(ClassUtils.isPresent("oracle.jdbc.OracleConnection", cl)),
						//
						() -> assertThat(context).hasSingleBean(HikariCheckpointRestoreLifecycleRegistry.class),
						() -> assertThat(context).doesNotHaveBean(OracleUcpCheckpointRestoreLifecycleRegistry.class));
			});
	}

	@Test
	void whenOracleUcpIsAvailableThenLifeCycleIsInstrumented() {
		this.contextRunner.withClassLoader(cracEnabledClassLoader(new FilteredClassLoader("com.zaxxer.hikari")))
			.run((context) -> {
				final ClassLoader cl = context.getClassLoader();
				assertAll("When CRaC and Oracle UCP are available, then life cycle is instrumented",
						() -> assertTrue(ClassUtils.isPresent("org.crac.Resource", cl)),
						() -> assertFalse(ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", cl)),
						() -> assertTrue(ClassUtils.isPresent("oracle.ucp.jdbc.PoolDataSourceImpl", cl)),
						() -> assertTrue(ClassUtils.isPresent("oracle.jdbc.OracleConnection", cl)),
						//
						() -> assertThat(context).doesNotHaveBean(HikariCheckpointRestoreLifecycleRegistry.class),
						() -> assertThat(context).hasSingleBean(OracleUcpCheckpointRestoreLifecycleRegistry.class));
			});
	}

	@Test
	void shouldBackoffWhenCustomHikariLifeCyclePresent() {
		this.contextRunner
			.withClassLoader(cracEnabledClassLoader(new FilteredClassLoader("oracle.ucp.jdbc", "oracle.jdbc")))
			.withBean("customHikariCheckpointRestoreLifecycle", HikariCheckpointRestoreLifecycleRegistry.class)
			.run((context) -> {
				final ClassLoader cl = context.getClassLoader();
				assertAll("When a custom Hikari life cycle is present, then autoconfiguration should back off",
						() -> assertTrue(ClassUtils.isPresent("org.crac.Resource", cl)),
						() -> assertTrue(ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", cl)),
						() -> assertFalse(ClassUtils.isPresent("oracle.ucp.jdbc.PoolDataSourceImpl", cl)),
						() -> assertFalse(ClassUtils.isPresent("oracle.jdbc.OracleConnection", cl)),
						//
						() -> assertThat(context).hasSingleBean(HikariCheckpointRestoreLifecycleRegistry.class),
						() -> assertThat(context).hasBean("customHikariCheckpointRestoreLifecycle"),
						() -> assertThat(context).doesNotHaveBean("hikariCheckpointRestoreLifecycle"),
						() -> assertThat(context).doesNotHaveBean(OracleUcpCheckpointRestoreLifecycleRegistry.class));
			});
	}

	@Test
	void shouldBackoffWhenCustomOracleUcpLifeCyclePresent() {
		this.contextRunner.withClassLoader(cracEnabledClassLoader(new FilteredClassLoader("com.zaxxer.hikari")))
			.withBean("customOracleUcpCheckpointRestoreLifecycle", OracleUcpCheckpointRestoreLifecycleRegistry.class)
			.run((context) -> {
				final ClassLoader cl = context.getClassLoader();
				assertAll("When a custom Oracle UCP life cycle is present, then autoconfiguration should back off",
						() -> assertTrue(ClassUtils.isPresent("org.crac.Resource", cl)),
						() -> assertFalse(ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource", cl)),
						() -> assertTrue(ClassUtils.isPresent("oracle.ucp.jdbc.PoolDataSourceImpl", cl)),
						() -> assertTrue(ClassUtils.isPresent("oracle.jdbc.OracleConnection", cl)),
						//
						() -> assertThat(context).doesNotHaveBean(HikariCheckpointRestoreLifecycleRegistry.class),
						() -> assertThat(context).hasSingleBean(OracleUcpCheckpointRestoreLifecycleRegistry.class),
						() -> assertThat(context).hasBean("customOracleUcpCheckpointRestoreLifecycle"),
						() -> assertThat(context).doesNotHaveBean("oracleUcpCheckpointRestoreLifecycle"));
			});
	}

	private static ClassLoader cracEnabledClassLoader(final ClassLoader parent) {
		return new ByteBuddy().subclass(Object.class)
			.name("org.crac.Resource")
			.make()
			.load(parent)
			.getLoaded()
			.getClassLoader();
	}

}
