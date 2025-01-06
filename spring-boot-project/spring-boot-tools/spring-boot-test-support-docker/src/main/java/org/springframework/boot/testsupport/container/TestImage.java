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

package org.springframework.boot.testsupport.container;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisStackContainer;
import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.util.Assert;

/**
 * References to container images used for integration tests. This class also acts a
 * central location for tests to {@link #container(Class) create} a correctly configured
 * {@link Container testcontainer}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Chris Bono
 * @author Phillip Webb
 */
public enum TestImage {

	/**
	 * A container image suitable for testing ActiveMQ.
	 */
	ACTIVE_MQ("symptoma/activemq", "5.18.3", () -> SymptomaActiveMQContainer.class),

	/**
	 * A container image suitable for testing ActiveMQ classic.
	 */
	ACTIVE_MQ_CLASSIC("apache/activemq-classic", "5.18.3", () -> ActiveMQContainer.class),

	/**
	 * A container image suitable for testing Apache Kafka.
	 */
	APACHE_KAFKA("apache/kafka", "3.7.0", () -> org.testcontainers.kafka.KafkaContainer.class),

	/**
	 * A container image suitable for testing Artemis.
	 */
	ARTEMIS("apache/activemq-artemis", "2.34.0", () -> ArtemisContainer.class),

