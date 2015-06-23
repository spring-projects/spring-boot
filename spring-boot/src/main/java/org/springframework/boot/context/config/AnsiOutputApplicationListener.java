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

package org.springframework.boot.context.config;

import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * An {@link ApplicationListener} that configures {@link AnsiOutput} depending on the the
 * value of the property {@code spring.output.ansi.enabled}. See {@link Enabled} for valid
 * values.
 *
 * @author Raphael von der Grün
 * @since 1.2.0
 */
public class AnsiOutputApplicationListener implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				event.getEnvironment(), "spring.output.ansi.");
		if (resolver.containsProperty("enabled")) {
			String enabled = resolver.getProperty("enabled");
			AnsiOutput.setEnabled(Enum.valueOf(Enabled.class, enabled.toUpperCase()));
		}

		if (resolver.containsProperty("console-available")) {
			AnsiOutput.setConsoleAvailable(resolver.getProperty("console-available",
					Boolean.class));
		}
	}

	@Override
	public int getOrder() {
		// Apply after the ConfigFileApplicationListener
		return ConfigFileApplicationListener.DEFAULT_ORDER + 1;
	}

}
