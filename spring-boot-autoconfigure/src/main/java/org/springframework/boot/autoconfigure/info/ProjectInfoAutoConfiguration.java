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

import java.util.Properties;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.DefaultResourceLoader;
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

	@Configuration
	@Conditional(GitResourceAvailableCondition.class)
	protected static class GitInfoAutoConfiguration {

		@ConditionalOnMissingBean
		@Bean
		public GitInfo gitInfo(ProjectInfoProperties properties) throws Exception {
			PropertiesConfigurationFactory<GitInfo> factory = new PropertiesConfigurationFactory<GitInfo>(
					new GitInfo());
			factory.setTargetName("git");
			Properties gitInfoProperties = PropertiesLoaderUtils
					.loadProperties(properties.getGit().getLocation());
			factory.setProperties(gitInfoProperties);
			return factory.getObject();
		}

	}


	static class GitResourceAvailableCondition extends SpringBootCondition {

		private final ResourceLoader defaultResourceLoader = new DefaultResourceLoader();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ResourceLoader loader = context.getResourceLoader() == null
					? this.defaultResourceLoader : context.getResourceLoader();
			PropertyResolver propertyResolver = context.getEnvironment();
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(propertyResolver, "spring.info.git.");
			String location = resolver.getProperty("location");
			if (location == null) {
				resolver = new RelaxedPropertyResolver(propertyResolver, "spring.git.");
				location = resolver.getProperty("properties");
				if (location == null) {
					location = "classpath:git.properties";
				}
			}
			boolean match = loader.getResource(location).exists();
			return new ConditionOutcome(match, "Git info " + (match ? "found" : "not found") + " at " + location);
		}
	}

}
