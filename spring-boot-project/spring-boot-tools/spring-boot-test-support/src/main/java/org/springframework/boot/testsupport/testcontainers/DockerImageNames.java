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

package org.springframework.boot.testsupport.testcontainers;

import org.testcontainers.utility.DockerImageName;

/**
 * Create {@link DockerImageName} instances for services used in integration tests.
 *
 * @author Stephane Nicoll
 * @since 2.3.6
 */
public final class DockerImageNames {

	private static final String CASSANDRA_VERSION = "3.11.10";

	private static final String COUCHBASE_VERSION = "6.5.1";

	private static final String ELASTICSEARCH_VERSION = "7.17.5";

	private static final String MONGO_VERSION = "4.0.23";

	private static final String NEO4J_VERSION = "4.0";

	private static final String POSTGRESQL_VERSION = "14.0";

	private static final String REDIS_VERSION = "4.0.14";

	private static final String REGISTRY_VERSION = "2.7.1";

	private DockerImageNames() {
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Cassandra.
	 * @return a docker image name for running cassandra
	 */
	public static DockerImageName cassandra() {
		return DockerImageName.parse("cassandra").withTag(CASSANDRA_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Couchbase.
	 * @return a docker image name for running couchbase
	 */
	public static DockerImageName couchbase() {
		return DockerImageName.parse("couchbase/server").withTag(COUCHBASE_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Elasticsearch.
	 * @return a docker image name for running elasticsearch
	 */
	public static DockerImageName elasticsearch() {
		return DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch").withTag(ELASTICSEARCH_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Mongo.
	 * @return a docker image name for running mongo
	 */
	public static DockerImageName mongo() {
		return DockerImageName.parse("mongo").withTag(MONGO_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Neo4j.
	 * @return a docker image name for running neo4j
	 */
	public static DockerImageName neo4j() {
		return DockerImageName.parse("neo4j").withTag(NEO4J_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running PostgreSQL.
	 * @return a docker image name for running postgresql
	 */
	public static DockerImageName postgresql() {
		return DockerImageName.parse("postgres").withTag(POSTGRESQL_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Redis.
	 * @return a docker image name for running redis
	 */
	public static DockerImageName redis() {
		return DockerImageName.parse("redis").withTag(REDIS_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running a Docker registry.
	 * @return a docker image name for running a registry
	 * @since 2.4.0
	 */
	public static DockerImageName registry() {
		return DockerImageName.parse("registry").withTag(REGISTRY_VERSION);
	}

}
