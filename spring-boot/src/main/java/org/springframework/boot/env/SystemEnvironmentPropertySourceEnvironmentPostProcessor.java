/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * An {@link EnvironmentPostProcessor} that replaces the systemEnvironment {@link SystemEnvironmentPropertySource}
 * with an {@link OriginTrackedSystemPropertySource} that can track the {@link SystemEnvironmentOrigin} for
 * every system environment property.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class SystemEnvironmentPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = SpringApplicationJsonEnvironmentPostProcessor.DEFAULT_ORDER - 1;

	private int order = DEFAULT_ORDER;

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String sourceName = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
		PropertySource<?> propertySource = environment.getPropertySources().get(sourceName);
		if (propertySource != null) {
			replacePropertySource(environment, sourceName, propertySource);
		}
	}

	@SuppressWarnings("unchecked")
	private void replacePropertySource(ConfigurableEnvironment environment, String sourceName, PropertySource<?> propertySource) {
		if (propertySource.getSource() instanceof Map) {
			Map<String, Object> originalSource = (Map<String, Object>) propertySource.getSource();
			Map<String, Object> originTrackedSource = new LinkedHashMap<>(originalSource);
			originTrackedSource.entrySet().forEach(e -> e.setValue(OriginTrackedValue.of(e.getValue(),
					new SystemEnvironmentOrigin(e.getKey()))));
			OriginTrackedSystemPropertySource source = new OriginTrackedSystemPropertySource(sourceName,
					Collections.unmodifiableMap(originTrackedSource));
			environment.getPropertySources().replace(sourceName, source);
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}

