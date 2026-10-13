/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.support;

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
 * @since 4.0.0
 */
public class AnsiOutputApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		Binder.get(environment)
			.bind("spring.output.ansi.enabled", AnsiOutput.Enabled.class)
			.ifBound(AnsiOutput::setEnabled);
		AnsiOutput.setConsoleAvailable(environment.getProperty("spring.output.ansi.console-available", Boolean.class));
	}

	@Override
	public int getOrder() {
		// Apply after EnvironmentPostProcessorApplicationListener
		return EnvironmentPostProcessorApplicationListener.DEFAULT_ORDER + 1;
	}

}
