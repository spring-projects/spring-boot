/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docker.compose.service.connection;

import java.util.Arrays;
import java.util.function.Predicate;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Base class for {@link ConnectionDetailsFactory} implementations that provide
 * {@link ConnectionDetails} from a {@link DockerComposeConnectionSource}.
 *
 * @param <D> the connection details type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public abstract class DockerComposeConnectionDetailsFactory<D extends ConnectionDetails>
		implements ConnectionDetailsFactory<DockerComposeConnectionSource, D> {

	private final Predicate<DockerComposeConnectionSource> predicate;

	private final String[] requiredClassNames;

	/**
	 * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
	 * @param connectionName the required connection name
	 * @param requiredClassNames the names of classes that must be present
	 */
	protected DockerComposeConnectionDetailsFactory(String connectionName, String... requiredClassNames) {
		this(new ConnectionNamePredicate(connectionName), requiredClassNames);
	}

	/**
	 * Create a new {@link DockerComposeConnectionDetailsFactory} instance.
	 * @param predicate a predicate used to check when a service is accepted
	 * @param requiredClassNames the names of classes that must be present
	 */
	protected DockerComposeConnectionDetailsFactory(Predicate<DockerComposeConnectionSource> predicate,
			String... requiredClassNames) {
		this.predicate = predicate;
		this.requiredClassNames = requiredClassNames;
	}

	@Override
	public final D getConnectionDetails(DockerComposeConnectionSource source) {
		return (!accept(source)) ? null : getDockerComposeConnectionDetails(source);
	}

	private boolean accept(DockerComposeConnectionSource source) {
		return hasRequiredClasses() && this.predicate.test(source);
	}

	private boolean hasRequiredClasses() {
		return ObjectUtils.isEmpty(this.requiredClassNames) || Arrays.stream(this.requiredClassNames)
			.allMatch((requiredClassName) -> ClassUtils.isPresent(requiredClassName, null));
	}

	/**
	 * Get the {@link ConnectionDetails} from the given {@link RunningService}
	 * {@code source}. May return {@code null} if no connection can be created. Result
	 * types should consider extending {@link DockerComposeConnectionDetails}.
	 * @param source the source
	 * @return the service connection or {@code null}.
	 */
	protected abstract D getDockerComposeConnectionDetails(DockerComposeConnectionSource source);

	/**
	 * Convenient base class for {@link ConnectionDetails} results that are backed by a
	 * {@link RunningService}.
	 */
	protected static class DockerComposeConnectionDetails implements ConnectionDetails, OriginProvider {

		private final Origin origin;

		/**
		 * Create a new {@link DockerComposeConnectionDetails} instance.
		 * @param runningService the source {@link RunningService}
		 */
		protected DockerComposeConnectionDetails(RunningService runningService) {
			Assert.notNull(runningService, "RunningService must not be null");
			this.origin = Origin.from(runningService);
		}

		@Override
		public Origin getOrigin() {
			return this.origin;
		}

	}

}
