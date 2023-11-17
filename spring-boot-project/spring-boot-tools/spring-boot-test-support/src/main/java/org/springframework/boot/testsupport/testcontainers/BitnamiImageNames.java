/*
 * Copyright 2012-2024 the original author or authors.
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
 * Create {@link DockerImageName} for Bitnami instances for services used in integration
 * tests.
 *
 * @author Scott Frederick
 */
public final class BitnamiImageNames {

	private static final String CASSANDRA_VERSION = "4.1.3";

	private static final String ELASTICSEARCH_VERSION = "8.12.1";

	private static final String MARIADB_VERSION = "11.2.3";

	private static final String MONGO_VERSION = "7.0.5";

	private static final String MYSQL_VERSION = "8.0.36";

	private static final String NEO4J_VERSION = "5.16.0";

	private static final String POSTGRESQL_VERSION = "16.2.0";

	private static final String RABBIT_VERSION = "3.11.28";

	private static final String REDIS_VERSION = "7.2.4";

	private BitnamiImageNames() {
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Cassandra.
	 * @return a docker image name for running cassandra
	 */
	public static DockerImageName cassandra() {
		return DockerImageName.parse("bitnami/cassandra").withTag(CASSANDRA_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Elasticsearch 7.
	 * @return a docker image name for running elasticsearch
	 */
	public static DockerImageName elasticsearch() {
		return DockerImageName.parse("bitnami/elasticsearch").withTag(ELASTICSEARCH_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running MariaDB.
	 * @return a docker image name for running MariaDB
	 */
	public static DockerImageName mariadb() {
		return DockerImageName.parse("bitnami/mariadb").withTag(MARIADB_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Mongo.
	 * @return a docker image name for running mongo
	 */
	public static DockerImageName mongo() {
		return DockerImageName.parse("bitnami/mongodb").withTag(MONGO_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running MySQL.
	 * @return a docker image name for running MySQL
	 */
	public static DockerImageName mysql() {
		return DockerImageName.parse("bitnami/mysql").withTag(MYSQL_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Neo4j.
	 * @return a docker image name for running neo4j
	 */
	public static DockerImageName neo4j() {
		return DockerImageName.parse("bitnami/neo4j").withTag(NEO4J_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running PostgreSQL.
	 * @return a docker image name for running postgresql
	 */
	public static DockerImageName postgresql() {
		return DockerImageName.parse("bitnami/postgresql").withTag(POSTGRESQL_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running RabbitMQ.
	 * @return a docker image name for running RabbitMQ
	 */
	public static DockerImageName rabbit() {
		return DockerImageName.parse("bitnami/rabbitmq").withTag(RABBIT_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Redis.
	 * @return a docker image name for running redis
	 */
	public static DockerImageName redis() {
		return DockerImageName.parse("bitnami/redis").withTag(REDIS_VERSION);
	}

}
