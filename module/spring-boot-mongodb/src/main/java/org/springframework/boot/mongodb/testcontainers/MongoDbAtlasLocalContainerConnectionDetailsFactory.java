/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.testcontainers;

import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link MongoConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link MongoDBAtlasLocalContainer}.
 *
 * @author Wouter Blancquaert
 */
class MongoDbAtlasLocalContainerConnectionDetailsFactory
		extends AbstractMongoContainerConnectionDetailsFactory<MongoDBAtlasLocalContainer> {

	MongoDbAtlasLocalContainerConnectionDetailsFactory() {
		super(MongoDBAtlasLocalContainer::getDatabaseConnectionString);
	}

}
