/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.context;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ShutdownEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class ShutdownEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ShutdownEndpointAutoConfiguration.class));

	@Test
	@SuppressWarnings("unchecked")
	void runShouldHaveEndpointBeanThatIsNotDisposable() {
		this.contextRunner.withPropertyValues("management.endpoint.shutdown.enabled:true")
			.withPropertyValues("management.endpoints.web.exposure.include=shutdown")
			.run((context) -> {
				assertThat(context).hasSingleBean(ShutdownEndpoint.class);
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				Map<String, Object> disposableBeans = (Map<String, Object>) ReflectionTestUtils.getField(beanFactory,
						"disposableBeans");
				assertThat(disposableBeans).isEmpty();
			});
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.shutdown.enabled:true")
			.run((context) -> assertThat(context).doesNotHaveBean(ShutdownEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.shutdown.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(ShutdownEndpoint.class));
	}

}
