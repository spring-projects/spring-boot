/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.testcontainers;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.cassandra.autoconfigure.CassandraConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class CassandraContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final CassandraContainer cassandra = TestImage.forContainer(CassandraContainer.class);

	@Autowired(required = false)
	private CassandraConnectionDetails connectionDetails;

	@Autowired
	private CqlSession cqlSession;

	@Test
	void connectionCanBeMadeToCassandraContainer() {
		assertThat(this.connectionDetails).isNotNull();
		assertThat(this.cqlSession.getMetadata().getNodes()).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(CassandraAutoConfiguration.class)
	static class TestConfiguration {

	}

}
