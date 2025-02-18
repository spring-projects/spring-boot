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

package org.springframework.boot.autoconfigure.service.connection;

/**
 * A factory to create {@link ConnectionDetails} from a given {@code source}.
 * Implementations should be registered in {@code META-INF/spring.factories}.
 *
 * @param <S> the source type accepted by the factory. Implementations are expected to
 * provide a valid {@code toString}.
 * @param <D> the type of {@link ConnectionDetails} produced by the factory
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface ConnectionDetailsFactory<S, D extends ConnectionDetails> {

	/**
	 * Get the {@link ConnectionDetails} from the given {@code source}. May return
	 * {@code null} if no details can be created.
	 * @param source the source
	 * @return the connection details or {@code null}
	 */
	D getConnectionDetails(S source);

}
