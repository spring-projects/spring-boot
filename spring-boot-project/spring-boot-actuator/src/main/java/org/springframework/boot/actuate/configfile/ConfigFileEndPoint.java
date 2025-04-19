package org.springframework.boot.actuate.configfile;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;

@Endpoint(id = "configfile")
public class ConfigFileEndpoint {

	private final Environment environment;
	private final List<String> sensitiveKeys;

	public ConfigFileEndpoint(Environment environment, List<String> sensitiveKeys) {
		this.environment = environment;
		this.sensitiveKeys = sensitiveKeys;
	}

	@ReadOperation
	public Map<String, Object> config() {
		Map<String, Object> properties = new LinkedHashMap<>();

		if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
			for (PropertySource<?> source : configurableEnvironment.getPropertySources()) {
				if (source.getName().contains("applicationConfig")) {
					if (source.getSource() instanceof Map<?, ?> map) {
						for (Map.Entry<?, ?> entry : map.entrySet()) {
							String key = String.valueOf(entry.getKey());
							Object value = sensitiveKeys.stream().anyMatch(key::contains) ? "****" : entry.getValue();
							properties.put(key, value);
						}
					}
				}
			}
		}

		return properties.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(e1, e2) -> e1,
						LinkedHashMap::new
				));
	}
}
