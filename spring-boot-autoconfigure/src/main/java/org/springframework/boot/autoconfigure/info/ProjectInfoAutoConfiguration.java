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

package org.springframework.boot.autoconfigure.info;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for various project information.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@EnableConfigurationProperties(ProjectInfoProperties.class)
public class ProjectInfoAutoConfiguration {

	private final ProjectInfoProperties properties;

	public ProjectInfoAutoConfiguration(ProjectInfoProperties properties) {
		this.properties = properties;
	}

	@Conditional(GitResourceAvailableCondition.class)
	@ConditionalOnMissingBean
	@Bean
	public GitProperties gitProperties() throws Exception {
		return new GitProperties(loadFrom(this.properties.getGit().getLocation(), "git"));
	}

	@ConditionalOnResource(resources = "${spring.info.build.location:classpath:META-INF/build-info.properties}")
	@ConditionalOnMissingBean
	@Bean
	public BuildProperties buildProperties() throws Exception {
		return new BuildProperties(
				loadFrom(this.properties.getBuild().getLocation(), "build"));
	}

	protected Properties loadFrom(Resource location, String prefix) throws IOException {
		String p = prefix.endsWith(".") ? prefix : prefix + ".";
		Properties source = PropertiesLoaderUtils.loadProperties(location);
		Properties target = new Properties();
		for (String key : source.stringPropertyNames()) {
			if (key.startsWith(p)) {
				target.put(key.substring(p.length()), source.get(key));
			}
		}
		return target;
	}

	static class GitResourceAvailableCondition extends SpringBootCondition {

		private final ResourceLoader defaultResourceLoader = new DefaultResourceLoader();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ResourceLoader loader = context.getResourceLoader();
			if (loader == null) {
				loader = this.defaultResourceLoader;
			}
			PropertyResolver propertyResolver = context.getEnvironment();
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
					propertyResolver, "spring.info.git.");
			String location = resolver.getProperty("location");
			if (location == null) {
				resolver = new RelaxedPropertyResolver(propertyResolver, "spring.git.");
				location = resolver.getProperty("properties");
				if (location == null) {
					location = "classpath:git.properties";
				}
			}
			boolean match = loader.getResource(location).exists();
			return new ConditionOutcome(match,
					"Git info " + (match ? "found" : "not found") + " at " + location);
		}

	}

}
