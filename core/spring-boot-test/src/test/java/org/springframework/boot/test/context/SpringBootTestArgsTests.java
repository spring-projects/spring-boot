/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with application arguments.
 *
 * @author Justin Griffin
 * @author Stephane Nicoll
 */
@SpringBootTest(args = { "--option.foo=foo-value", "other.bar=other-bar-value" })
class SpringBootTestArgsTests {

	@Autowired
	private ApplicationArguments args;

	@Test
	void applicationArgumentsPopulated() {
		assertThat(this.args.getOptionNames()).containsOnly("option.foo");
		assertThat(this.args.getOptionValues("option.foo")).containsOnly("foo-value");
		assertThat(this.args.getNonOptionArgs()).containsOnly("other.bar=other-bar-value");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
