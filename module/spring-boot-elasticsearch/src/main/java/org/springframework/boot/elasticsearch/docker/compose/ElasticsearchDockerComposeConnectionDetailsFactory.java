/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.elasticsearch.docker.compose;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.ssl.SslBundle;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ElasticsearchConnectionDetails} for an {@code elasticsearch} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ElasticsearchDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ElasticsearchConnectionDetails> {

	private static final int ELASTICSEARCH_PORT = 9200;

	protected ElasticsearchDockerComposeConnectionDetailsFactory() {
		super(new String[] { "elasticsearch", "elasticsearch/elasticsearch" });
	}

	@Override
	protected ElasticsearchConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ElasticsearchDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ElasticsearchConnectionDetails} backed by an {@code elasticsearch}
	 * {@link RunningService}.
	 */
	static class ElasticsearchDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ElasticsearchConnectionDetails {

		private final ElasticsearchEnvironment environment;

		private final List<Node> nodes;

		private final @Nullable SslBundle sslBundle;

		ElasticsearchDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ElasticsearchEnvironment(service.env());
			this.sslBundle = getSslBundle(service);
			Protocol protocol = (this.sslBundle != null) ? Protocol.HTTPS : Protocol.HTTP;
			this.nodes = List.of(new Node(service.host(), service.ports().get(ELASTICSEARCH_PORT), protocol,
					getUsername(), getPassword()));
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

		@Override
		public String getUsername() {
			return "elastic";
		}

		@Override
		public @Nullable String getPassword() {
			return this.environment.getPassword();
		}

		@Override
		public List<Node> getNodes() {
			return this.nodes;
		}

	}

}
