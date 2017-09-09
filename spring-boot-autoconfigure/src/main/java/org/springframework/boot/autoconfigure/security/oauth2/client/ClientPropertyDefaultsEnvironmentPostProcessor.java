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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * An {@link EnvironmentPostProcessor} that registers a <code>PropertySource</code> against the
 * <code>Environment</code>, which contains a default set of client properties.
 *
 * @author Joe Grandja
 * @since 2.0.0
 */
public class ClientPropertyDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	public static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE;

	static final String CLIENT_DEFAULTS_RESOURCE = "META-INF/spring-security-oauth2-client-defaults.properties";

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private int order = DEFAULT_ORDER;

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (this.isClientsConfigured(environment)) {
			try {
				environment.getPropertySources().addLast(
						new ResourcePropertySource(this.resourceLoader.getResource(CLIENT_DEFAULTS_RESOURCE)));
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to load class path resource: " + CLIENT_DEFAULTS_RESOURCE, ex);
			}
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	private boolean isClientsConfigured(ConfigurableEnvironment environment) {
		return !OAuth2ClientAutoConfiguration.getClientKeys(environment).isEmpty();
	}
}
