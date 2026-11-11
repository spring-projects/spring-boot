/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} in a reactive environment configured
 * with {@link WebEnvironment#RANDOM_PORT}.
 *
 * @author Stephane Nicoll
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.main.webApplicationType=reactive", "value=123" })
public class SpringBootTestReactiveWebEnvironmentRandomPortTests
		extends AbstractSpringBootTestEmbeddedReactiveWebEnvironmentTests {

	@Configuration(proxyBeanMethods = false)
	@EnableWebFlux
	@RestController
	static class Config extends AbstractConfig {

	}

}
