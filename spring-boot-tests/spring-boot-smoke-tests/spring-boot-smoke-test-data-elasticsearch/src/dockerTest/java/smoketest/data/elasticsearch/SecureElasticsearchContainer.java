/*
 * Copyright 2012-2025 the original author or authors.
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

package smoketest.data.elasticsearch;

import java.util.Map;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A {@link ElasticsearchContainer} for Elasticsearch with SSL configuration.
 *
 * @author Moritz Halbritter
 */
class SecureElasticsearchContainer extends ElasticsearchContainer {

	SecureElasticsearchContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	@Override
	public void configure() {
		// See
		// https://www.elastic.co/guide/en/elastic-stack-get-started/7.5/get-started-docker.html#get-started-docker-tls
		withEnv(Map.of("xpack.security.http.ssl.enabled", "true", "xpack.security.http.ssl.key",
				"/usr/share/elasticsearch/config/ssl.key", "xpack.security.http.ssl.certificate",
				"/usr/share/elasticsearch/config/ssl.crt", "xpack.security.transport.ssl.enabled", "true",
				"xpack.security.transport.ssl.verification_mode", "certificate", "xpack.security.transport.ssl.key",
				"/usr/share/elasticsearch/config/ssl.key", "xpack.security.transport.ssl.certificate",
				"/usr/share/elasticsearch/config/ssl.crt"));
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl.crt"),
				"/usr/share/elasticsearch/config/ssl.crt");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl.key"),
				"/usr/share/elasticsearch/config/ssl.key");
	}

}
