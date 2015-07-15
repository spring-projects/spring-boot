/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.boot.actuate.info;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * A {@link InfoProvider} that provides all environment entries prefixed with info
 * 
 * See something
 *
 * @author Meang Akira Tanaka
 * @since 1.3.0
 */
public class EnvironmentInfoProvider implements InfoProvider {

	private final ConfigurableEnvironment environment;
	private final Map<String, Object> infoMap;
	private final Info info;

	public EnvironmentInfoProvider(ConfigurableEnvironment environment) throws Exception {
		this.environment = environment;
		infoMap = extractInfoFromEnvironment();
		this.info = new Info(infoMap);
	}

	@Override
	public String name() {
		return "environment";
	}

	@Override
	public Info provide() {
		return info;
	}

	private Map<String, Object> extractInfoFromEnvironment() throws Exception {
		PropertiesConfigurationFactory<Map<String, Object>> factory = new PropertiesConfigurationFactory<Map<String, Object>>(
				new LinkedHashMap<String, Object>());
		factory.setTargetName("info");
		factory.setPropertySources(this.environment.getPropertySources());
		return factory.getObject();
	}
}
