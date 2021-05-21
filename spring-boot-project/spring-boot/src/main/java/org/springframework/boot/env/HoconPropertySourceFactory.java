package org.springframework.boot.env;

import com.typesafe.config.*;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link PropertySourceFactory} factory that loads a HOCON file into an instance of {@link PropertySource}.
 */
public class HoconPropertySourceFactory implements PropertySourceFactory {
	private static final ConfigParseOptions PARSE_OPTIONS =
			ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF);

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource encoded) throws IOException {
		Resource resource = encoded.getResource();
		String actualName = Optional.ofNullable(name).orElseGet(resource::getFilename);
		Map<String, Object> source = toFlatMap(resource, parseHoconFrom(resource));
		return new OriginTrackedMapPropertySource(actualName, source);
	}

	private Config parseHoconFrom(Resource resource) throws IOException {
		try (InputStream inputStream = resource.getInputStream(); InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			return ConfigFactory.parseReader(reader, PARSE_OPTIONS).resolve();
		}
	}

	private Map<String, Object> toFlatMap(Resource resource, Config config) {
		Map<String, Object> properties = new LinkedHashMap<>();
		toFlatMap(properties, "", resource, config);
		return properties;
	}

	private void toFlatMap(Map<String, Object> properties, String parentKey, Resource resource, Config config) {
		final String prefix = "".equals(parentKey) ? "" : parentKey + ".";

		config.entrySet().stream().forEach(entry -> {
			String propertyKey = prefix + entry.getKey();
			addConfigValuePropertyTo(properties, propertyKey, resource, entry.getValue());
		});
	}

	private void addConfigValuePropertyTo(Map<String, Object> properties, String key, Resource resource, ConfigValue value) {
		if (value instanceof ConfigList) {
			processListValue(properties, key, resource, (ConfigList) value);
		} else if (value instanceof ConfigObject) {
			processObjectValue(properties, key, resource, (ConfigObject) value);
		} else {
			processScalarValue(properties, key, resource, value);
		}
	}

	private void processListValue(Map<String, Object> properties, String key, Resource resource, ConfigList value) {
		if (value.isEmpty()) {
			addConfigValuePropertyTo(properties, key, resource, ConfigValueFactory.fromAnyRef(""));
			return;
		}

		for (int i = 0; i < value.size(); i++) {
			// Used to properly populate lists in @ConfigurationProperties beans
			String propertyName = String.format("%s[%d]", key, i);
			ConfigValue propertyValue = value.get(i);
			addConfigValuePropertyTo(properties, propertyName, resource, propertyValue);
		}
	}

	private void processObjectValue(Map<String, Object> properties, String key, Resource resource, ConfigObject value) {
		toFlatMap(properties, key, resource, value.toConfig());
	}

	private void processScalarValue(Map<String, Object> properties, String key, Resource resource, ConfigValue value) {
		properties.put(key, value.unwrapped());
		Origin origin = new TextResourceOrigin(resource, new TextResourceOrigin.Location(value.origin().lineNumber() - 1, 0));
		properties.put(key, OriginTrackedValue.of(value.unwrapped(), origin));
	}

}
