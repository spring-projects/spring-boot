/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.boot.autoconfigure.data.elasticsearch;

import org.elasticsearch.Version;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Extension of {@link ElasticsearchContainer} to override default version.
 *
 * @author Scott Frederick
 */
public class VersionOverridingElasticsearchContainer extends ElasticsearchContainer {

	/**
	 * Elasticsearch Docker base URL
	 */
	private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch";

	/**
	 * Elasticsearch version
	 */
	protected static final String ELASTICSEARCH_VERSION = Version.CURRENT.toString();

	public VersionOverridingElasticsearchContainer() {
		super(ELASTICSEARCH_IMAGE + ":" + ELASTICSEARCH_VERSION);
	}

}
