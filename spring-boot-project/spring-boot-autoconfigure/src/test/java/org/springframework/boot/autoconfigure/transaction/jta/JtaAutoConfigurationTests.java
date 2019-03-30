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
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jms.ConnectionFactory;
import javax.jms.TemporaryQueue;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.jta.atomikos.AtomikosProperties;
import org.springframework.boot.jta.bitronix.BitronixDependentBeanFactoryPostProcessor;
import org.springframework.boot.jta.bitronix.PoolingConnectionFactoryBean;
import org.springframework.boot.jta.bitronix.PoolingDataSourceBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JtaAutoConfiguration}.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Nishant Raut
 */
public class JtaAutoConfigurationTests {

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void customPlatformTransactionManager() {
		this.context = new AnnotationConfigApplicationContext(
				CustomTransactionManagerConfig.class, JtaAutoConfiguration.class);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(JtaTransactionManager.class));
	}

	@Test
	public void disableJtaSupport() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.jta.enabled:false").applyTo(this.context);
		this.context.register(JtaAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(JtaTransactionManager.class)).isEmpty();
		assertThat(this.context.getBeansOfType(XADataSourceWrapper.class)).isEmpty();
		assertThat(this.context.getBeansOfType(XAConnectionFactoryWrapper.class))
				.isEmpty();
	}

	@Test
	public void atomikosSanityCheck() {
		this.context = new AnnotationConfigApplicationContext(JtaProperties.class,
				AtomikosJtaConfiguration.class);
		this.context.getBean(AtomikosProperties.class);
		this.context.getBean(UserTransactionService.class);
		this.context.getBean(UserTransactionManager.class);
		this.context.getBean(UserTransaction.class);
		this.context.getBean(XADataSourceWrapper.class);
		this.context.getBean(XAConnectionFactoryWrapper.class);
		this.context.getBean(AtomikosDependsOnBeanFactoryPostProcessor.class);
		this.context.getBean(JtaTransactionManager.class);
	}

	@Test
	public void bitronixSanityCheck() {
		this.context = new AnnotationConfigApplicationContext(JtaProperties.class,
				BitronixJtaConfiguration.class);
		this.context.getBean(bitronix.tm.Configuration.class);
		this.context.getBean(TransactionManager.class);
		this.context.getBean(XADataSourceWrapper.class);
		this.context.getBean(XAConnectionFactoryWrapper.class);
		this.context.getBean(BitronixDependentBeanFactoryPostProcessor.class);
		this.context.getBean(JtaTransactionManager.class);
	}

	@Test
	public void defaultBitronixServerId() throws UnknownHostException {
		this.context = new AnnotationConfigApplicationContext(
				BitronixJtaConfiguration.class);
		String serverId = this.context.getBean(bitronix.tm.Configuration.class)
				.getServerId();
		assertThat(serverId).isEqualTo(InetAddress.getLocalHost().getHostAddress());
	}

	@Test
	public void customBitronixServerId() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.jta.transactionManagerId:custom")
				.applyTo(this.context);
		this.context.register(BitronixJtaConfiguration.class);
		this.context.refresh();
		String serverId = this.context.getBean(bitronix.tm.Configuration.class)
				.getServerId();
		assertThat(serverId).isEqualTo("custom");
	}

	@Test
	public void defaultAtomikosTransactionManagerName() throws IOException {
		this.context = new AnnotationConfigApplicationContext();
		File logs = this.temp.newFolder("jta");
		TestPropertyValues.of("spring.jta.logDir:" + logs.getAbsolutePath())
				.applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class);
		this.context.refresh();

		File epochFile = new File(logs, "tmlog0.log");
		assertThat(epochFile.isFile()).isTrue();
	}

	@Test
	public void atomikosConnectionFactoryPoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.jta.atomikos.connectionfactory.minPoolSize:5",
						"spring.jta.atomikos.connectionfactory.maxPoolSize:10")
				.applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		AtomikosConnectionFactoryBean connectionFactory = this.context
				.getBean(AtomikosConnectionFactoryBean.class);
		assertThat(connectionFactory.getMinPoolSize()).isEqualTo(5);
		assertThat(connectionFactory.getMaxPoolSize()).isEqualTo(10);
	}

	@Test
	public void bitronixConnectionFactoryPoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.jta.bitronix.connectionfactory.minPoolSize:5",
						"spring.jta.bitronix.connectionfactory.maxPoolSize:10")
				.applyTo(this.context);
		this.context.register(BitronixJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		PoolingConnectionFactoryBean connectionFactory = this.context
				.getBean(PoolingConnectionFactoryBean.class);
		assertThat(connectionFactory.getMinPoolSize()).isEqualTo(5);
		assertThat(connectionFactory.getMaxPoolSize()).isEqualTo(10);
	}

	@Test
	public void atomikosDataSourcePoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.jta.atomikos.datasource.minPoolSize:5",
						"spring.jta.atomikos.datasource.maxPoolSize:10")
				.applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		AtomikosDataSourceBean dataSource = this.context
				.getBean(AtomikosDataSourceBean.class);
		assertThat(dataSource.getMinPoolSize()).isEqualTo(5);
		assertThat(dataSource.getMaxPoolSize()).isEqualTo(10);
	}

	@Test
	public void bitronixDataSourcePoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.jta.bitronix.datasource.minPoolSize:5",
						"spring.jta.bitronix.datasource.maxPoolSize:10")
				.applyTo(this.context);
		this.context.register(BitronixJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		PoolingDataSourceBean dataSource = this.context
				.getBean(PoolingDataSourceBean.class);
		assertThat(dataSource.getMinPoolSize()).isEqualTo(5);
		assertThat(dataSource.getMaxPoolSize()).isEqualTo(10);
	}

	@Test
	public void atomikosCustomizeJtaTransactionManagerUsingProperties() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.transaction.default-timeout:30",
						"spring.transaction.rollback-on-commit-failure:true")
				.applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		JtaTransactionManager transactionManager = this.context
				.getBean(JtaTransactionManager.class);
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
	}

	@Test
	public void bitronixCustomizeJtaTransactionManagerUsingProperties() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.transaction.default-timeout:30",
						"spring.transaction.rollback-on-commit-failure:true")
				.applyTo(this.context);
		this.context.register(BitronixJtaConfiguration.class,
				TransactionAutoConfiguration.class);
		this.context.refresh();
		JtaTransactionManager transactionManager = this.context
				.getBean(JtaTransactionManager.class);
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomTransactionManagerConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class PoolConfiguration {

		@Bean
		public ConnectionFactory pooledConnectionFactory(
				XAConnectionFactoryWrapper wrapper) throws Exception {
			XAConnectionFactory connectionFactory = mock(XAConnectionFactory.class);
			XAConnection connection = mock(XAConnection.class);
			XASession session = mock(XASession.class);
			TemporaryQueue queue = mock(TemporaryQueue.class);
			XAResource resource = mock(XAResource.class);
			given(connectionFactory.createXAConnection()).willReturn(connection);
			given(connection.createXASession()).willReturn(session);
			given(session.createTemporaryQueue()).willReturn(queue);
			given(session.getXAResource()).willReturn(resource);
			return wrapper.wrapConnectionFactory(connectionFactory);
		}

		@Bean
		public DataSource pooledDataSource(XADataSourceWrapper wrapper) throws Exception {
			XADataSource dataSource = mock(XADataSource.class);
			return wrapper.wrapDataSource(dataSource);
		}

	}

}
