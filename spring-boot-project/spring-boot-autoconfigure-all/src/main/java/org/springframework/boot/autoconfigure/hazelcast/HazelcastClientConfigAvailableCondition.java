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

	HazelcastClientConfigAvailableCondition() {
		super(HazelcastClientConfiguration.CONFIG_SYSTEM_PROPERTY, "file:./hazelcast-client.xml",
				"classpath:/hazelcast-client.xml", "file:./hazelcast-client.yaml", "classpath:/hazelcast-client.yaml",
				"file:./hazelcast-client.yml", "classpath:/hazelcast-client.yml");
	}

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

	static class HazelcastClientValidation {

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

		private static String existingConfigurationOutcome(Resource resource, boolean client) throws IOException {
			URL location = resource.getURL();
			return client ? "Hazelcast client configuration detected at '" + location + "'"
					: "Hazelcast server configuration detected  at '" + location + "'";
		}

	}

}
