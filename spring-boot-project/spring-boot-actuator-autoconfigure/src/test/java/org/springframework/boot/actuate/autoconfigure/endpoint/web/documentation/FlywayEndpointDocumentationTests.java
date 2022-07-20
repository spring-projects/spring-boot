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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.flyway.FlywayEndpoint;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link FlywayEndpoint}.
 *
 * @author Andy Wilkinson
 */
class FlywayEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void flyway() throws Exception {
		this.mockMvc.perform(get("/actuator/flyway")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("flyway",
						responseFields(fieldWithPath("contexts").description("Application contexts keyed by id"),
								fieldWithPath("contexts.*.flywayBeans.*.migrations").description(
										"Migrations performed by the Flyway instance, keyed by Flyway bean name."))
												.andWithPrefix("contexts.*.flywayBeans.*.migrations.[].",
														migrationFieldDescriptors())
												.and(parentIdField())));
	}

	private List<FieldDescriptor> migrationFieldDescriptors() {
		return Arrays.asList(fieldWithPath("checksum").description("Checksum of the migration, if any.").optional(),
				fieldWithPath("description").description("Description of the migration, if any.").optional(),
				fieldWithPath("executionTime").description("Execution time in milliseconds of an applied migration.")
						.optional(),
				fieldWithPath("installedBy").description("User that installed the applied migration, if any.")
						.optional(),
				fieldWithPath("installedOn")
						.description("Timestamp of when the applied migration was installed, if any.").optional(),
				fieldWithPath("installedRank")
						.description("Rank of the applied migration, if any. Later migrations have higher ranks.")
						.optional(),
				fieldWithPath("script").description("Name of the script used to execute the migration, if any.")
						.optional(),
				fieldWithPath("state")
						.description("State of the migration. (" + describeEnumValues(MigrationState.class) + ")"),
				fieldWithPath("type").description("Type of the migration."), fieldWithPath("version")
						.description("Version of the database after applying the migration, if any.").optional());
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	@ImportAutoConfiguration(FlywayAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true)
					.setType(EmbeddedDatabaseConnection.get(getClass().getClassLoader()).getType()).build();
		}

		@Bean
		FlywayEndpoint endpoint(ApplicationContext context) {
			return new FlywayEndpoint(context);
		}

	}

}