	/**
	 * A container image suitable for testing Cassandra.
	 */
	CASSANDRA("cassandra", "3.11.10", () -> CassandraContainer.class,
			(container) -> ((CassandraContainer) container).withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing Cassandra using the deprecated
	 * {@link org.testcontainers.containers.CassandraContainer}.
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of {@link #CASSANDRA}
	 */
	@SuppressWarnings("deprecation")
	@Deprecated(since = "3.4.0", forRemoval = true)
	CASSANDRA_DEPRECATED("cassandra", "3.11.10", () -> org.testcontainers.containers.CassandraContainer.class,
			(container) -> ((org.testcontainers.containers.CassandraContainer<?>) container)
				.withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing ClickHouse.
	 */
	CLICKHOUSE("clickhouse/clickhouse-server", "24.3"),

	/**
	 * A container image suitable for testing Couchbase.
	 */
	COUCHBASE("couchbase/server", "7.1.4", () -> CouchbaseContainer.class,
			(container) -> ((CouchbaseContainer) container).withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing Elasticsearch 7.
	 */
	ELASTICSEARCH("docker.elastic.co/elasticsearch/elasticsearch", "7.17.5", () -> ElasticsearchContainer.class,
			(container) -> ((ElasticsearchContainer) container).withEnv("ES_JAVA_OPTS", "-Xms32m -Xmx512m")
				.withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing Elasticsearch 8.
	 */
	ELASTICSEARCH_8("elasticsearch", "8.6.1"),

	/**
	 * A container image suitable for testing Grafana OTel LGTM.
	 */
	GRAFANA_OTEL_LGTM("grafana/otel-lgtm", "0.6.0", () -> LgtmStackContainer.class,
			(container) -> ((LgtmStackContainer) container).withStartupTimeout(Duration.ofMinutes(2))),

	/**
	 * A container image suitable for testing Hazelcast.
	 */
	HAZELCAST("hazelcast/hazelcast", "5.5.0-slim", () -> HazelcastContainer.class),

	/**
	 * A container image suitable for testing Confluent's distribution of Kafka.
	 */
	CONFLUENT_KAFKA("confluentinc/cp-kafka", "7.4.0", () -> ConfluentKafkaContainer.class),

	/**
	 * A container image suitable for testing Confluent's distribution of Kafka using the
	 * deprecated {@link org.testcontainers.containers.KafkaContainer}.
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of {@link #CONFLUENT_KAFKA}
	 */
	@SuppressWarnings("deprecation")
	@Deprecated(since = "3.4.0", forRemoval = true)
	CONFLUENT_KAFKA_DEPRECATED("confluentinc/cp-kafka", "7.4.0",
			() -> org.testcontainers.containers.KafkaContainer.class),

	/**
	 * A container image suitable for testing OpenLDAP.
	 */
	OPEN_LDAP("osixia/openldap", "1.5.0", () -> OpenLdapContainer.class),

	/**
	 * A container image suitable for testing SMTP.
	 */
	MAILPIT("axllent/mailpit", "v1.19.0", () -> MailpitContainer.class),

	/**
	 * A container image suitable for testing MariaDB.
	 */
	MARIADB("mariadb", "10.10"),

	/**
	 * A container image suitable for testing MongoDB.
	 */
	MONGODB("mongo", "5.0.17", () -> MongoDBContainer.class,
			(container) -> ((MongoDBContainer) container).withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(5))),

	/**
	 * A container image suitable for testing MySQL.
	 */
	MYSQL("mysql", "8.0"),

	/**
	 * A container image suitable for testing Neo4j.
	 */
	NEO4J("neo4j", "4.4.11", () -> Neo4jContainer.class,
			(container) -> ((Neo4jContainer<?>) container).withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing Oracle Free.
	 */
	ORACLE_FREE("gvenzl/oracle-free", "23.3-slim", () -> org.testcontainers.oracle.OracleContainer.class,
			(container) -> ((org.testcontainers.oracle.OracleContainer) container)
				.withStartupTimeout(Duration.ofMinutes(2))),

	/**
	 * A container image suitable for testing Oracle XA.
	 */
	ORACLE_XE("gvenzl/oracle-xe", "18.4.0-slim", () -> org.testcontainers.containers.OracleContainer.class,
			(container) -> ((org.testcontainers.containers.OracleContainer) container)
				.withStartupTimeout(Duration.ofMinutes(2))),

	/**
	 * A container image suitable for testing Opentelemetry.
	 */
	OPENTELEMETRY("otel/opentelemetry-collector-contrib", "0.75.0"),

	/**
	 * A container image suitable for testing Postgres.
	 */
	POSTGRESQL("postgres", "14.0", () -> PostgreSQLContainer.class),

	/**
	 * A container image suitable for testing Pulsar.
	 */
	PULSAR("apachepulsar/pulsar", "3.3.3", () -> PulsarContainer.class,
			(container) -> ((PulsarContainer) container).withStartupAttempts(2)
				.withStartupTimeout(Duration.ofMinutes(3))),

	/**
	 * A container image suitable for testing RabbitMQ.
	 */
	RABBITMQ("rabbitmq", "3.11-alpine", () -> RabbitMQContainer.class,
			(container) -> ((RabbitMQContainer) container).withStartupTimeout(Duration.ofMinutes(4))),

	/**
	 * A container image suitable for testing Redis.
	 */
	REDIS("redis", "7.0.11", () -> RedisContainer.class,
			(container) -> ((RedisContainer) container).withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing Redis Stack.
	 */
	REDIS_STACK("redis/redis-stack", "7.2.0-v11", () -> RedisStackContainer.class,
			(container) -> ((RedisStackContainer) container).withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing Redis Stack Server.
	 */
	REDIS_STACK_SERVER("redis/redis-stack-server", "7.2.0-v11", () -> RedisStackServerContainer.class,
			(container) -> ((RedisStackServerContainer) container).withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(10))),

	/**
	 * A container image suitable for testing Redpanda.
	 */
	REDPANDA("redpandadata/redpanda", "v23.1.2", () -> RedpandaContainer.class,
			(container) -> ((RedpandaContainer) container).withStartupTimeout(Duration.ofMinutes(5))),

	/**
	 * A container image suitable for testing Docker Registry.
	 */
	REGISTRY("registry", "2.7.1", () -> RegistryContainer.class,
			(container) -> ((RegistryContainer) container).withStartupAttempts(5)
				.withStartupTimeout(Duration.ofMinutes(3))),

	/**
	 * A container image suitable for testing MS SQL Server.
	 */
	SQL_SERVER("mcr.microsoft.com/mssql/server"),

	/**
	 * A container image suitable for testing Zipkin.
	 */
	ZIPKIN("openzipkin/zipkin", "3.0.6", () -> ZipkinContainer.class),

	/**
	 * A container image suitable for testing Cassandra via Bitnami.
	 */
	BITNAMI_CASSANDRA("bitnami/cassandra", "4.1.3"),

	/**
	 * A container image suitable for testing ClickHouse via Bitnami.
	 */
	BITNAMI_CLICKHOUSE("bitnami/clickhouse", "24.3"),

	/**
	 * A container image suitable for testing Elasticsearch via Bitnami.
	 */
	BITNAMI_ELASTICSEARCH("bitnami/elasticsearch", "8.12.1"),

	/**
	 * A container image suitable for testing MariaDB via Bitnami.
	 */
	BITNAMI_MARIADB("bitnami/mariadb", "11.2.3"),

	/**
	 * A container image suitable for testing MongoDB via Bitnami.
	 */
	BITNAMI_MONGODB("bitnami/mongodb", "7.0.5"),

	/**
	 * A container image suitable for testing MySQL via Bitnami.
	 */
	BITNAMI_MYSQL("bitnami/mysql", "8.0.36"),

	/**
	 * A container image suitable for testing Neo4j via Bitnami.
	 */
	BITNAMI_NEO4J("bitnami/neo4j", "5.16.0"),

	/**
	 * A container image suitable for testing Postgres via Bitnami.
	 */
	BITNAMI_POSTGRESQL("bitnami/postgresql", "16.2.0"),

	/**
	 * A container image suitable for testing RabbitMQ via Bitnami.
	 */
	BITNAMI_RABBITMQ("bitnami/rabbitmq", "3.11.28"),

	/**
	 * A container image suitable for testing Redis via Bitnami.
	 */
	BITNAMI_REDIS("bitnami/redis", "7.2.4");

	private final String name;

	private final String tag;

	private final Class<?> containerClass;

	private final Consumer<?> containerSetup;

	TestImage(String name) {
		this(name, null);
	}

	TestImage(String name, String tag) {
		this(name, tag, null, null);
	}

	TestImage(String name, String tag, Supplier<Class<?>> containerClass) {
		this(name, tag, containerClass, null);
	}

	TestImage(String name, String tag, Consumer<?> containerSetup) {
		this(name, tag, null, containerSetup);
	}

	TestImage(String name, String tag, Supplier<Class<?>> containerClass, Consumer<?> containerSetup) {
		this.name = name;
		this.tag = tag;
		this.containerClass = getIfPossible(containerClass);
		this.containerSetup = containerSetup;
	}

	static Class<?> getIfPossible(Supplier<Class<?>> supplier) {
		try {
			return (supplier != null) ? supplier.get() : null;
		}
		catch (NoClassDefFoundError ex) {
			return null;
		}
	}

	private boolean matchesContainerClass(Class<?> containerClass) {
		return this.containerClass != null && this.containerClass.isAssignableFrom(containerClass);
	}

	/**
	 * Create a {@link GenericContainer} for the given {@link TestImage}.
	 * @return a generic container for the test image
	 */
	public GenericContainer<?> genericContainer() {
		return createContainer(GenericContainer.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <C extends Container<?>> C createContainer(Class<C> containerClass) {
		DockerImageName dockerImageName = DockerImageName.parse(toString());
		try {
			Constructor<C> constructor = containerClass.getDeclaredConstructor(DockerImageName.class);
			constructor.setAccessible(true);
			C container = constructor.newInstance(dockerImageName);
			if (this.containerSetup != null) {
				((Consumer) this.containerSetup).accept(container);
			}
			return container;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create container " + containerClass, ex);
		}
	}

	public String getTag() {
		return this.tag;
	}

	@Override
	public String toString() {
		return (this.tag != null) ? this.name + ":" + this.tag : this.name;
	}

	/**
	 * Factory method to create and configure a {@link Container} using a deduced
	 * {@link TestImage}.
	 * @param <C> the container type
	 * @param containerClass the container type
	 * @return a container instance
	 */
	public static <C extends Container<?>> C container(Class<C> containerClass) {
		return forContainerClass(containerClass).createContainer(containerClass);
	}

	private static TestImage forContainerClass(Class<?> containerClass) {
		List<TestImage> images = Arrays.stream(values())
			.filter((image) -> image.matchesContainerClass(containerClass))
			.toList();
		Assert.state(!images.isEmpty(), () -> "Unknown container class " + containerClass);
		Assert.state(images.size() == 1, () -> "Multiple test images match container class " + containerClass);
		return images.get(0);
	}

}
