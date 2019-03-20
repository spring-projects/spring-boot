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

import java.lang.reflect.Method;
import java.util.Properties;

import org.assertj.core.data.MapEntry;
import org.junit.Test;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

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
		this.properties.setMaxTimeout(1L);
		this.properties.setDefaultJtaTimeout(2L);
		this.properties.setMaxActives(3);
		this.properties.setEnableLogging(true);
		this.properties.setTransactionManagerUniqueName("uniqueName");
		this.properties.setSerialJtaTransactions(true);
		this.properties.setForceShutdownOnVmExit(true);
		this.properties.setLogBaseName("logBaseName");
		this.properties.setLogBaseDir("logBaseDir");
		this.properties.setCheckpointInterval(4);
		this.properties.setThreadedTwoPhaseCommit(true);

		assertThat(this.properties.asProperties().size()).isEqualTo(12);
		assertProperty("com.atomikos.icatch.service", "service");
		assertProperty("com.atomikos.icatch.max_timeout", "1");
		assertProperty("com.atomikos.icatch.default_jta_timeout", "2");
		assertProperty("com.atomikos.icatch.max_actives", "3");
		assertProperty("com.atomikos.icatch.enable_logging", "true");
		assertProperty("com.atomikos.icatch.tm_unique_name", "uniqueName");
		assertProperty("com.atomikos.icatch.serial_jta_transactions", "true");
		assertProperty("com.atomikos.icatch.force_shutdown_on_vm_exit", "true");
		assertProperty("com.atomikos.icatch.log_base_name", "logBaseName");
		assertProperty("com.atomikos.icatch.log_base_dir", "logBaseDir");
		assertProperty("com.atomikos.icatch.checkpoint_interval", "4");
		assertProperty("com.atomikos.icatch.threaded_2pc", "true");
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
				"com.atomikos.icatch.force_shutdown_on_vm_exit",
				"com.atomikos.icatch.log_base_name",
				"com.atomikos.icatch.checkpoint_interval",
				"com.atomikos.icatch.threaded_2pc"));
		assertThat(properties).hasSize(9);
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
			Class<?> target = ClassUtils.forName(
					"com.atomikos.icatch.standalone.UserTransactionServiceImp",
					getClass().getClassLoader());
			Method m = target.getMethod("getDefaultProperties");
			m.setAccessible(true);
			return (Properties) ReflectionUtils.invokeMethod(m, null);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to get default from Atomikos", ex);
		}
	}

	private void assertProperty(String key, String value) {
		assertThat(this.properties.asProperties().getProperty(key)).isEqualTo(value);
	}

}
