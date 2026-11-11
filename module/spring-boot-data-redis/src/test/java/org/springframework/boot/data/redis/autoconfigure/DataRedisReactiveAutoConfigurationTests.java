/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisReactiveAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class DataRedisReactiveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(DataRedisAutoConfiguration.class, DataRedisReactiveAutoConfiguration.class));

	@Test
	void testDefaultRedisConfiguration() {
		this.contextRunner.run((context) -> {
			Map<String, ?> beans = context.getBeansOfType(ReactiveRedisTemplate.class);
			assertThat(beans).containsOnlyKeys("reactiveRedisTemplate", "reactiveStringRedisTemplate");
		});
	}

}
