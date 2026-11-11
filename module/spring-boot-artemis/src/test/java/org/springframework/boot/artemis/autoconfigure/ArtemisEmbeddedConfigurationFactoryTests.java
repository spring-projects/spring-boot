/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.autoconfigure;

import java.util.List;
import java.util.Map;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArtemisEmbeddedConfigurationFactory}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ArtemisEmbeddedConfigurationFactoryTests {

	@Test
	void defaultDataDir() {
		ArtemisProperties properties = new ArtemisProperties();
		properties.getEmbedded().setPersistent(true);
		Configuration configuration = new ArtemisEmbeddedConfigurationFactory(properties).createConfiguration();
		assertThat(configuration.getJournalDirectory()).startsWith(System.getProperty("java.io.tmpdir"))
			.endsWith("/journal");
	}

	@Test
	void persistenceSetup() {
		ArtemisProperties properties = new ArtemisProperties();
		properties.getEmbedded().setPersistent(true);
		Configuration configuration = new ArtemisEmbeddedConfigurationFactory(properties).createConfiguration();
		assertThat(configuration.isPersistenceEnabled()).isTrue();
		assertThat(configuration.getJournalType()).isEqualTo(JournalType.NIO);
	}

	@Test
	void generatedClusterPassword() {
		ArtemisProperties properties = new ArtemisProperties();
		Configuration configuration = new ArtemisEmbeddedConfigurationFactory(properties).createConfiguration();
		assertThat(configuration.getClusterPassword()).hasSize(36);
	}

	@Test
	void specificClusterPassword() {
		ArtemisProperties properties = new ArtemisProperties();
		properties.getEmbedded().setClusterPassword("password");
		Configuration configuration = new ArtemisEmbeddedConfigurationFactory(properties).createConfiguration();
		assertThat(configuration.getClusterPassword()).isEqualTo("password");
	}

	@Test
	void hasDlqExpiryQueueAddressSettingsConfigured() {
		ArtemisProperties properties = new ArtemisProperties();
		Configuration configuration = new ArtemisEmbeddedConfigurationFactory(properties).createConfiguration();
		Map<String, AddressSettings> addressSettings = configuration.getAddressSettings();
		AddressSettings sharp = addressSettings.get("#");
		assertThat(sharp).isNotNull();
		assertThat((Object) sharp.getDeadLetterAddress()).isEqualTo(SimpleString.of("DLQ"));
		assertThat((Object) sharp.getExpiryAddress()).isEqualTo(SimpleString.of("ExpiryQueue"));
	}

	@Test
	void hasDlqExpiryQueueConfigured() {
		ArtemisProperties properties = new ArtemisProperties();
		Configuration configuration = new ArtemisEmbeddedConfigurationFactory(properties).createConfiguration();
		List<CoreAddressConfiguration> addressConfigurations = configuration.getAddressConfigurations();
		assertThat(addressConfigurations).hasSize(2);
	}

}
