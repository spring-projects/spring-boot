/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.autoconfigure;

import org.mockito.ArgumentMatchers;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Driver configuration mocked to avoid instantiation of a real driver with connection
 * creation.
 *
 * @author Michael J. Simons
 */
@Configuration(proxyBeanMethods = false)
class MockedDriverConfiguration {

	@Bean
	Driver driver() {
		Driver driver = mock(Driver.class);
		Session session = mock(Session.class);
		given(driver.session(ArgumentMatchers.any(SessionConfig.class))).willReturn(session);
		return driver;
	}

}
