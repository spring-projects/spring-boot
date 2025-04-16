package org.springframework.boot.actuate.configview;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.*;

@Endpoint(id = "config-view")
public class ConfigViewEndpoint {

	private final ConfigurableEnvironment environment;

	public ConfigViewEndpoint(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@ReadOperation
	public List<ConfigProperty> configProperties() {
		List<ConfigProperty> results = new ArrayList<>();

		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			if (propertySource instanceof EnumerablePropertySource<?> enumerable) {
				for (String propertyName : enumerable.getPropertyNames()) {
					Object value = enumerable.getProperty(propertyName);
					String displayValue = isSensitive(propertyName) ? "****" : String.valueOf(value);
					results.add(new ConfigProperty(propertyName, displayValue, propertySource.getName()));
				}
			}
		}
		return results;
	}

	private boolean isSensitive(String name) {
		String lower = name.toLowerCase();
		return lower.contains("password") || lower.contains("secret") || lower.contains("token") || lower.contains("key");
	}

	public record ConfigProperty(String key, String value, String source) {}
}
