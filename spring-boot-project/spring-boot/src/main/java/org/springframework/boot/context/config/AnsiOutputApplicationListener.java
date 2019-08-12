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

package org.springframework.boot.context.config;

import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * An {@link ApplicationListener} that configures {@link AnsiOutput} depending on the
 * value of the property {@code spring.output.ansi.enabled}. See {@link Enabled} for valid
 * values.
 *
 * @author Raphael von der Grün
 * @author Madhura Bhave
 * @since 1.2.0
 */
public class AnsiOutputApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		Binder.get(environment).bind("spring.output.ansi.enabled", AnsiOutput.Enabled.class)
				.ifBound(AnsiOutput::setEnabled);
		AnsiOutput.setConsoleAvailable(environment.getProperty("spring.output.ansi.console-available", Boolean.class));
	}

	@Override
	public int getOrder() {
		// Apply after ConfigFileApplicationListener has called EnvironmentPostProcessors
		return ConfigFileApplicationListener.DEFAULT_ORDER + 1;
	}

}
