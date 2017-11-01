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

package org.springframework.boot.autoconfigure.info;

import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProjectInfoAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class ProjectInfoAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void gitPropertiesUnavailableIfResourceNotAvailable() {
		load();
		Map<String, GitProperties> beans = this.context
				.getBeansOfType(GitProperties.class);
		assertThat(beans).hasSize(0);
	}

	@Test
	public void gitPropertiesWithNoData() {
		load("spring.info.git.location=classpath:/org/springframework/boot/autoconfigure/info/git-no-data.properties");
		GitProperties gitProperties = this.context.getBean(GitProperties.class);
		assertThat(gitProperties.getBranch()).isNull();
	}

	@Test
	public void gitPropertiesFallbackWithGitPropertiesBean() {
		load(CustomInfoPropertiesConfiguration.class,
				"spring.info.git.location=classpath:/org/springframework/boot/autoconfigure/info/git.properties");
		GitProperties gitProperties = this.context.getBean(GitProperties.class);
		assertThat(gitProperties).isSameAs(this.context.getBean("customGitProperties"));
	}

	@Test
	public void buildPropertiesDefaultLocation() {
		load();
		BuildProperties buildProperties = this.context.getBean(BuildProperties.class);
		assertThat(buildProperties.getGroup()).isEqualTo("com.example");
		assertThat(buildProperties.getArtifact()).isEqualTo("demo");
		assertThat(buildProperties.getName()).isEqualTo("Demo Project");
		assertThat(buildProperties.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
		assertThat(buildProperties.getTime().getTime()).isEqualTo(1457100965000L);
	}

	@Test
	public void buildPropertiesCustomLocation() {
		load("spring.info.build.location=classpath:/org/springframework/boot/autoconfigure/info/build-info.properties");
		BuildProperties buildProperties = this.context.getBean(BuildProperties.class);
		assertThat(buildProperties.getGroup()).isEqualTo("com.example.acme");
		assertThat(buildProperties.getArtifact()).isEqualTo("acme");
		assertThat(buildProperties.getName()).isEqualTo("acme");
		assertThat(buildProperties.getVersion()).isEqualTo("1.0.1-SNAPSHOT");
		assertThat(buildProperties.getTime().getTime()).isEqualTo(1457088120000L);
	}

	@Test
	public void buildPropertiesCustomInvalidLocation() {
		load("spring.info.build.location=classpath:/org/acme/no-build-info.properties");
		Map<String, BuildProperties> beans = this.context
				.getBeansOfType(BuildProperties.class);
		assertThat(beans).hasSize(0);
	}

	@Test
	public void buildPropertiesFallbackWithBuildInfoBean() {
		load(CustomInfoPropertiesConfiguration.class);
		BuildProperties buildProperties = this.context.getBean(BuildProperties.class);
		assertThat(buildProperties)
				.isSameAs(this.context.getBean("customBuildProperties"));
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (config != null) {
			context.register(config);
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				ProjectInfoAutoConfiguration.class);
		TestPropertyValues.of(environment).applyTo(context);
		context.refresh();
		this.context = context;
	}

	@Configuration
	static class CustomInfoPropertiesConfiguration {

		@Bean
		public GitProperties customGitProperties() {
			return new GitProperties(new Properties());
		}

		@Bean
		public BuildProperties customBuildProperties() {
			return new BuildProperties(new Properties());
		}

	}

}
