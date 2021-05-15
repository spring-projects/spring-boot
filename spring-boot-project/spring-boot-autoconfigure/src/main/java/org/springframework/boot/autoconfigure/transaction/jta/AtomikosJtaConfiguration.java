/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Properties;

import javax.jms.Message;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.jta.atomikos.AtomikosProperties;
import org.springframework.boot.jta.atomikos.AtomikosXAConnectionFactoryWrapper;
import org.springframework.boot.jta.atomikos.AtomikosXADataSourceWrapper;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.StringUtils;

/**
 * JTA Configuration for <A href="https://www.atomikos.com/">Atomikos</a>.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ AtomikosProperties.class, JtaProperties.class })
@ConditionalOnClass({ JtaTransactionManager.class, UserTransactionManager.class })
@ConditionalOnMissingBean(org.springframework.transaction.TransactionManager.class)
class AtomikosJtaConfiguration {

	@Bean(initMethod = "init", destroyMethod = "shutdownWait")
	@ConditionalOnMissingBean(UserTransactionService.class)
	UserTransactionServiceImp userTransactionService(AtomikosProperties atomikosProperties,
			JtaProperties jtaProperties) {
		Properties properties = new Properties();
		if (StringUtils.hasText(jtaProperties.getTransactionManagerId())) {
			properties.setProperty("com.atomikos.icatch.tm_unique_name", jtaProperties.getTransactionManagerId());
		}
		properties.setProperty("com.atomikos.icatch.log_base_dir", getLogBaseDir(jtaProperties));
		properties.putAll(atomikosProperties.asProperties());
		return new UserTransactionServiceImp(properties);
	}

	private String getLogBaseDir(JtaProperties jtaProperties) {
		if (StringUtils.hasLength(jtaProperties.getLogDir())) {
			return jtaProperties.getLogDir();
		}
		File home = new ApplicationHome().getDir();
		return new File(home, "transaction-logs").getAbsolutePath();
	}

	@Bean(initMethod = "init", destroyMethod = "close")
	@ConditionalOnMissingBean(TransactionManager.class)
	UserTransactionManager atomikosTransactionManager(UserTransactionService userTransactionService) throws Exception {
		UserTransactionManager manager = new UserTransactionManager();
		manager.setStartupTransactionService(false);
		manager.setForceShutdown(true);
		return manager;
	}

	@Bean
	@ConditionalOnMissingBean(XADataSourceWrapper.class)
	AtomikosXADataSourceWrapper xaDataSourceWrapper() {
		return new AtomikosXADataSourceWrapper();
	}

	@Bean
	@ConditionalOnMissingBean
	static AtomikosDependsOnBeanFactoryPostProcessor atomikosDependsOnBeanFactoryPostProcessor() {
		return new AtomikosDependsOnBeanFactoryPostProcessor();
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
	static class AtomikosJtaJmsConfiguration {

		@Bean
		@ConditionalOnMissingBean(XAConnectionFactoryWrapper.class)
		AtomikosXAConnectionFactoryWrapper xaConnectionFactoryWrapper() {
			return new AtomikosXAConnectionFactoryWrapper();
		}

	}

}
