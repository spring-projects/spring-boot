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

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Bean friendly variant of <a
 * href="http://www.atomikos.com/Documentation/JtaProperties">Atomikos configuration
 * properties</a>. Allows for setter based configuration and is amiable to relaxed data
 * binding.
 *
 * @author Phillip Webb
 * @see #asProperties()
 * @since 1.2.0
 */
public class AtomikosProperties {

	private final Map<String, String> values = new TreeMap<String, String>();

	/**
	 * Specifies the transaction manager implementation that should be started. There is
	 * no default value and this must be set. Generally,
	 * {@literal com.atomikos.icatch.standalone.UserTransactionServiceFactory} is the
	 * value you should set.
	 * @param service the service
	 */
	public void setService(String service) {
		set("service", service);
	}

	/**
	 * Specifies the maximum timeout (in milliseconds) that can be allowed for
	 * transactions. Defaults to {@literal 300000}. This means that calls to
	 * UserTransaction.setTransactionTimeout() with a value higher than configured here
	 * will be max'ed to this value.
	 * @param maxTimeout the max timeout
	 */
	public void setMaxTimeout(long maxTimeout) {
		set("max_timeout", maxTimeout);
	}

	/**
	 * The default timeout for JTA transactions (optional, defaults to {@literal 10000}
	 * ms).
	 * @param defaultJtaTimeout the default JTA timeout
	 */
	public void setDefaultJtaTimeout(long defaultJtaTimeout) {
		set("default_jta_timeout", defaultJtaTimeout);
	}

	/**
	 * Specifies the maximum number of active transactions. Defaults to {@literal 50}. A
	 * negative value means infinite amount. You will get an {@code IllegalStateException}
	 * with error message "Max number of active transactions reached" if you call
	 * {@code UserTransaction.begin()} while there are already n concurrent transactions
	 * running, n being this value.
	 * @param maxActivities the max activities
	 */
	public void setMaxActives(int maxActivities) {
		set("max_actives", maxActivities);
	}

	/**
	 * Specifies if disk logging should be enabled or not. Defaults to true. It is useful
	 * for JUnit testing, or to profile code without seeing the transaction manager's
	 * activity as a hot spot but this should never be disabled on production or data
	 * integrity cannot be guaranteed.
	 * @param enableLogging if logging is enabled
	 */
	public void setEnableLogging(boolean enableLogging) {
		set("enable_logging", enableLogging);
	}

	/**
	 * Specifies the transaction manager's unique name. Defaults to the machine's IP
	 * address. If you plan to run more than one transaction manager against one database
	 * you must set this property to a unique value or you might run into duplicate
	 * transaction ID (XID) problems that can be quite subtle (example:
	 * {@literal http://fogbugz.atomikos.com/default.asp?community.6.2225.7}). If multiple
	 * instances need to use the same properties file then the easiest way to ensure
	 * uniqueness for this property is by referencing a system property specified at VM
	 * startup.
	 * @param uniqueName the unique name
	 */
	public void setTransactionManagerUniqueName(String uniqueName) {
		set("tm_unique_name", uniqueName);
	}

	/**
	 * Specifies if subtransactions should be joined when possible. Defaults to true. When
	 * false, no attempt to call {@code XAResource.start(TM_JOIN)} will be made for
	 * different but related subtransctions. This setting has no effect on resource access
	 * within one and the same transaction. If you don't use subtransactions then this
	 * setting can be ignored.
	 * @param serialJtaTransactions if serial JTA transaction are supported
	 */
	public void setSerialJtaTransactions(boolean serialJtaTransactions) {
		set("serial_jta_transactions", serialJtaTransactions);
	}

	/**
	 * Specifies whether VM shutdown should trigger forced shutdown of the transaction
	 * core. Defaults to false.
	 * @param forceShutdownOnVmExit
	 */
	public void setForceShutdownOnVmExit(boolean forceShutdownOnVmExit) {
		set("force_shutdown_on_vm_exit", forceShutdownOnVmExit);
	}

