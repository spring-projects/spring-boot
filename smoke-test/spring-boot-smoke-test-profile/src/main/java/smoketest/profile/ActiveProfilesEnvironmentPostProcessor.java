/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.profile;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link EnvironmentPostProcessor} that adds an active profile.
 *
 * @author Madhura Bhave
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class ActiveProfilesEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.getProperty("enableEnvironmentPostProcessor") != null) {
			environment.addActiveProfile("dev");
		}
	}

}
