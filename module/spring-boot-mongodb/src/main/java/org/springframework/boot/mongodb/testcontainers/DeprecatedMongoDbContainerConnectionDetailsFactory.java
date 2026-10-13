/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.testcontainers;

import org.testcontainers.containers.MongoDBContainer;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link MongoConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link MongoDBContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link MongoDbContainerConnectionDetailsFactory}.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
class DeprecatedMongoDbContainerConnectionDetailsFactory
		extends AbstractMongoContainerConnectionDetailsFactory<MongoDBContainer> {

	DeprecatedMongoDbContainerConnectionDetailsFactory() {
		super(MongoDBContainer::getReplicaSetUrl);
	}

}
