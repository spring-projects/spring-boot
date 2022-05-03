/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import com.couchbase.client.java.env.ClusterEnvironment;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link ClusterEnvironment} via a
 * {@link com.couchbase.client.java.env.ClusterEnvironment.Builder
 * ClusterEnvironment.Builder} whilst retaining default auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
@FunctionalInterface
public interface ClusterEnvironmentBuilderCustomizer {

	/**
	 * Customize the {@link com.couchbase.client.java.env.ClusterEnvironment.Builder
	 * ClusterEnvironment.Builder}.
	 * @param builder the builder to customize
	 */
	void customize(ClusterEnvironment.Builder builder);

}
