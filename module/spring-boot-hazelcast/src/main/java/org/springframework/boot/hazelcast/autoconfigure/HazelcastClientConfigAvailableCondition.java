/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.hazelcast.client.config.ClientConfigRecognizer;
import com.hazelcast.config.ConfigStream;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

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

		static @Nullable ConditionOutcome clientConfigOutcome(ConditionContext context, String propertyName,
				Builder builder) {
			String resourcePath = context.getEnvironment().getProperty(propertyName);
			Assert.state(resourcePath != null, "'resourcePath' must not be null");
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
					: "Hazelcast server configuration detected at '" + location + "'";
		}

	}

}
