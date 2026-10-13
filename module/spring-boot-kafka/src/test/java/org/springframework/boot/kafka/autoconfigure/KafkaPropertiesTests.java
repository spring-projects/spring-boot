/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Admin;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Cleanup;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.IsolationLevel;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Listener;
import org.springframework.kafka.core.CleanupConfig;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ContainerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaProperties}.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class KafkaPropertiesTests {

	@Test
	void isolationLevelEnumConsistentWithKafkaVersion() {
		org.apache.kafka.common.IsolationLevel[] original = org.apache.kafka.common.IsolationLevel.values();
		assertThat(original).extracting(Enum::name)
			.containsExactly(IsolationLevel.READ_UNCOMMITTED.name(), IsolationLevel.READ_COMMITTED.name());
		assertThat(original).extracting("id")
			.containsExactly(IsolationLevel.READ_UNCOMMITTED.id(), IsolationLevel.READ_COMMITTED.id());
		assertThat(original).hasSameSizeAs(IsolationLevel.values());
	}

	@Test
	void adminDefaultValuesAreConsistent() {
		KafkaAdmin admin = new KafkaAdmin(Collections.emptyMap());
		Admin adminProperties = new KafkaProperties().getAdmin();
		assertThat(admin).hasFieldOrPropertyWithValue("fatalIfBrokerNotAvailable", adminProperties.isFailFast());
		assertThat(admin).hasFieldOrPropertyWithValue("modifyTopicConfigs", adminProperties.isModifyTopicConfigs());
	}

	@Test
	void listenerDefaultValuesAreConsistent() {
		ContainerProperties container = new ContainerProperties("test");
		Listener listenerProperties = new KafkaProperties().getListener();
		assertThat(listenerProperties.isMissingTopicsFatal()).isEqualTo(container.isMissingTopicsFatal());
	}

	@Test
	void cleanupConfigDefaultValuesAreConsistent() {
		CleanupConfig cleanupConfig = new CleanupConfig();
		Cleanup cleanup = new KafkaProperties().getStreams().getCleanup();
		assertThat(cleanup.isOnStartup()).isEqualTo(cleanupConfig.cleanupOnStart());
		assertThat(cleanup.isOnShutdown()).isEqualTo(cleanupConfig.cleanupOnStop());
	}

}
