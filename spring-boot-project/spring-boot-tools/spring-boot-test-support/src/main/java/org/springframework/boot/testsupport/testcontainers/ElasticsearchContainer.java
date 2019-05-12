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

package org.springframework.boot.testsupport.testcontainers;

import java.time.Duration;

/**
 * A {@link Container} for Elasticsearch.
 *
 * @author Andy Wilkinson
 */
public class ElasticsearchContainer extends Container {

	public ElasticsearchContainer() {
		super("elasticsearch:6.4.3", 9200,
				(container) -> container.withStartupTimeout(Duration.ofSeconds(120))
						.withStartupAttempts(5).withEnv("discovery.type", "single-node")
						.addExposedPort(9300));
	}

	public int getMappedTransportPort() {
		return getContainer().getMappedPort(9300);
	}

	@Override
	public void start() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
		super.start();
	}

	@Override
	public void stop() {
		System.clearProperty("es.set.netty.runtime.available.processors");
		super.stop();
	}

}
