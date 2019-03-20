/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.hypermedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link EnvironmentEndpoint} with reduced output to look better in the documentation.
 *
 * @author Phillip Webb
 */
public class LimitedEnvironmentEndpoint extends EnvironmentEndpoint {

	private static final MultiValueMap<String, String> INCLUDES;

	static {
		Map<String, List<String>> includes = new LinkedHashMap<String, List<String>>();
		List<String> systemProperties = new ArrayList<String>();
		systemProperties.add("java.runtime.name");
		systemProperties.add("sun.boot.library.path");
		systemProperties.add("java.vendor.url");
		systemProperties.add("path.separator");
		systemProperties.add("sun.java.launcher");
		systemProperties.add("java.runtime.version");
		systemProperties.add("os.arch");
		systemProperties.add("line.separator");
		systemProperties.add("os.name");
		systemProperties.add("user.timezone");
		systemProperties.add("file.encoding");
		systemProperties.add("java.vm.specification.version");
		systemProperties.add("sun.java.command");
		systemProperties.add("sun.arch.data.model");
		systemProperties.add("user.language");
		systemProperties.add("awt.toolkit");
		systemProperties.add("java.awt.headless");
		systemProperties.add("java.vendor");
		systemProperties.add("file.separator");
		includes.put("systemProperties", systemProperties);
		List<String> systemEnvironment = new ArrayList<String>();
		systemEnvironment.add("SHELL");
		systemEnvironment.add("TMPDIR");
		systemEnvironment.add("DISPLAY");
		systemEnvironment.add("LOGNAME");
		includes.put("systemEnvironment", systemEnvironment);
		INCLUDES = new LinkedMultiValueMap<String, String>(
				Collections.unmodifiableMap(includes));
	}

	@Override
	public Object sanitize(String name, Object object) {
		if (name.equals("gopherProxySet")) {
			return object;
		}
		return null;
	}

	@Override
	protected Map<String, Object> postProcessSourceProperties(String sourceName,
			Map<String, Object> properties) {
		List<String> sourceIncludes = INCLUDES.get(sourceName);
		if (sourceIncludes != null) {
			Set<Entry<String, Object>> entries = properties.entrySet();
			Iterator<Map.Entry<String, Object>> iterator = entries.iterator();
			while (iterator.hasNext()) {
				if (!sourceIncludes.contains(iterator.next().getKey())) {
					iterator.remove();
				}
			}

		}
		return properties;
	}

}
