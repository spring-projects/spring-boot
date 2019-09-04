/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.boot.actuate.info.BuildInfoContributor;
import org.springframework.boot.actuate.info.EnvironmentInfoContributor;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for standard
 * {@link InfoContributor}s.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(ProjectInfoAutoConfiguration.class)
@EnableConfigurationProperties(InfoContributorProperties.class)
public class InfoContributorAutoConfiguration {

	/**
	 * The default order for the core {@link InfoContributor} beans.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	@Bean
	@ConditionalOnEnabledInfoContributor("env")
	@Order(DEFAULT_ORDER)
	public EnvironmentInfoContributor envInfoContributor(ConfigurableEnvironment environment) {
		return new EnvironmentInfoContributor(environment);
	}

	@Bean
	@ConditionalOnEnabledInfoContributor("git")
	@ConditionalOnSingleCandidate(GitProperties.class)
	@ConditionalOnMissingBean
	@Order(DEFAULT_ORDER)
	public GitInfoContributor gitInfoContributor(GitProperties gitProperties,
			InfoContributorProperties infoContributorProperties) {
		return new GitInfoContributor(gitProperties, infoContributorProperties.getGit().getMode());
	}

	@Bean
	@ConditionalOnEnabledInfoContributor("build")
	@ConditionalOnSingleCandidate(BuildProperties.class)
	@Order(DEFAULT_ORDER)
	public InfoContributor buildInfoContributor(BuildProperties buildProperties) {
		return new BuildInfoContributor(buildProperties);
	}

}
