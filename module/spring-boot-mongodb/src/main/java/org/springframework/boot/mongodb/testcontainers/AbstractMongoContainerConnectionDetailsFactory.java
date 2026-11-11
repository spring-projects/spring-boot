/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.testcontainers;

import java.util.function.Function;

import com.mongodb.ConnectionString;
import org.jspecify.annotations.Nullable;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Abstract {@link ContainerConnectionDetailsFactory} to create
 * {@link MongoConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated MongoDB container.
 *
 * @param <T> type of MongoDB container supported by the factory
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Wouter Blancquaert
 */
abstract class AbstractMongoContainerConnectionDetailsFactory<T extends GenericContainer<T>>
		extends ContainerConnectionDetailsFactory<T, MongoConnectionDetails> {

	private final Function<T, String> connectionStringFunction;

	AbstractMongoContainerConnectionDetailsFactory(Function<T, String> connectionStringFunction) {
		super(ANY_CONNECTION_NAME, "com.mongodb.ConnectionString");
		this.connectionStringFunction = connectionStringFunction;
	}

	@Override
	protected MongoConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<T> source) {
		return new MongoContainerConnectionDetails<>(source, this.connectionStringFunction);
	}

	/**
	 * {@link MongoConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MongoContainerConnectionDetails<T extends GenericContainer<T>>
			extends ContainerConnectionDetails<T> implements MongoConnectionDetails {

		private final Function<T, String> connectionStringFunction;

		private MongoContainerConnectionDetails(ContainerConnectionSource<T> source,
				Function<T, String> connectionStringFunction) {
			super(source);
			this.connectionStringFunction = connectionStringFunction;
		}

		@Override
		public ConnectionString getConnectionString() {
			return new ConnectionString(this.connectionStringFunction.apply(getContainer()));
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