	/**
	 * Specifies the transactions log file base name. Defaults to {@literal tmlog}. The
	 * transactions logs are stored in files using this name appended with a number and
	 * the extension {@literal .log}. At checkpoint, a new transactions log file is
	 * created and the number is incremented.
	 * @param logBaseName the log base name
	 */
	public void setLogBaseName(String logBaseName) {
		set("log_base_name", logBaseName);
	}

	/**
	 * Specifies the directory in which the log files should be stored. Defaults to the
	 * current working directory. This directory should be a stable storage like a SAN,
	 * RAID or at least backed up location. The transactions logs files are as important
	 * as the data themselves to guarantee consistency in case of failures.
	 * @param logBaseDir the log base dir
	 */
	public void setLogBaseDir(String logBaseDir) {
		set("log_base_dir", logBaseDir);
	}

	/**
	 * Specifies the interval between checkpoints. A checkpoint reduces the log file size
	 * at the expense of adding some overhead in the runtime. Defaults to {@literal 500}.
	 * @param checkpointInterval the checkpoint interval
	 */
	public void setCheckpointInterval(long checkpointInterval) {
		set("checkpoint_interval", checkpointInterval);
	}

	/**
	 * Specifies the console log level. Defaults to {@link AtomikosLoggingLevel#WARN}.
	 * @param consoleLogLevel the console log level
	 */
	public void setConsoleLogLevel(AtomikosLoggingLevel consoleLogLevel) {
		set("console_log_level", consoleLogLevel);
	}

	/**
	 * Specifies the directory in which to store the debug log files. Defaults to the
	 * current working directory.
	 * @param outputDir the output dir
	 */
	public void setOutputDir(String outputDir) {
		set("output_dir", outputDir);
	}

	/**
	 * Specifies the debug logs file name. Defaults to {@literal tm.out}.
	 * @param consoleFileName the console file name
	 */
	public void setConsoleFileName(String consoleFileName) {
		set("console_file_name", consoleFileName);
	}

	/**
	 * Specifies how many debug logs files can be created. Defaults to {@literal 1}.
	 * @param consoleFileCount the console file count
	 */
	public void setConsoleFileCount(int consoleFileCount) {
		set("console_file_count", consoleFileCount);
	}

	/**
	 * Specifies how many bytes can be stored at most in debug logs files. Defaults to
	 * {@literal -1}. Negative values means unlimited.
	 * @param consoleFileLimit the console file limit
	 */
	public void setConsoleFileLimit(int consoleFileLimit) {
		set("console_file_limit", consoleFileLimit);
	}

	/**
	 * Specifies whether or not to use different (and concurrent) threads for two-phase
	 * commit on the participating resources. Setting this to {@literal true} implies that
	 * the commit is more efficient since waiting for acknowledgements is done in
	 * parallel. Defaults to {@literal true}. If you set this to {@literal false}, then
	 * commits will happen in the order that resources are accessed within the
	 * transaction.
	 * @param threadedTwoPhaseCommit if threaded two phase commits should be used
	 */
	public void setThreadedTwoPhaseCommit(boolean threadedTwoPhaseCommit) {
		set("threaded_2pc", threadedTwoPhaseCommit);
	}

	private void set(String key, Object value) {
		set("com.atomikos.icatch.", key, value);
	}

	private void set(String keyPrefix, String key, Object value) {
		if (value != null) {
			this.values.put(keyPrefix + key, value.toString());
		}
		else {
			this.values.remove(keyPrefix + key);
		}
	}

	/**
	 * Returns the properties as a {@link Properties} object that can be used with
	 * Atomikos.
	 * @return the properties
	 */
	public Properties asProperties() {
		Properties properties = new Properties();
		properties.putAll(this.values);
		return properties;
	}

}
