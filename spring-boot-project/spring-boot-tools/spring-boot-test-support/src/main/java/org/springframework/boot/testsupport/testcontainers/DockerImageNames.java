/*
 * Copyright 2012-2023 the original author or authors.
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
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Chris Bono
 */
public final class DockerImageNames {

	private static final String ACTIVE_MQ_VERSION = "5.18.0";

	private static final String CASSANDRA_VERSION = "3.11.10";

	private static final String COUCHBASE_VERSION = "7.1.4";

	private static final String ELASTICSEARCH_VERSION = "7.17.5";

	private static final String ELASTICSEARCH_8_VERSION = "8.6.1";

	private static final String KAFKA_VERSION = "7.4.0";

	private static final String MARIADB_VERSION = "10.10";

	private static final String MONGO_VERSION = "5.0.17";

	private static final String MYSQL_VERSION = "8.0";

	private static final String NEO4J_VERSION = "4.4.11";

	private static final String ORACLE_XE_VERSION = "18.4.0-slim";

	private static final String OPENTELEMETRY_VERSION = "0.75.0";

	private static final String PULSAR_VERSION = "3.1.0";

	private static final String POSTGRESQL_VERSION = "14.0";

	private static final String RABBIT_VERSION = "3.11-alpine";

	private static final String REDIS_VERSION = "7.0.11";

	private static final String REDPANDA_VERSION = "v23.1.2";

	private static final String REGISTRY_VERSION = "2.7.1";

	private static final String ZIPKIN_VERSION = "2.24.1";

	private DockerImageNames() {
	}

	/**
	 * Return a {@link DockerImageName} suitable for running ActiveMQ.
	 * @return a docker image name for running activeMq
	 */
	public static DockerImageName activeMq() {
		return DockerImageName.parse("symptoma/activemq").withTag(ACTIVE_MQ_VERSION);
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
	 * Return a {@link DockerImageName} suitable for running Elasticsearch 7.
	 * @return a docker image name for running elasticsearch
	 */
	public static DockerImageName elasticsearch() {
		return DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch").withTag(ELASTICSEARCH_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Elasticsearch 8.
	 * @return a docker image name for running elasticsearch
	 */
	public static DockerImageName elasticsearch8() {
		return DockerImageName.parse("elasticsearch").withTag(ELASTICSEARCH_8_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Kafka.
	 * @return a docker image name for running Kafka
	 */
	public static DockerImageName kafka() {
		return DockerImageName.parse("confluentinc/cp-kafka").withTag(KAFKA_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running MariaDB.
	 * @return a docker image name for running MariaDB
	 */
	public static DockerImageName mariadb() {
		return DockerImageName.parse("mariadb").withTag(MARIADB_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Mongo.
	 * @return a docker image name for running mongo
	 */
	public static DockerImageName mongo() {
		return DockerImageName.parse("mongo").withTag(MONGO_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running MySQL.
	 * @return a docker image name for running MySQL
	 */
	public static DockerImageName mysql() {
		return DockerImageName.parse("mysql").withTag(MYSQL_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Neo4j.
	 * @return a docker image name for running neo4j
	 */
	public static DockerImageName neo4j() {
		return DockerImageName.parse("neo4j").withTag(NEO4J_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running the Oracle database.
	 * @return a docker image name for running the Oracle database
	 */
	public static DockerImageName oracleXe() {
		return DockerImageName.parse("gvenzl/oracle-xe").withTag(ORACLE_XE_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running the Oracle database.
	 * @return a docker image name for running the Oracle database
	 */
	public static DockerImageName opentelemetry() {
		return DockerImageName.parse("otel/opentelemetry-collector-contrib").withTag(OPENTELEMETRY_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Apache Pulsar.
	 * @return a docker image name for running pulsar
	 */
	public static DockerImageName pulsar() {
		return DockerImageName.parse("apachepulsar/pulsar").withTag(PULSAR_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running PostgreSQL.
	 * @return a docker image name for running postgresql
	 */
	public static DockerImageName postgresql() {
		return DockerImageName.parse("postgres").withTag(POSTGRESQL_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running RabbitMQ.
	 * @return a docker image name for running RabbitMQ
	 */
	public static DockerImageName rabbit() {
		return DockerImageName.parse("rabbitmq").withTag(RABBIT_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Redis.
	 * @return a docker image name for running redis
	 */
	public static DockerImageName redis() {
		return DockerImageName.parse("redis").withTag(REDIS_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Redpanda.
	 * @return a docker image name for running redpanda
	 */
	public static DockerImageName redpanda() {
		return DockerImageName.parse("redpandadata/redpanda")
			.withTag(REDPANDA_VERSION)
			.asCompatibleSubstituteFor("docker.redpanda.com/redpandadata/redpanda");
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Microsoft SQLServer.
	 * @return a docker image name for running SQLServer
	 */
	public static DockerImageName sqlserver() {
		return DockerImageName.parse("mcr.microsoft.com/mssql/server");
	}

	/**
	 * Return a {@link DockerImageName} suitable for running a Docker registry.
	 * @return a docker image name for running a registry
	 */
	public static DockerImageName registry() {
		return DockerImageName.parse("registry").withTag(REGISTRY_VERSION);
	}

	/**
	 * Return a {@link DockerImageName} suitable for running Zipkin.
	 * @return a docker image name for running Zipkin
	 */
	public static DockerImageName zipkin() {
		return DockerImageName.parse("openzipkin/zipkin").withTag(ZIPKIN_VERSION);
	}

}
