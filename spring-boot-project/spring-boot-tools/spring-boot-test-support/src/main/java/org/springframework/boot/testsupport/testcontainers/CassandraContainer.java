/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.testsupport.testcontainers;

import java.time.Duration;

/**
 * Custom {@link org.testcontainers.containers.CassandraContainer} tuned for stability in
 * heavily contended environments such as CI.
 *
 * @author Andy Wilkinson
 * @since 2.4.10
 */
public class CassandraContainer extends org.testcontainers.containers.CassandraContainer<CassandraContainer> {

	public CassandraContainer() {
		super(DockerImageNames.cassandra());
		withStartupTimeout(Duration.ofMinutes(10));
	}

}
