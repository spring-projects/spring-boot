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

package org.springframework.boot.autoconfigure.integration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.context.IntegrationProperties;

/**
 * The {@link EnvironmentPostProcessor} for Spring Integration.
 *
 * @author Artem Bilan
 * @since 2.5
 */
public class IntegrationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		registerIntegrationPropertiesFileSource(environment);
	}

	private static void registerIntegrationPropertiesFileSource(ConfigurableEnvironment environment) {
		Resource integrationPropertiesResource = new ClassPathResource("META-INF/spring.integration.properties");
		PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();
		try {
			OriginTrackedMapPropertySource propertyFileSource = (OriginTrackedMapPropertySource) loader
					.load("integration-properties-file", integrationPropertiesResource).get(0);

			environment.getPropertySources().addLast(new IntegrationPropertySource(propertyFileSource));
		}
		catch (FileNotFoundException ex) {
			// Ignore when no META-INF/spring.integration.properties file in classpath
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Failed to load integration properties from " + integrationPropertiesResource, ex);
		}
	}

	private static final class IntegrationPropertySource extends PropertySource<Map<String, Object>>
			implements OriginLookup<String> {

		private static final String PREFIX = "spring.integration.";

		private static final Map<String, String> KEYS_MAPPING = new HashMap<>();

		static {
			KEYS_MAPPING.put(PREFIX + "channels.auto-create", IntegrationProperties.CHANNELS_AUTOCREATE);
			KEYS_MAPPING.put(PREFIX + "channels.max-unicast-subscribers",
					IntegrationProperties.CHANNELS_MAX_UNICAST_SUBSCRIBERS);
			KEYS_MAPPING.put(PREFIX + "channels.max-broadcast-subscribers",
					IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS);
			KEYS_MAPPING.put(PREFIX + "channels.error-require-subscribers",
					IntegrationProperties.ERROR_CHANNEL_REQUIRE_SUBSCRIBERS);
			KEYS_MAPPING.put(PREFIX + "channels.error-ignore-failures",
					IntegrationProperties.ERROR_CHANNEL_IGNORE_FAILURES);
			KEYS_MAPPING.put(PREFIX + "endpoints.throw-exception-on-late-reply",
					IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY);
			KEYS_MAPPING.put(PREFIX + "endpoints.read-only-headers", IntegrationProperties.READ_ONLY_HEADERS);
			KEYS_MAPPING.put(PREFIX + "endpoints.no-auto-startup", IntegrationProperties.ENDPOINTS_NO_AUTO_STARTUP);
		}

		private final OriginTrackedMapPropertySource origin;

		IntegrationPropertySource(OriginTrackedMapPropertySource origin) {
			super("original-integration-properties", origin.getSource());
			this.origin = origin;
		}

		@Override
		public Object getProperty(String name) {
			return this.origin.getProperty(KEYS_MAPPING.get(name));
		}

		@Override
		public Origin getOrigin(String key) {
			return this.origin.getOrigin(KEYS_MAPPING.get(name));
		}

	}

}
