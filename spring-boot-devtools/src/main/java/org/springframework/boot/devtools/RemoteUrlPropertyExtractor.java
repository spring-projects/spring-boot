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

package org.springframework.boot.devtools;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationListener} to extract the remote URL for the
 * {@link RemoteSpringApplication} to use.
 *
 * @author Phillip Webb
 */
class RemoteUrlPropertyExtractor implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private static final String NON_OPTION_ARGS = CommandLinePropertySource.DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		String url = environment.getProperty(NON_OPTION_ARGS);
		Assert.state(StringUtils.hasLength(url), "No remote URL specified");
		Assert.state(url.indexOf(",") == -1, "Multiple URLs specified");
		try {
			new URI(url);
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Malformed URL '" + url + "'");
		}
		Map<String, Object> source = Collections.singletonMap("remoteUrl", (Object) url);
		PropertySource<?> propertySource = new MapPropertySource("remoteUrl", source);
		environment.getPropertySources().addLast(propertySource);
	}

}
