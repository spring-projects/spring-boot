/*
 * Copyright 2010-2015 the original author or authors.
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

package org.springframework.boot.cloudfoundry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationListener} that knows where to find VCAP (a.k.a. Cloud Foundry)
 * meta data in the existing environment. It parses out the VCAP_APPLICATION and
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
 * to <code>vcap.application.*</code> in a fairly obvious way, and the VCAP_SERVICES
 * object is unwrapped so that it is a hash of objects with key equal to the service
 * instance name (e.g. "mysql" in the example above), and value equal to that instances
 * properties, and then flattened in the same way. E.g.
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
 */
public class VcapApplicationListener implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private static final Log logger = LogFactory.getLog(VcapApplicationListener.class);

	private static final String VCAP_APPLICATION = "VCAP_APPLICATION";

	private static final String VCAP_SERVICES = "VCAP_SERVICES";

	// Before ConfigFileApplicationListener so values there can use these ones
	private int order = ConfigFileApplicationListener.DEFAULT_ORDER - 1;

	private final JsonParser parser = JsonParserFactory.getJsonParser();

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		if (!environment.containsProperty(VCAP_APPLICATION)
				&& !environment.containsProperty(VCAP_SERVICES)) {
			return;
		}
		Properties properties = new Properties();
		addWithPrefix(properties, getPropertiesFromApplication(environment),
				"vcap.application.");
		addWithPrefix(properties, getPropertiesFromServices(environment),
				"vcap.services.");
		MutablePropertySources propertySources = environment.getPropertySources();
		if (propertySources
				.contains(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
			propertySources.addAfter(
					CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
					new PropertiesPropertySource("vcap", properties));
		}
		else {
			propertySources.addFirst(new PropertiesPropertySource("vcap", properties));
		}
	}

	private void addWithPrefix(Properties properties, Properties other, String prefix) {
		for (String key : other.stringPropertyNames()) {
			String prefixed = prefix + key;
			properties.setProperty(prefixed, other.getProperty(key));
		}
	}

	private Properties getPropertiesFromApplication(Environment environment) {
		Properties properties = new Properties();
		try {
			Map<String, Object> map = this.parser.parseMap(environment.getProperty(
					VCAP_APPLICATION, "{}"));
			extractPropertiesFromApplication(properties, map);
		}
		catch (Exception ex) {
			logger.error("Could not parse VCAP_APPLICATION", ex);
		}
		return properties;
	}

	private Properties getPropertiesFromServices(Environment environment) {
		Properties properties = new Properties();
		try {
			Map<String, Object> map = this.parser.parseMap(environment.getProperty(
					VCAP_SERVICES, "{}"));
			extractPropertiesFromServices(properties, map);
		}
		catch (Exception ex) {
			logger.error("Could not parse VCAP_SERVICES", ex);
		}
		return properties;
	}

	private void extractPropertiesFromApplication(Properties properties,
			Map<String, Object> map) {
		if (map != null) {
			flatten(properties, map, "");
		}
	}

	private void extractPropertiesFromServices(Properties properties,
			Map<String, Object> map) {
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
		for (Entry<String, Object> entry : input.entrySet()) {
			String key = getFullKey(path, entry.getKey());
			Object value = entry.getValue();
			if (value instanceof Map) {
				// Need a compound key
				flatten(properties, (Map<String, Object>) value, key);
			}
			else if (value instanceof Collection) {
				// Need a compound key
				Collection<Object> collection = (Collection<Object>) value;
				properties.put(key,
						StringUtils.collectionToCommaDelimitedString(collection));
				int count = 0;
				for (Object item : collection) {
					String itemKey = "[" + (count++) + "]";
					flatten(properties, Collections.singletonMap(itemKey, item), key);
				}
			}
			else if (value instanceof String) {
				properties.put(key, value);
			}
			else if (value instanceof Number) {
				properties.put(key, value.toString());
			}
			else if (value instanceof Boolean) {
				properties.put(key, value.toString());
			}
			else {
				properties.put(key, value == null ? "" : value);
			}
		}
	}

	private String getFullKey(String path, String key) {
		if (!StringUtils.hasText(path)) {
			return key;
		}
		if (key.startsWith("[")) {
			return path + key;
		}
		return path + "." + key;
	}

}
