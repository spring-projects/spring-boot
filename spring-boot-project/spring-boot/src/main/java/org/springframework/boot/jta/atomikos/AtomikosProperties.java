/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean friendly variant of
 * <a href="http://www.atomikos.com/Documentation/JtaProperties">Atomikos configuration
 * properties</a>. Allows for setter based configuration and is amiable to relaxed data
 * binding.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.2.0
 * @see #asProperties()
 */
@ConfigurationProperties(prefix = "spring.jta.atomikos.properties")
public class AtomikosProperties {

	/**
	 * Transaction manager implementation that should be started.
	 */
	private String service;

	/**
	 * Maximum timeout that can be allowed for transactions.
	 */
	private Duration maxTimeout = Duration.ofMillis(300000);

	/**
	 * Default timeout for JTA transactions.
	 */
	private Duration defaultJtaTimeout = Duration.ofMillis(10000);

	/**
	 * Maximum number of active transactions.
	 */
	private int maxActives = 50;

	/**
	 * Whether to enable disk logging.
	 */
	private boolean enableLogging = true;

	/**
	 * The transaction manager's unique name. Defaults to the machine's IP address. If you
	 * plan to run more than one transaction manager against one database you must set
	 * this property to a unique value.
	 */
	private String transactionManagerUniqueName;

	/**
	 * Whether sub-transactions should be joined when possible.
	 */
	private boolean serialJtaTransactions = true;

	/**
	 * Specify whether sub-transactions are allowed.
	 */
	private boolean allowSubTransactions = true;

	/**
	 * Whether a VM shutdown should trigger forced shutdown of the transaction core.
	 */
	private boolean forceShutdownOnVmExit;

	/**
	 * How long should normal shutdown (no-force) wait for transactions to complete.
	 */
	private long defaultMaxWaitTimeOnShutdown = Long.MAX_VALUE;

	/**
	 * Transactions log file base name.
	 */
	private String logBaseName = "tmlog";

	/**
	 * Directory in which the log files should be stored. Defaults to the current working
	 * directory.
	 */
	private String logBaseDir;

	/**
	 * Interval between checkpoints, expressed as the number of log writes between two
	 * checkpoints. A checkpoint reduces the log file size at the expense of adding some
	 * overhead in the runtime.
	 */
	private long checkpointInterval = 500;

	/**
	 * Whether to use different (and concurrent) threads for two-phase commit on the
	 * participating resources.
	 */
	private boolean threadedTwoPhaseCommit;

	private final Recovery recovery = new Recovery();

	/**
	 * Specifies the transaction manager implementation that should be started. There is
	 * no default value and this must be set. Generally,
	 * {@literal com.atomikos.icatch.standalone.UserTransactionServiceFactory} is the
	 * value you should set.
	 * @param service the service
	 */
	public void setService(String service) {
		this.service = service;
	}

	public String getService() {
		return this.service;
	}

	/**
	 * Specifies the maximum timeout that can be allowed for transactions. Defaults to
	 * {@literal 300000}. This means that calls to UserTransaction.setTransactionTimeout()
	 * with a value higher than configured here will be max'ed to this value.
	 * @param maxTimeout the max timeout
	 */
	public void setMaxTimeout(Duration maxTimeout) {
		this.maxTimeout = maxTimeout;
	}

	public Duration getMaxTimeout() {
		return this.maxTimeout;
	}

	/**
	 * The default timeout for JTA transactions (optional, defaults to {@literal 10000}
	 * ms).
	 * @param defaultJtaTimeout the default JTA timeout
	 */
	public void setDefaultJtaTimeout(Duration defaultJtaTimeout) {
		this.defaultJtaTimeout = defaultJtaTimeout;
	}

	public Duration getDefaultJtaTimeout() {
		return this.defaultJtaTimeout;
	}

