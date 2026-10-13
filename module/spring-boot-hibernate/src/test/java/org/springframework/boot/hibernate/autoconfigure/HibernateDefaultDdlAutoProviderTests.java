/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure;

import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateDefaultDdlAutoProvider}.
 *
 * @author Stephane Nicoll
 */
class HibernateDefaultDdlAutoProviderTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
		.withPropertyValues("spring.sql.init.mode:never");

	@Test
	void defaultDDlAutoForEmbedded() {
		this.contextRunner.run((context) -> {
			HibernateDefaultDdlAutoProvider ddlAutoProvider = new HibernateDefaultDdlAutoProvider(
					Collections.emptyList());
			assertThat(ddlAutoProvider.getDefaultDdlAuto(context.getBean(DataSource.class))).isEqualTo("create-drop");
		});
	}

	@Test
	void defaultDDlAutoForEmbeddedWithPositiveContributor() {
		this.contextRunner.run((context) -> {
			DataSource dataSource = context.getBean(DataSource.class);
			SchemaManagementProvider provider = mock(SchemaManagementProvider.class);
			given(provider.getSchemaManagement(dataSource)).willReturn(SchemaManagement.MANAGED);
			HibernateDefaultDdlAutoProvider ddlAutoProvider = new HibernateDefaultDdlAutoProvider(
					Collections.singletonList(provider));
			assertThat(ddlAutoProvider.getDefaultDdlAuto(dataSource)).isEqualTo("none");
		});
	}

	@Test
	void defaultDDlAutoForEmbeddedWithNegativeContributor() {
		this.contextRunner.run((context) -> {
			DataSource dataSource = context.getBean(DataSource.class);
			SchemaManagementProvider provider = mock(SchemaManagementProvider.class);
			given(provider.getSchemaManagement(dataSource)).willReturn(SchemaManagement.UNMANAGED);
			HibernateDefaultDdlAutoProvider ddlAutoProvider = new HibernateDefaultDdlAutoProvider(
					Collections.singletonList(provider));
			assertThat(ddlAutoProvider.getDefaultDdlAuto(dataSource)).isEqualTo("create-drop");
		});
	}

}
