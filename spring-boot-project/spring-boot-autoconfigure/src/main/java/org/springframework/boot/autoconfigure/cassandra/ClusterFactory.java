/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Initializer;

/**
 * {@code ClusterFactory} provides control over the creation of a {@link Cluster} from an
 * {@link Initializer}.
 *
 * @author Steffen F. Qvistgaard
 * @since 2.2.0
 */
@FunctionalInterface
public interface ClusterFactory {

	/**
	 * Creates a {@link Cluster} from the given {@link Initializer}.
	 * @param initializer the {@code Initializer}
	 * @return the {@code Cluster}
	 */
	Cluster create(Initializer initializer);

}
