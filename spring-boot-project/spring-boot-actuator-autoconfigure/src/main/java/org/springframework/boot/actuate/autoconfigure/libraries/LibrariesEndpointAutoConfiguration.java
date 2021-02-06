/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.libraries;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.libraries.LibrariesProperties.Bundled;
import org.springframework.boot.actuate.libraries.BasicLibrariesContributor;
import org.springframework.boot.actuate.libraries.LibrariesContributor;
import org.springframework.boot.actuate.libraries.LibrariesEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link LibrariesEndpoint}.
 *
 * @author Phil Clay
 * @since 2.5.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LibrariesProperties.class)
@ConditionalOnAvailableEndpoint(endpoint = LibrariesEndpoint.class)
public class LibrariesEndpointAutoConfiguration {

	/**
	 * The default order for the core {@link LibrariesContributor} beans.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	/**
	 * A {@link LibrariesContributor} that contributes libraries that were bundled in the
	 * spring boot application at build time, typically by the spring boot maven or gradle
	 * plugins.
	 * @param librariesProperties libraries configuration properties
	 * @return a {@link LibrariesContributor} that contributes libraries that were bundled
	 * in the spring boot application at build time, typically by the spring boot maven or
	 * gradle plugins.
	 * @throws IOException if an exception occurs reading the build libraries yaml file
	 */
	@Bean
	@ConditionalOnEnabledLibrariesContributor("bundled")
	@ConditionalOnResource(resources = "${" + LibrariesProperties.CONFIGURATION_PROPERTIES_PREFIX
			+ ".bundled.location:classpath:" + Bundled.DEFAULT_CLASSPATH_LOCATION + "}")
	@Order(DEFAULT_ORDER)
	public LibrariesContributor bundledLibrariesContributor(LibrariesProperties librariesProperties)
			throws IOException {
		return BasicLibrariesContributor.fromYamlResource("bundled", librariesProperties.getBundled().getLocation());
	}

	@Bean
	@ConditionalOnMissingBean
	public LibrariesEndpoint librariesEndpoint(ObjectProvider<LibrariesContributor> librariesContributors) {
		return new LibrariesEndpoint(librariesContributors.orderedStream().collect(Collectors.toList()));
	}

}
