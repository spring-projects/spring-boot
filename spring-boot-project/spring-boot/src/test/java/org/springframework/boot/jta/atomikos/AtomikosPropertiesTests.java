/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.jta.atomikos;

import java.time.Duration;
import java.util.Properties;

import org.assertj.core.data.MapEntry;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link AtomikosProperties}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class AtomikosPropertiesTests {

	private AtomikosProperties properties = new AtomikosProperties();

	@Test
	public void testProperties() {
		this.properties.setService("service");
		this.properties.setMaxTimeout(Duration.ofMillis(1));
		this.properties.setDefaultJtaTimeout(Duration.ofMillis(2));
		this.properties.setMaxActives(3);
		this.properties.setEnableLogging(true);
		this.properties.setTransactionManagerUniqueName("uniqueName");
		this.properties.setSerialJtaTransactions(true);
		this.properties.setAllowSubTransactions(false);
		this.properties.setForceShutdownOnVmExit(true);
		this.properties.setDefaultMaxWaitTimeOnShutdown(20);
		this.properties.setLogBaseName("logBaseName");
		this.properties.setLogBaseDir("logBaseDir");
		this.properties.setCheckpointInterval(4);
		this.properties.setThreadedTwoPhaseCommit(true);
		this.properties.getRecovery()
				.setForgetOrphanedLogEntriesDelay(Duration.ofMillis(2000));
		this.properties.getRecovery().setDelay(Duration.ofMillis(3000));
		this.properties.getRecovery().setMaxRetries(10);
		this.properties.getRecovery().setRetryInterval(Duration.ofMillis(4000));
		assertThat(this.properties.asProperties().size()).isEqualTo(18);
		assertProperty("com.atomikos.icatch.service", "service");
		assertProperty("com.atomikos.icatch.max_timeout", "1");
		assertProperty("com.atomikos.icatch.default_jta_timeout", "2");
		assertProperty("com.atomikos.icatch.max_actives", "3");
		assertProperty("com.atomikos.icatch.enable_logging", "true");
		assertProperty("com.atomikos.icatch.tm_unique_name", "uniqueName");
		assertProperty("com.atomikos.icatch.serial_jta_transactions", "true");
		assertProperty("com.atomikos.icatch.allow_subtransactions", "false");
		assertProperty("com.atomikos.icatch.force_shutdown_on_vm_exit", "true");
		assertProperty("com.atomikos.icatch.default_max_wait_time_on_shutdown", "20");
		assertProperty("com.atomikos.icatch.log_base_name", "logBaseName");
		assertProperty("com.atomikos.icatch.log_base_dir", "logBaseDir");
		assertProperty("com.atomikos.icatch.checkpoint_interval", "4");
		assertProperty("com.atomikos.icatch.threaded_2pc", "true");
		assertProperty("com.atomikos.icatch.forget_orphaned_log_entries_delay", "2000");
		assertProperty("com.atomikos.icatch.recovery_delay", "3000");
		assertProperty("com.atomikos.icatch.oltp_max_retries", "10");
		assertProperty("com.atomikos.icatch.oltp_retry_interval", "4000");
	}

	@Test
	public void testDefaultProperties() {
		Properties defaultSettings = loadDefaultSettings();
		Properties properties = this.properties.asProperties();
		assertThat(properties).contains(defaultOf(defaultSettings,
				"com.atomikos.icatch.max_timeout",
				"com.atomikos.icatch.default_jta_timeout",
				"com.atomikos.icatch.max_actives", "com.atomikos.icatch.enable_logging",
				"com.atomikos.icatch.serial_jta_transactions",
				"com.atomikos.icatch.allow_subtransactions",
				"com.atomikos.icatch.force_shutdown_on_vm_exit",
				"com.atomikos.icatch.default_max_wait_time_on_shutdown",
				"com.atomikos.icatch.log_base_name",
				"com.atomikos.icatch.checkpoint_interval",
				"com.atomikos.icatch.threaded_2pc",
				"com.atomikos.icatch.forget_orphaned_log_entries_delay",
				"com.atomikos.icatch.oltp_max_retries",
				"com.atomikos.icatch.oltp_retry_interval"));
		assertThat(properties).contains(entry("com.atomikos.icatch.recovery_delay",
				defaultSettings.get("com.atomikos.icatch.default_jta_timeout")));
		assertThat(properties).hasSize(15);
	}

	private MapEntry<?, ?>[] defaultOf(Properties defaultSettings, String... keys) {
		MapEntry<?, ?>[] entries = new MapEntry[keys.length];
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			entries[i] = entry(key, defaultSettings.get(key));
		}
		return entries;
	}

	private Properties loadDefaultSettings() {
		try {

			return PropertiesLoaderUtils.loadProperties(
					new ClassPathResource("transactions-defaults.properties"));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to get default from Atomikos", ex);
		}
	}

	private void assertProperty(String key, String value) {
		assertThat(this.properties.asProperties().getProperty(key)).isEqualTo(value);
	}

}
