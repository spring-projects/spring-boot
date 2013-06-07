/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.endpoint.env;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Dave Syer
 */
@Controller
public class EnvEndpoint {

	private final ConfigurableEnvironment environment;

	public EnvEndpoint(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@RequestMapping("${endpoints.metrics.path:/env}")
	@ResponseBody
	public Map<String, Object> env() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (PropertySource<?> source : this.environment.getPropertySources()) {
			if (source instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				for (String name : enumerable.getPropertyNames()) {
					map.put(name, sanitize(name, enumerable.getProperty(name)));
				}
				result.put(source.getName(), map);
			}
		}
		return result;
	}

	@RequestMapping("${endpoints.metrics.path:/env}/{name:[a-zA-Z0-9._-]+}")
	@ResponseBody
	public Map<String, Object> env(@PathVariable String name) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put(name, sanitize(name, this.environment.getProperty(name)));
		return result;
	}

	private Object sanitize(String name, Object object) {
		if (name.toLowerCase().endsWith("password")
				|| name.toLowerCase().endsWith("secret")) {
			return object == null ? null : "******";
		}
		return object;
	}

}
