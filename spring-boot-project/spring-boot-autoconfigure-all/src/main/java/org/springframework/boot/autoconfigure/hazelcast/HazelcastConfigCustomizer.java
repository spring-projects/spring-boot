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

package org.springframework.boot.autoconfigure.hazelcast;

import com.hazelcast.config.Config;

/**
 * Callback interface that can be implemented by beans wishing to customize the Hazelcast
 * server {@link Config configuration}.
 *
 * @author Jaromir Hamala
 * @author Stephane Nicoll
 * @since 2.7.0
 */
@FunctionalInterface
public interface HazelcastConfigCustomizer {

	/**
	 * Customize the configuration.
	 * @param config the {@link Config} to customize
	 */
	void customize(Config config);

}
