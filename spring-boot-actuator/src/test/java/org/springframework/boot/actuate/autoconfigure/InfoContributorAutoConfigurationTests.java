/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.info.GitInfo;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfoContributorAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class InfoContributorAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void disableEnvContributor() {
		load("management.info.env.enabled:false");
		Map<String, InfoContributor> beans = this.context
				.getBeansOfType(InfoContributor.class);
		assertThat(beans).hasSize(0);
	}

	@Test
	public void defaultInfoContributorsDisabled() {
		load("management.info.defaults.enabled:false");
		Map<String, InfoContributor> beans = this.context
				.getBeansOfType(InfoContributor.class);
		assertThat(beans).hasSize(0);
	}

	@Test
	public void defaultInfoContributorsDisabledWithCustomOne() {
		load(CustomInfoProviderConfiguration.class,
				"management.info.defaults.enabled:false");
		Map<String, InfoContributor> beans = this.context
				.getBeansOfType(InfoContributor.class);
		assertThat(beans).hasSize(1);
		assertThat(this.context.getBean("customInfoContributor"))
				.isSameAs(beans.values().iterator().next());
	}

	@Test
	public void gitInfoAvailable() {
		load(GitInfoConfiguration.class);
		Map<String, InfoContributor> beans = this.context
				.getBeansOfType(InfoContributor.class);
		assertThat(beans).containsKeys("gitInfoContributor");
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (config != null) {
			context.register(config);
		}
		context.register(InfoContributorAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(context, environment);
		context.refresh();
		this.context = context;
	}

	@Configuration
	static class GitInfoConfiguration {

		@Bean
		public GitInfo gitInfo() {
			GitInfo gitInfo = new GitInfo();
			gitInfo.setBranch("master");
			gitInfo.getCommit().setId("abcdefg");
			return gitInfo;
		}

	}

	@Configuration
	static class CustomInfoProviderConfiguration {

		@Bean
		public InfoContributor customInfoContributor() {
			return new InfoContributor() {
				@Override
				public void contribute(Info.Builder builder) {
				}
			};
		}

	}

}
