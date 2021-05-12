/*
 * Copyright 2012-2021 the original author or authors.
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
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.StringUtils;

/**
 * JTA Configuration for <A href="https://github.com/bitronix/btm">Bitronix</A>.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @deprecated since 2.3.0 for removal in 2.5.0 as the Bitronix project is no longer being
 * maintained
 */
@Deprecated
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JtaProperties.class)
@ConditionalOnClass({ JtaTransactionManager.class, BitronixContext.class })
@ConditionalOnMissingBean(org.springframework.transaction.TransactionManager.class)
class BitronixJtaConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConfigurationProperties(prefix = "spring.jta.bitronix.properties")
	bitronix.tm.Configuration bitronixConfiguration(JtaProperties jtaProperties) {
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
	BitronixTransactionManager bitronixTransactionManager(bitronix.tm.Configuration configuration) {
		// Inject configuration to force ordering
		return TransactionManagerServices.getTransactionManager();
	}

	@Bean
	@ConditionalOnMissingBean(XADataSourceWrapper.class)
	org.springframework.boot.jta.bitronix.BitronixXADataSourceWrapper xaDataSourceWrapper() {
		return new org.springframework.boot.jta.bitronix.BitronixXADataSourceWrapper();
	}

	@Bean
	@ConditionalOnMissingBean
	static org.springframework.boot.jta.bitronix.BitronixDependentBeanFactoryPostProcessor bitronixDependentBeanFactoryPostProcessor() {
		return new org.springframework.boot.jta.bitronix.BitronixDependentBeanFactoryPostProcessor();
	}

	@Bean
	JtaTransactionManager transactionManager(UserTransaction userTransaction, TransactionManager transactionManager,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(userTransaction, transactionManager);
		transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(jtaTransactionManager));
		return jtaTransactionManager;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Message.class)
	static class BitronixJtaJmsConfiguration {

		@Bean
		@ConditionalOnMissingBean(XAConnectionFactoryWrapper.class)
		org.springframework.boot.jta.bitronix.BitronixXAConnectionFactoryWrapper xaConnectionFactoryWrapper() {
			return new org.springframework.boot.jta.bitronix.BitronixXAConnectionFactoryWrapper();
		}

	}

}
