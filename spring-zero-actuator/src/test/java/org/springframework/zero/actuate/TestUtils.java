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
package org.springframework.zero.actuate;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Dave Syer
 * 
 */
public class TestUtils {

	public static void addEnviroment(ConfigurableApplicationContext context,
			String... pairs) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (String pair : pairs) {
			int index = pair.indexOf(":");
			String key = pair.substring(0, index > 0 ? index : pair.length());
			String value = index > 0 ? pair.substring(index + 1) : "";
			map.put(key, value);
		}
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test", map));
	}

}
