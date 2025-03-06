/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;
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
