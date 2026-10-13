/*
 * Copyright 2012-present the original author or authors.
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
