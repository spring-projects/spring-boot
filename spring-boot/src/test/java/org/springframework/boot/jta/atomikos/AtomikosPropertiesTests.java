/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jta.atomikos;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for ;@link AtomikosProperties}.
 *
 * @author Phillip Webb
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
		this.properties.setConsoleLogLevel(AtomikosLoggingLevel.WARN);
		this.properties.setOutputDir("outputDir");
		this.properties.setConsoleFileName("consoleFileName");
		this.properties.setConsoleFileCount(5);
		this.properties.setConsoleFileLimit(6);
		this.properties.setThreadedTwoPhaseCommit(true);

		assertThat(this.properties.asProperties().size(), equalTo(17));
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
		assertProperty("com.atomikos.icatch.console_log_level", "WARN");
		assertProperty("com.atomikos.icatch.output_dir", "outputDir");
		assertProperty("com.atomikos.icatch.console_file_name", "consoleFileName");
		assertProperty("com.atomikos.icatch.console_file_count", "5");
		assertProperty("com.atomikos.icatch.console_file_limit", "6");
		assertProperty("com.atomikos.icatch.threaded_2pc", "true");
	}

	private void assertProperty(String key, String value) {
		assertThat(this.properties.asProperties().getProperty(key), equalTo(value));
	}

}
