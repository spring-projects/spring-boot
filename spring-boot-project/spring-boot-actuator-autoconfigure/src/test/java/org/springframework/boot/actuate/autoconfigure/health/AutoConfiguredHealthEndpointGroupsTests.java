/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfiguredHealthEndpointGroups}.
 *
 * @author Phillip Webb
 * @author Leo Li
 */
class AutoConfiguredHealthEndpointGroupsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AutoConfiguredHealthEndpointGroupsTestConfiguration.class));

	@Test
	void getPrimaryGroupMatchesAllMembers() {
		this.contextRunner.run((context) -> {
			HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
			HealthEndpointGroup primary = groups.getPrimary();
			assertThat(primary.isMember("a")).isTrue();
			assertThat(primary.isMember("b")).isTrue();
			assertThat(primary.isMember("C")).isTrue();
		});
	}

	@Test
	void getNamesReturnsGroupNames() {
		this.contextRunner.withPropertyValues("management.endpoint.health.group.a.include=*",
				"management.endpoint.health.group.b.include=*").run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					assertThat(groups.getNames()).containsExactlyInAnyOrder("a", "b");
				});
	}

	@Test
	void getGroupWhenGroupExistsReturnsGroup() {
		this.contextRunner.withPropertyValues("management.endpoint.health.group.a.include=*").run((context) -> {
			HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
			HealthEndpointGroup group = groups.get("a");
			assertThat(group).isNotNull();
		});
	}

	@Test
	void getGroupWhenGroupDoesNotExistReturnsNull() {
		this.contextRunner.withPropertyValues("management.endpoint.health.group.a.include=*").run((context) -> {
			HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
			HealthEndpointGroup group = groups.get("b");
			assertThat(group).isNull();
		});
	}

	@Test
	void createWhenNoDefinedBeansAdaptsProperties() {
		this.contextRunner.withPropertyValues("management.endpoint.health.show-components=always",
				"management.endpoint.health.show-details=never", "management.endpoint.health.status.order=up,down",
				"management.endpoint.health.status.http-mapping.down=200").run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					assertThat(primary.showComponents(SecurityContext.NONE)).isTrue();
					assertThat(primary.showDetails(SecurityContext.NONE)).isFalse();
					assertThat(primary.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN))
							.isEqualTo(Status.UP);
					assertThat(primary.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(200);
				});
	}

	@Test
	void createWhenHasStatusAggregatorBeanReturnsInstanceWithAggregatorUsedForAllGroups() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorConfiguration.class)
				.withPropertyValues("management.endpoint.health.status.order=up,down",
						"management.endpoint.health.group.a.include=*")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					assertThat(primary.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UNKNOWN);
					assertThat(groupA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UNKNOWN);
				});
	}

	@Test
	void createWhenHasStatusAggregatorBeanAndGroupSpecificPropertyReturnsInstanceThatUsesBeanOnlyForUnconfiguredGroups() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorConfiguration.class)
				.withPropertyValues("management.endpoint.health.group.a.include=*",
						"management.endpoint.health.group.a.status.order=up,down",
						"management.endpoint.health.group.b.include=*")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UNKNOWN);
					assertThat(groupA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UP);
					assertThat(groupB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UNKNOWN);
				});
	}

	@Test
	void createWhenHasStatusAggregatorPropertyReturnsInstanceWithPropertyUsedForAllGroups() {
		this.contextRunner.withPropertyValues("management.endpoint.health.status.order=up,down",
				"management.endpoint.health.group.a.include=*").run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					assertThat(primary.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN))
							.isEqualTo(Status.UP);
					assertThat(groupA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN))
							.isEqualTo(Status.UP);
				});
	}

	@Test
	void createWhenHasStatusAggregatorPropertyAndGroupSpecificPropertyReturnsInstanceWithPropertyUsedForExpectedGroups() {
		this.contextRunner.withPropertyValues("management.endpoint.health.status.order=up,down",
				"management.endpoint.health.group.a.include=*",
				"management.endpoint.health.group.a.status.order=unknown,up,down",
				"management.endpoint.health.group.b.include=*").run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UP);
					assertThat(groupA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UNKNOWN);
					assertThat(groupB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UP);
				});
	}

	@Test
	void createWhenHasStatusAggregatorPropertyAndGroupQualifiedBeanReturnsInstanceWithBeanUsedForExpectedGroups() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorGroupAConfiguration.class)
				.withPropertyValues("management.endpoint.health.status.order=up,down",
						"management.endpoint.health.group.a.include=*",
						"management.endpoint.health.group.a.status.order=up,down",
						"management.endpoint.health.group.b.include=*")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UP);
					assertThat(groupA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UNKNOWN);
					assertThat(groupB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UP);
				});
	}

	@Test
	void createWhenHasGroupSpecificStatusAggregatorPropertyAndGroupQualifiedBeanReturnsInstanceWithBeanUsedForExpectedGroups() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorGroupAConfiguration.class)
				.withPropertyValues("management.endpoint.health.group.a.include=*",
						"management.endpoint.health.group.a.status.order=up,down",
						"management.endpoint.health.group.b.include=*",
						"management.endpoint.health.group.b.status.order=up,down")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.DOWN);
					assertThat(groupA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UNKNOWN);
					assertThat(groupB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
							.isEqualTo(Status.UP);
				});
	}

	@Test
	void createWhenHasHttpCodeStatusMapperBeanReturnsInstanceWithMapperUsedForAllGroups() {
		this.contextRunner.withUserConfiguration(CustomHttpCodeStatusMapperConfiguration.class)
				.withPropertyValues("management.endpoint.health.status.http-mapping.down=201",
						"management.endpoint.health.group.a.include=*")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					assertThat(primary.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(200);
					assertThat(groupA.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(200);
				});
	}

	@Test
	void createWhenHasHttpCodeStatusMapperBeanAndGroupSpecificPropertyReturnsInstanceThatUsesBeanOnlyForUnconfiguredGroups() {
		this.contextRunner.withUserConfiguration(CustomHttpCodeStatusMapperConfiguration.class)
				.withPropertyValues("management.endpoint.health.group.a.include=*",
						"management.endpoint.health.group.a.status.http-mapping.down=201",
						"management.endpoint.health.group.b.include=*")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(200);
					assertThat(groupA.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
					assertThat(groupB.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(200);
				});
	}

	@Test
	void createWhenHasHttpCodeStatusMapperPropertyReturnsInstanceWithPropertyUsedForAllGroups() {
		this.contextRunner.withPropertyValues("management.endpoint.health.status.http-mapping.down=201",
				"management.endpoint.health.group.a.include=*").run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					assertThat(primary.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
					assertThat(groupA.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
				});
	}

	@Test
	void createWhenHasHttpCodeStatusMapperPropertyAndGroupSpecificPropertyReturnsInstanceWithPropertyUsedForExpectedGroups() {
		this.contextRunner.withPropertyValues("management.endpoint.health.status.http-mapping.down=201",
				"management.endpoint.health.group.a.include=*",
				"management.endpoint.health.group.a.status.http-mapping.down=202",
				"management.endpoint.health.group.b.include=*").run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
					assertThat(groupA.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(202);
					assertThat(groupB.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
				});
	}

	@Test
	void createWhenHasHttpCodeStatusMapperPropertyAndGroupQualifiedBeanReturnsInstanceWithBeanUsedForExpectedGroups() {
		this.contextRunner.withUserConfiguration(CustomHttpCodeStatusMapperGroupAConfiguration.class)
				.withPropertyValues("management.endpoint.health.status.http-mapping.down=201",
						"management.endpoint.health.group.a.include=*",
						"management.endpoint.health.group.a.status.http-mapping.down=201",
						"management.endpoint.health.group.b.include=*")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
					assertThat(groupA.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(200);
					assertThat(groupB.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
				});
	}

	@Test
	void createWhenHasGroupSpecificHttpCodeStatusMapperPropertyAndGroupQualifiedBeanReturnsInstanceWithBeanUsedForExpectedGroups() {
		this.contextRunner.withUserConfiguration(CustomHttpCodeStatusMapperGroupAConfiguration.class)
				.withPropertyValues("management.endpoint.health.group.a.include=*",
						"management.endpoint.health.group.a.status.http-mapping.down=201",
						"management.endpoint.health.group.b.include=*",
						"management.endpoint.health.group.b.status.http-mapping.down=201")
				.run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup primary = groups.getPrimary();
					HealthEndpointGroup groupA = groups.get("a");
					HealthEndpointGroup groupB = groups.get("b");
					assertThat(primary.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(503);
					assertThat(groupA.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(200);
					assertThat(groupB.getHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(201);
				});
	}

	@Test
	void createWhenGroupWithNoShowDetailsOverrideInheritsShowDetails() {
		this.contextRunner.withPropertyValues("management.endpoint.health.show-details=always",
				"management.endpoint.health.group.a.include=*").run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					HealthEndpointGroup groupA = groups.get("a");
					assertThat(groupA.showDetails(SecurityContext.NONE)).isTrue();
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(HealthEndpointProperties.class)
	static class AutoConfiguredHealthEndpointGroupsTestConfiguration {

		@Bean
		AutoConfiguredHealthEndpointGroups healthEndpointGroups(ConfigurableApplicationContext applicationContext,
				HealthEndpointProperties properties) {
			return new AutoConfiguredHealthEndpointGroups(applicationContext, properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomStatusAggregatorConfiguration {

		@Bean
		@Primary
		StatusAggregator statusAggregator() {
			return new SimpleStatusAggregator(Status.UNKNOWN, Status.UP, Status.DOWN);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomStatusAggregatorGroupAConfiguration {

		@Bean
		@Qualifier("a")
		StatusAggregator statusAggregator() {
			return new SimpleStatusAggregator(Status.UNKNOWN, Status.UP, Status.DOWN);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHttpCodeStatusMapperConfiguration {

		@Bean
		@Primary
		HttpCodeStatusMapper httpCodeStatusMapper() {
			return new SimpleHttpCodeStatusMapper(Collections.singletonMap(Status.DOWN.getCode(), 200));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHttpCodeStatusMapperGroupAConfiguration {

		@Bean
		@Qualifier("a")
		HttpCodeStatusMapper httpCodeStatusMapper() {
			return new SimpleHttpCodeStatusMapper(Collections.singletonMap(Status.DOWN.getCode(), 200));
		}

	}

}
