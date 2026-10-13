/*
 * Copyright 2012-present the original author or authors.
 */

 package org.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class SampleApplicationTests {

	@Autowired
	private MyBean myBean;

	@Test
	void contextLoads() {
		assertThat(this.myBean).isNotNull();
	}

	@Configuration
	static class MyConfig {

		@Bean
		MyBean myBean() {
			return new MyBean();
		}

	}

	static class MyBean {

	}

}
