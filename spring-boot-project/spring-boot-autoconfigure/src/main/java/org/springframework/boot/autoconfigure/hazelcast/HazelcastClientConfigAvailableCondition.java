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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.hazelcast.client.config.ClientConfigRecognizer;
import com.hazelcast.config.ConfigStream;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link HazelcastConfigResourceCondition} that checks if the
 * {@code spring.hazelcast.config} configuration key is defined.
 *
 * @author Stephane Nicoll
 */
class HazelcastClientConfigAvailableCondition extends HazelcastConfigResourceCondition {

	/**
	 * Constructs a new HazelcastClientConfigAvailableCondition with the default
	 * configuration file locations. The configuration file locations are checked in the
	 * following order: 1. System property "hazelcast.client.config" with value
	 * "file:./hazelcast-client.xml" 2. Classpath resource "hazelcast-client.xml" 3.
	 * System property "hazelcast.client.config" with value "file:./hazelcast-client.yaml"
	 * 4. Classpath resource "hazelcast-client.yaml" 5. System property
	 * "hazelcast.client.config" with value "file:./hazelcast-client.yml" 6. Classpath
	 * resource "hazelcast-client.yml"
	 */
	HazelcastClientConfigAvailableCondition() {
		super(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY, "file:./hazelcast-client.xml",
				"classpath:/hazelcast-client.xml", "file:./hazelcast-client.yaml", "classpath:/hazelcast-client.yaml",
				"file:./hazelcast-client.yml", "classpath:/hazelcast-client.yml");
	}

	/**
	 * Determines the outcome of the condition for the availability of the Hazelcast
	 * client configuration.
	 * @param context the condition context
	 * @param metadata the annotated type metadata
	 * @return the condition outcome
	 */
	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		if (context.getEnvironment().containsProperty(HAZELCAST_CONFIG_PROPERTY)) {
			ConditionOutcome configValidationOutcome = HazelcastClientValidation.clientConfigOutcome(context,
					HAZELCAST_CONFIG_PROPERTY, startConditionMessage());
			return (configValidationOutcome != null) ? configValidationOutcome : ConditionOutcome
				.match(startConditionMessage().foundExactly("property " + HAZELCAST_CONFIG_PROPERTY));
		}
		return getResourceOutcome(context, metadata);
	}

	/**
	 * HazelcastClientValidation class.
	 */
	static class HazelcastClientValidation {

		/**
		 * Determines the outcome of the client configuration based on the provided
		 * condition context, property name, and builder.
		 * @param context the condition context
		 * @param propertyName the name of the property containing the resource path
		 * @param builder the builder used to construct the condition outcome
		 * @return the condition outcome indicating whether the client configuration
		 * exists and if it is recognized
		 */
		static ConditionOutcome clientConfigOutcome(ConditionContext context, String propertyName, Builder builder) {
			String resourcePath = context.getEnvironment().getProperty(propertyName);
			Resource resource = context.getResourceLoader().getResource(resourcePath);
			if (!resource.exists()) {
				return ConditionOutcome.noMatch(builder.because("Hazelcast configuration does not exist"));
			}
			try (InputStream in = resource.getInputStream()) {
				boolean clientConfig = new ClientConfigRecognizer().isRecognized(new ConfigStream(in));
				return new ConditionOutcome(clientConfig, existingConfigurationOutcome(resource, clientConfig));
			}
			catch (Throwable ex) {
				return null;
			}
		}

		/**
		 * Returns the outcome of an existing configuration based on the given resource
		 * and client flag.
		 * @param resource the resource containing the configuration
		 * @param client flag indicating whether the configuration is for a client or
		 * server
		 * @return the outcome message indicating the type of configuration detected
		 * @throws IOException if an I/O error occurs while retrieving the resource URL
		 */
		private static String existingConfigurationOutcome(Resource resource, boolean client) throws IOException {
			URL location = resource.getURL();
			return client ? "Hazelcast client configuration detected at '" + location + "'"
					: "Hazelcast server configuration detected  at '" + location + "'";
		}

	}

}
