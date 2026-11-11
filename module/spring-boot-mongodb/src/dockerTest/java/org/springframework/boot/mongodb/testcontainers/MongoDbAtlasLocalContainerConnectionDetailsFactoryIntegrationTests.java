/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.testcontainers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MongoDbAtlasLocalContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class MongoDbAtlasLocalContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final MongoDBAtlasLocalContainer mongoDb = TestImage.forContainer(MongoDBAtlasLocalContainer.class);

	@Autowired(required = false)
	private MongoConnectionDetails connectionDetails;

	@Test
	void connectionCanBeMadeToContainer() {
		assertThat(this.connectionDetails).isNotNull();
		MongoClient client = MongoClients.create(this.connectionDetails.getConnectionString());
		assertThat(client.listDatabaseNames()).containsExactly("admin", "config", "local");
	}

}