	/**
	 * Specifies the maximum number of active transactions. Defaults to {@literal 50}. A
	 * negative value means infinite amount. You will get an {@code IllegalStateException}
	 * with error message "Max number of active transactions reached" if you call
	 * {@code UserTransaction.begin()} while there are already n concurrent transactions
	 * running, n being this value.
	 * @param maxActives the max activities
	 */
	public void setMaxActives(int maxActives) {
		this.maxActives = maxActives;
	}

	public int getMaxActives() {
		return this.maxActives;
	}

	/**
	 * Specifies if disk logging should be enabled or not. Defaults to true. It is useful
	 * for JUnit testing, or to profile code without seeing the transaction manager's
	 * activity as a hot spot but this should never be disabled on production or data
	 * integrity cannot be guaranteed.
	 * @param enableLogging if logging is enabled
	 */
	public void setEnableLogging(boolean enableLogging) {
		this.enableLogging = enableLogging;
	}

	public boolean isEnableLogging() {
		return this.enableLogging;
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
		this.transactionManagerUniqueName = uniqueName;
	}

	public String getTransactionManagerUniqueName() {
		return this.transactionManagerUniqueName;
	}

	/**
	 * Specifies if subtransactions should be joined when possible. Defaults to true. When
	 * false, no attempt to call {@code XAResource.start(TM_JOIN)} will be made for
	 * different but related subtransactions. This setting has no effect on resource
	 * access within one and the same transaction. If you don't use subtransactions then
	 * this setting can be ignored.
	 * @param serialJtaTransactions if serial JTA transactions are supported
	 */
	public void setSerialJtaTransactions(boolean serialJtaTransactions) {
		this.serialJtaTransactions = serialJtaTransactions;
	}

	public boolean isSerialJtaTransactions() {
		return this.serialJtaTransactions;
	}

	public void setAllowSubTransactions(boolean allowSubTransactions) {
		this.allowSubTransactions = allowSubTransactions;
	}

	public boolean isAllowSubTransactions() {
		return this.allowSubTransactions;
	}

	/**
	 * Specifies whether VM shutdown should trigger forced shutdown of the transaction
	 * core. Defaults to false.
	 * @param forceShutdownOnVmExit if VM shutdown should be forced
	 */
	public void setForceShutdownOnVmExit(boolean forceShutdownOnVmExit) {
		this.forceShutdownOnVmExit = forceShutdownOnVmExit;
	}

	public boolean isForceShutdownOnVmExit() {
		return this.forceShutdownOnVmExit;
	}

	/**
	 * Specifies how long should a normal shutdown (no-force) wait for transactions to
	 * complete. Defaults to {@literal Long.MAX_VALUE}.
	 * @param defaultMaxWaitTimeOnShutdown the default max wait time on shutdown
	 */
	public void setDefaultMaxWaitTimeOnShutdown(long defaultMaxWaitTimeOnShutdown) {
		this.defaultMaxWaitTimeOnShutdown = defaultMaxWaitTimeOnShutdown;
	}

	public long getDefaultMaxWaitTimeOnShutdown() {
		return this.defaultMaxWaitTimeOnShutdown;
	}

	/**
	 * Specifies the transactions log file base name. Defaults to {@literal tmlog}. The
	 * transactions logs are stored in files using this name appended with a number and
	 * the extension {@literal .log}. At checkpoint, a new transactions log file is
	 * created and the number is incremented.
	 * @param logBaseName the log base name
	 */
	public void setLogBaseName(String logBaseName) {
		this.logBaseName = logBaseName;
	}

	public String getLogBaseName() {
		return this.logBaseName;
	}

	/**
	 * Specifies the directory in which the log files should be stored. Defaults to the
	 * current working directory. This directory should be a stable storage like a SAN,
	 * RAID or at least backed up location. The transactions logs files are as important
	 * as the data themselves to guarantee consistency in case of failures.
	 * @param logBaseDir the log base dir
	 */
	public void setLogBaseDir(String logBaseDir) {
		this.logBaseDir = logBaseDir;
	}

	public String getLogBaseDir() {
		return this.logBaseDir;
	}

