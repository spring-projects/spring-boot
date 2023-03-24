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

package org.springframework.boot.actuate.autoconfigure.info;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.info.BuildInfoContributor;
import org.springframework.boot.actuate.info.EnvironmentInfoContributor;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.JavaInfoContributor;
import org.springframework.boot.actuate.info.OsInfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.info.JavaInfo;
import org.springframework.boot.info.OsInfo;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfoContributorAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Jonatan Ivanov
 */
class InfoContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(InfoContributorAutoConfiguration.class));

	@Test
	void envContributor() {
		this.contextRunner.withPropertyValues("management.info.env.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(EnvironmentInfoContributor.class));
	}

	@Test
	void defaultInfoContributorsEnabled() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(InfoContributor.class));
	}

	@Test
	void defaultInfoContributorsEnabledWithPrerequisitesInPlace() {
		this.contextRunner.withUserConfiguration(GitPropertiesConfiguration.class, BuildPropertiesConfiguration.class)
			.run((context) -> assertThat(context.getBeansOfType(InfoContributor.class)).hasSize(2)
				.satisfies((contributors) -> assertThat(contributors.values())
					.hasOnlyElementsOfTypes(BuildInfoContributor.class, GitInfoContributor.class)));
	}

	@Test
	void defaultInfoContributorsDisabledWithPrerequisitesInPlace() {
		this.contextRunner.withUserConfiguration(GitPropertiesConfiguration.class, BuildPropertiesConfiguration.class)
			.withPropertyValues("management.info.defaults.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(InfoContributor.class));
	}

	@Test
	void defaultInfoContributorsDisabledWithCustomOne() {
		this.contextRunner.withUserConfiguration(CustomInfoContributorConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(InfoContributor.class);
			assertThat(context.getBean(InfoContributor.class)).isSameAs(context.getBean("customInfoContributor"));
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	void gitPropertiesDefaultMode() {
		this.contextRunner.withUserConfiguration(GitPropertiesConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(GitInfoContributor.class);
			Map<String, Object> content = invokeContributor(context.getBean(GitInfoContributor.class));
			Object git = content.get("git");
			assertThat(git).isInstanceOf(Map.class);
			Map<String, Object> gitInfo = (Map<String, Object>) git;
			assertThat(gitInfo).containsOnlyKeys("branch", "commit");
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	void gitPropertiesFullMode() {
		this.contextRunner.withPropertyValues("management.info.git.mode=full")
			.withUserConfiguration(GitPropertiesConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(GitInfoContributor.class);
				Map<String, Object> content = invokeContributor(context.getBean(GitInfoContributor.class));
				Object git = content.get("git");
				assertThat(git).isInstanceOf(Map.class);
				Map<String, Object> gitInfo = (Map<String, Object>) git;
				assertThat(gitInfo).containsOnlyKeys("branch", "commit", "foo");
				assertThat(gitInfo).containsEntry("foo", "bar");
			});
	}

	@Test
	void customGitInfoContributor() {
		this.contextRunner.withUserConfiguration(CustomGitInfoContributorConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(GitInfoContributor.class);
			assertThat(context.getBean(GitInfoContributor.class)).isSameAs(context.getBean("customGitInfoContributor"));
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	void buildProperties() {
		this.contextRunner.withUserConfiguration(BuildPropertiesConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(BuildInfoContributor.class);
			Map<String, Object> content = invokeContributor(context.getBean(BuildInfoContributor.class));
			Object build = content.get("build");
			assertThat(build).isInstanceOf(Map.class);
			Map<String, Object> buildInfo = (Map<String, Object>) build;
			assertThat(buildInfo).containsOnlyKeys("group", "artifact", "foo");
			assertThat(buildInfo).containsEntry("foo", "bar");
		});
	}

	@Test
	void customBuildInfoContributor() {
		this.contextRunner.withUserConfiguration(CustomBuildInfoContributorConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(BuildInfoContributor.class);
			assertThat(context.getBean(BuildInfoContributor.class))
				.isSameAs(context.getBean("customBuildInfoContributor"));
		});
	}

	@Test
	void javaInfoContributor() {
		this.contextRunner.withPropertyValues("management.info.java.enabled=true").run((context) -> {
			assertThat(context).hasSingleBean(JavaInfoContributor.class);
			Map<String, Object> content = invokeContributor(context.getBean(JavaInfoContributor.class));
			assertThat(content).containsKey("java");
			assertThat(content.get("java")).isInstanceOf(JavaInfo.class);
		});
	}

	@Test
	void osInfoContributor() {
		this.contextRunner.withPropertyValues("management.info.os.enabled=true").run((context) -> {
			assertThat(context).hasSingleBean(OsInfoContributor.class);
			Map<String, Object> content = invokeContributor(context.getBean(OsInfoContributor.class));
			assertThat(content).containsKey("os");
			assertThat(content.get("os")).isInstanceOf(OsInfo.class);
		});
	}

	private Map<String, Object> invokeContributor(InfoContributor contributor) {
		Info.Builder builder = new Info.Builder();
		contributor.contribute(builder);
		return builder.build().getDetails();
	}

	@Configuration(proxyBeanMethods = false)
	static class GitPropertiesConfiguration {

		@Bean
		GitProperties gitProperties() {
			Properties properties = new Properties();
			properties.put("branch", "master");
			properties.put("commit.id", "abcdefg");
			properties.put("foo", "bar");
			return new GitProperties(properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BuildPropertiesConfiguration {

		@Bean
		BuildProperties buildProperties() {
			Properties properties = new Properties();
			properties.put("group", "com.example");
			properties.put("artifact", "demo");
			properties.put("foo", "bar");
			return new BuildProperties(properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomInfoContributorConfiguration {

		@Bean
		InfoContributor customInfoContributor() {
			return (builder) -> {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomGitInfoContributorConfiguration {

		@Bean
		GitInfoContributor customGitInfoContributor() {
			return new GitInfoContributor(new GitProperties(new Properties()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBuildInfoContributorConfiguration {

		@Bean
		BuildInfoContributor customBuildInfoContributor() {
			return new BuildInfoContributor(new BuildProperties(new Properties()));
		}

	}

}
