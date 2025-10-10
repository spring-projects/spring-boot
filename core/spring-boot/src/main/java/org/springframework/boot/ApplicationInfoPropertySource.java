/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.env.PropertySourceInfo;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} which provides information about the application, like the
 * process ID (PID) or the version.
 *
 * @author Moritz Halbritter
 */
class ApplicationInfoPropertySource extends MapPropertySource implements PropertySourceInfo {

	static final String NAME = "applicationInfo";

	ApplicationInfoPropertySource(@Nullable Class<?> mainClass) {
		super(NAME, getProperties(readVersion(mainClass)));
	}

	ApplicationInfoPropertySource(@Nullable String applicationVersion) {
		super(NAME, getProperties(applicationVersion));
	}

	@Override
	public boolean isImmutable() {
		return true;
	}

	private static Map<String, Object> getProperties(@Nullable String applicationVersion) {
		Map<String, Object> result = new HashMap<>();
		if (StringUtils.hasText(applicationVersion)) {
			result.put("spring.application.version", applicationVersion);
		}
		ApplicationPid applicationPid = new ApplicationPid();
		Long pid = applicationPid.toLong();
		if (pid != null) {
			result.put("spring.application.pid", pid);
		}
		return result;
	}

	private static @Nullable String readVersion(@Nullable Class<?> applicationClass) {
		Package sourcePackage = (applicationClass != null) ? applicationClass.getPackage() : null;
		return (sourcePackage != null) ? sourcePackage.getImplementationVersion() : null;
	}

	/**
	 * Moves the {@link ApplicationInfoPropertySource} to the end of the environment's
	 * property sources.
	 * @param environment the environment
	 */
	static void moveToEnd(ConfigurableEnvironment environment) {
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> propertySource = propertySources.remove(NAME);
		if (propertySource != null) {
			propertySources.addLast(propertySource);
		}
	}

}
