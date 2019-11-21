/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.cloud;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

/**
 * An {@link EnvironmentPostProcessor} that knows where to find VCAP (a.k.a. Cloud
 * Foundry) meta data in the existing environment. It parses out the VCAP_APPLICATION and
 * VCAP_SERVICES meta data and dumps it in a form that is easily consumed by
 * {@link Environment} users. If the app is running in Cloud Foundry then both meta data
 * items are JSON objects encoded in OS environment variables. VCAP_APPLICATION is a
 * shallow hash with basic information about the application (name, instance id, instance
 * index, etc.), and VCAP_SERVICES is a hash of lists where the keys are service labels
 * and the values are lists of hashes of service instance meta data. Examples are:
 *
 * <pre class="code">
 * VCAP_APPLICATION: {"instance_id":"2ce0ac627a6c8e47e936d829a3a47b5b","instance_index":0,
 *   "version":"0138c4a6-2a73-416b-aca0-572c09f7ca53","name":"foo",
 *   "uris":["foo.cfapps.io"], ...}
 * VCAP_SERVICES: {"rds-mysql-1.0":[{"name":"mysql","label":"rds-mysql-1.0","plan":"10mb",
 *   "credentials":{"name":"d04fb13d27d964c62b267bbba1cffb9da","hostname":"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com",
 *   "host":"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com","port":3306,"user":"urpRuqTf8Cpe6",
 *   "username":"urpRuqTf8Cpe6","password":"pxLsGVpsC9A5S"}
 * }]}
 * </pre>
 *
 * These objects are flattened into properties. The VCAP_APPLICATION object goes straight
 * to {@code vcap.application.*} in a fairly obvious way, and the VCAP_SERVICES object is
 * unwrapped so that it is a hash of objects with key equal to the service instance name
 * (e.g. "mysql" in the example above), and value equal to that instances properties, and
 * then flattened in the same way. E.g.
 *
 * <pre class="code">
 * vcap.application.instance_id: 2ce0ac627a6c8e47e936d829a3a47b5b
 * vcap.application.version: 0138c4a6-2a73-416b-aca0-572c09f7ca53
 * vcap.application.name: foo
 * vcap.application.uris[0]: foo.cfapps.io
 *
 * vcap.services.mysql.name: mysql
 * vcap.services.mysql.label: rds-mysql-1.0
 * vcap.services.mysql.credentials.name: d04fb13d27d964c62b267bbba1cffb9da
 * vcap.services.mysql.credentials.port: 3306
 * vcap.services.mysql.credentials.host: mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com
 * vcap.services.mysql.credentials.username: urpRuqTf8Cpe6
 * vcap.services.mysql.credentials.password: pxLsGVpsC9A5S
 * ...
 * </pre>
 *
 * N.B. this initializer is mainly intended for informational use (the application and
 * instance ids are particularly useful). For service binding you might find that Spring
 * Cloud is more convenient and more robust against potential changes in Cloud Foundry.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class CloudFoundryVcapEnvironmentPostProcessor
		implements EnvironmentPostProcessor, Ordered, ApplicationListener<ApplicationPreparedEvent> {

	private static final DeferredLog logger = new DeferredLog();

	private static final String VCAP_APPLICATION = "VCAP_APPLICATION";

	private static final String VCAP_SERVICES = "VCAP_SERVICES";

	// Before ConfigFileApplicationListener so values there can use these ones
	private int order = ConfigFileApplicationListener.DEFAULT_ORDER - 1;

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (CloudPlatform.CLOUD_FOUNDRY.isActive(environment)) {
			Properties properties = new Properties();
			JsonParser jsonParser = JsonParserFactory.getJsonParser();
			addWithPrefix(properties, getPropertiesFromApplication(environment, jsonParser), "vcap.application.");
			addWithPrefix(properties, getPropertiesFromServices(environment, jsonParser), "vcap.services.");
			MutablePropertySources propertySources = environment.getPropertySources();
			if (propertySources.contains(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
				propertySources.addAfter(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
						new PropertiesPropertySource("vcap", properties));
			}
			else {
				propertySources.addFirst(new PropertiesPropertySource("vcap", properties));
			}
		}
	}

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		logger.switchTo(CloudFoundryVcapEnvironmentPostProcessor.class);
	}

	private void addWithPrefix(Properties properties, Properties other, String prefix) {
		for (String key : other.stringPropertyNames()) {
			String prefixed = prefix + key;
			properties.setProperty(prefixed, other.getProperty(key));
		}
	}

	private Properties getPropertiesFromApplication(Environment environment, JsonParser parser) {
		Properties properties = new Properties();
		try {
			String property = environment.getProperty(VCAP_APPLICATION, "{}");
			Map<String, Object> map = parser.parseMap(property);
			extractPropertiesFromApplication(properties, map);
		}
		catch (Exception ex) {
			logger.error("Could not parse VCAP_APPLICATION", ex);
		}
		return properties;
	}

	private Properties getPropertiesFromServices(Environment environment, JsonParser parser) {
		Properties properties = new Properties();
		try {
			String property = environment.getProperty(VCAP_SERVICES, "{}");
			Map<String, Object> map = parser.parseMap(property);
			extractPropertiesFromServices(properties, map);
		}
		catch (Exception ex) {
			logger.error("Could not parse VCAP_SERVICES", ex);
		}
		return properties;
	}

	private void extractPropertiesFromApplication(Properties properties, Map<String, Object> map) {
		if (map != null) {
			flatten(properties, map, "");
		}
	}

	private void extractPropertiesFromServices(Properties properties, Map<String, Object> map) {
		if (map != null) {
			for (Object services : map.values()) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) services;
				for (Object object : list) {
					@SuppressWarnings("unchecked")
					Map<String, Object> service = (Map<String, Object>) object;
					String key = (String) service.get("name");
					if (key == null) {
						key = (String) service.get("label");
					}
					flatten(properties, service, key);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void flatten(Properties properties, Map<String, Object> input, String path) {
		input.forEach((key, value) -> {
			String name = getPropertyName(path, key);
			if (value instanceof Map) {
				// Need a compound key
				flatten(properties, (Map<String, Object>) value, name);
			}
			else if (value instanceof Collection) {
				// Need a compound key
				Collection<Object> collection = (Collection<Object>) value;
				properties.put(name, StringUtils.collectionToCommaDelimitedString(collection));
				int count = 0;
				for (Object item : collection) {
					String itemKey = "[" + (count++) + "]";
					flatten(properties, Collections.singletonMap(itemKey, item), name);
				}
			}
			else if (value instanceof String) {
				properties.put(name, value);
			}
			else if (value instanceof Number) {
				properties.put(name, value.toString());
			}
			else if (value instanceof Boolean) {
				properties.put(name, value.toString());
			}
			else {
				properties.put(name, (value != null) ? value : "");
			}
		});
	}

	private String getPropertyName(String path, String key) {
		if (!StringUtils.hasText(path)) {
			return key;
		}
		if (key.startsWith("[")) {
			return path + key;
		}
		return path + "." + key;
	}

}