	/**
	 * Specifies the interval between checkpoints. A checkpoint reduces the log file size
	 * at the expense of adding some overhead in the runtime. Defaults to {@literal 500}.
	 * @param checkpointInterval the checkpoint interval
	 */
	public void setCheckpointInterval(long checkpointInterval) {
		this.checkpointInterval = checkpointInterval;
	}

	public long getCheckpointInterval() {
		return this.checkpointInterval;
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
		this.threadedTwoPhaseCommit = threadedTwoPhaseCommit;
	}

	public boolean isThreadedTwoPhaseCommit() {
		return this.threadedTwoPhaseCommit;
	}

	public Recovery getRecovery() {
		return this.recovery;
	}

	/**
	 * Returns the properties as a {@link Properties} object that can be used with
	 * Atomikos.
	 * @return the properties
	 */
	public Properties asProperties() {
		Properties properties = new Properties();
		set(properties, "service", getService());
		set(properties, "max_timeout", getMaxTimeout());
		set(properties, "default_jta_timeout", getDefaultJtaTimeout());
		set(properties, "max_actives", getMaxActives());
		set(properties, "enable_logging", isEnableLogging());
		set(properties, "tm_unique_name", getTransactionManagerUniqueName());
		set(properties, "serial_jta_transactions", isSerialJtaTransactions());
		set(properties, "allow_subtransactions", isAllowSubTransactions());
		set(properties, "force_shutdown_on_vm_exit", isForceShutdownOnVmExit());
		set(properties, "default_max_wait_time_on_shutdown",
				getDefaultMaxWaitTimeOnShutdown());
		set(properties, "log_base_name", getLogBaseName());
		set(properties, "log_base_dir", getLogBaseDir());
		set(properties, "checkpoint_interval", getCheckpointInterval());
		set(properties, "threaded_2pc", isThreadedTwoPhaseCommit());
		Recovery recovery = getRecovery();
		set(properties, "forget_orphaned_log_entries_delay",
				recovery.getForgetOrphanedLogEntriesDelay());
		set(properties, "recovery_delay", recovery.getDelay());
		set(properties, "oltp_max_retries", recovery.getMaxRetries());
		set(properties, "oltp_retry_interval", recovery.getRetryInterval());
		return properties;
	}

	private void set(Properties properties, String key, Object value) {
		String id = "com.atomikos.icatch." + key;
		if (value != null && !properties.containsKey(id)) {
			properties.setProperty(id, asString(value));
		}
	}

	private String asString(Object value) {
		if (value instanceof Duration) {
			return String.valueOf(((Duration) value).toMillis());
		}
		return value.toString();
	}

	/**
	 * Recovery specific settings.
	 */
	public static class Recovery {

		/**
		 * Delay after which recovery can cleanup pending ('orphaned') log entries.
		 */
		private Duration forgetOrphanedLogEntriesDelay = Duration.ofMillis(86400000);

		/**
		 * Delay between two recovery scans.
		 */
		private Duration delay = Duration.ofMillis(10000);

		/**
		 * Number of retry attempts to commit the transaction before throwing an
		 * exception.
		 */
		private int maxRetries = 5;

		/**
		 * Delay between retry attempts.
		 */
		private Duration retryInterval = Duration.ofMillis(10000);

		public Duration getForgetOrphanedLogEntriesDelay() {
			return this.forgetOrphanedLogEntriesDelay;
		}

		public void setForgetOrphanedLogEntriesDelay(
				Duration forgetOrphanedLogEntriesDelay) {
			this.forgetOrphanedLogEntriesDelay = forgetOrphanedLogEntriesDelay;
		}

		public Duration getDelay() {
			return this.delay;
		}

		public void setDelay(Duration delay) {
			this.delay = delay;
		}

		public int getMaxRetries() {
			return this.maxRetries;
		}

		public void setMaxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
		}

		public Duration getRetryInterval() {
			return this.retryInterval;
		}

		public void setRetryInterval(Duration retryInterval) {
			this.retryInterval = retryInterval;
		}

	}

}
