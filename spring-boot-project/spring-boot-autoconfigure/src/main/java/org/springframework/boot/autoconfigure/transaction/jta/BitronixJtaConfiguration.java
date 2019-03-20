/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.transaction.jta;

import java.io.File;

import javax.jms.Message;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.bitronix.BitronixDependentBeanFactoryPostProcessor;
import org.springframework.boot.jta.bitronix.BitronixXAConnectionFactoryWrapper;
import org.springframework.boot.jta.bitronix.BitronixXADataSourceWrapper;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.StringUtils;

/**
 * JTA Configuration for <A href="http://docs.codehaus.org/display/BTM/Home">Bitronix</A>.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @since 1.2.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JtaProperties.class)
@ConditionalOnClass({ JtaTransactionManager.class, BitronixContext.class })
@ConditionalOnMissingBean(PlatformTransactionManager.class)
class BitronixJtaConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConfigurationProperties(prefix = "spring.jta.bitronix.properties")
	public bitronix.tm.Configuration bitronixConfiguration(JtaProperties jtaProperties) {
		bitronix.tm.Configuration config = TransactionManagerServices.getConfiguration();
		if (StringUtils.hasText(jtaProperties.getTransactionManagerId())) {
			config.setServerId(jtaProperties.getTransactionManagerId());
		}
		File logBaseDir = getLogBaseDir(jtaProperties);
		config.setLogPart1Filename(new File(logBaseDir, "part1.btm").getAbsolutePath());
		config.setLogPart2Filename(new File(logBaseDir, "part2.btm").getAbsolutePath());
		config.setDisableJmx(true);
		return config;
	}

	private File getLogBaseDir(JtaProperties jtaProperties) {
		if (StringUtils.hasLength(jtaProperties.getLogDir())) {
			return new File(jtaProperties.getLogDir());
		}
		File home = new ApplicationHome().getDir();
		return new File(home, "transaction-logs");
	}

	@Bean
	@ConditionalOnMissingBean(TransactionManager.class)
	public BitronixTransactionManager bitronixTransactionManager(
			bitronix.tm.Configuration configuration) {
		// Inject configuration to force ordering
		return TransactionManagerServices.getTransactionManager();
	}

	@Bean
	@ConditionalOnMissingBean(XADataSourceWrapper.class)
	public BitronixXADataSourceWrapper xaDataSourceWrapper() {
		return new BitronixXADataSourceWrapper();
	}

	@Bean
	@ConditionalOnMissingBean
	public static BitronixDependentBeanFactoryPostProcessor bitronixDependentBeanFactoryPostProcessor() {
		return new BitronixDependentBeanFactoryPostProcessor();
	}

	@Bean
	public JtaTransactionManager transactionManager(UserTransaction userTransaction,
			TransactionManager transactionManager,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(
				userTransaction, transactionManager);
		transactionManagerCustomizers.ifAvailable(
				(customizers) -> customizers.customize(jtaTransactionManager));
		return jtaTransactionManager;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Message.class)
	static class BitronixJtaJmsConfiguration {

		@Bean
		@ConditionalOnMissingBean(XAConnectionFactoryWrapper.class)
		public BitronixXAConnectionFactoryWrapper xaConnectionFactoryWrapper() {
			return new BitronixXAConnectionFactoryWrapper();
		}

	}

}
