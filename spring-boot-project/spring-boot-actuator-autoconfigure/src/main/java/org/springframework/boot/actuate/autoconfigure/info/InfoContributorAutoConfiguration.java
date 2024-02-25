/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.boot.actuate.info.JavaInfoContributor;
import org.springframework.boot.actuate.info.OsInfoContributor;
import org.springframework.boot.actuate.info.ProcessInfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for standard
 * {@link InfoContributor}s.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @author Jonatan Ivanov
 * @since 2.0.0
 */
@AutoConfiguration(after = ProjectInfoAutoConfiguration.class)
@EnableConfigurationProperties(InfoContributorProperties.class)
public class InfoContributorAutoConfiguration {

	/**
	 * The default order for the core {@link InfoContributor} beans.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	/**
	 * Creates an instance of {@link EnvironmentInfoContributor} if the "env" info
	 * contributor is enabled. The order of the info contributor is set to the default
	 * order.
	 * @param environment the {@link ConfigurableEnvironment} to be used by the info
	 * contributor
	 * @return an instance of {@link EnvironmentInfoContributor} if the "env" info
	 * contributor is enabled, otherwise returns null
	 */
	@Bean
	@ConditionalOnEnabledInfoContributor(value = "env", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	public EnvironmentInfoContributor envInfoContributor(ConfigurableEnvironment environment) {
		return new EnvironmentInfoContributor(environment);
	}

	/**
	 * Creates a GitInfoContributor bean if the "git" info contributor is enabled and
	 * there is a single candidate for GitProperties. If there is no existing bean of type
	 * GitInfoContributor, it creates a new one with the provided GitProperties and the
	 * mode specified in the InfoContributorProperties. The order of the
	 * GitInfoContributor is set to the default order.
	 * @param gitProperties The GitProperties bean used to configure the
	 * GitInfoContributor.
	 * @param infoContributorProperties The InfoContributorProperties bean used to
	 * configure the GitInfoContributor's mode.
	 * @return The GitInfoContributor bean.
	 */
	@Bean
	@ConditionalOnEnabledInfoContributor("git")
	@ConditionalOnSingleCandidate(GitProperties.class)
	@ConditionalOnMissingBean
	@Order(DEFAULT_ORDER)
	public GitInfoContributor gitInfoContributor(GitProperties gitProperties,
			InfoContributorProperties infoContributorProperties) {
		return new GitInfoContributor(gitProperties, infoContributorProperties.getGit().getMode());
	}

	/**
	 * Creates an instance of {@link InfoContributor} for providing build information.
	 * This method is conditionally enabled based on the presence of the "build" info
	 * contributor and a single candidate of type {@link BuildProperties}. The order of
	 * this info contributor is set to the default order.
	 * @param buildProperties the {@link BuildProperties} instance used to retrieve build
	 * information
	 * @return an instance of {@link InfoContributor} for providing build information
	 */
	@Bean
	@ConditionalOnEnabledInfoContributor("build")
	@ConditionalOnSingleCandidate(BuildProperties.class)
	@Order(DEFAULT_ORDER)
	public InfoContributor buildInfoContributor(BuildProperties buildProperties) {
		return new BuildInfoContributor(buildProperties);
	}

	/**
	 * Creates a new instance of {@link JavaInfoContributor}.
	 *
	 * This method is annotated with {@link Bean} to indicate that it is a bean definition
	 * method.
	 *
	 * It is conditionally enabled based on the value of the "java" property in the
	 * application's configuration. If the property is not present or is set to false, the
	 * {@link JavaInfoContributor} bean will not be created.
	 *
	 * The {@link ConditionalOnEnabledInfoContributor} annotation is used to conditionally
	 * enable the bean based on the value of the "java" property. The fallback attribute
	 * is set to {@link InfoContributorFallback.DISABLE}, which means that if the "java"
	 * property is not present or is set to false, the {@link JavaInfoContributor} bean
	 * will be disabled.
	 *
	 * The {@link Order} annotation is used to specify the order in which the
	 * {@link JavaInfoContributor} bean should be executed relative to other beans. The
	 * {@link Order#DEFAULT_ORDER} value is used, which means that the bean will have the
	 * default order.
	 * @return a new instance of {@link JavaInfoContributor}
	 */
	@Bean
	@ConditionalOnEnabledInfoContributor(value = "java", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	public JavaInfoContributor javaInfoContributor() {
		return new JavaInfoContributor();
	}

	/**
	 * Creates an instance of {@link OsInfoContributor} if the "os" info contributor is
	 * enabled. The order of the info contributor is set to the default order. If the "os"
	 * info contributor is not enabled, it falls back to the
	 * {@link InfoContributorFallback#DISABLE} fallback.
	 * @return the {@link OsInfoContributor} instance
	 */
	@Bean
	@ConditionalOnEnabledInfoContributor(value = "os", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	public OsInfoContributor osInfoContributor() {
		return new OsInfoContributor();
	}

	/**
	 * Creates a new instance of {@link ProcessInfoContributor} and registers it as an
	 * info contributor. This method is conditionally enabled based on the value of the
	 * "process" property in the application's configuration. If the property is not
	 * present or set to false, the info contributor will be disabled. The order of the
	 * info contributor is set to the default order.
	 * @return the created {@link ProcessInfoContributor} instance
	 */
	@Bean
	@ConditionalOnEnabledInfoContributor(value = "process", fallback = InfoContributorFallback.DISABLE)
	@Order(DEFAULT_ORDER)
	public ProcessInfoContributor processInfoContributor() {
		return new ProcessInfoContributor();
	}

}
