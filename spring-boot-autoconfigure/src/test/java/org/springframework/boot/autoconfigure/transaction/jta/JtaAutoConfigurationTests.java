/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.transaction.jta;

import java.io.File;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jta.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.jta.atomikos.AtomikosProperties;
import org.springframework.boot.jta.bitronix.BitronixDependentBeanFactoryPostProcessor;
import org.springframework.boot.jta.bitronix.PoolingConnectionFactoryBean;
import org.springframework.boot.jta.bitronix.PoolingDataSourceBean;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.FileSystemUtils;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jms.AtomikosConnectionFactoryBean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JtaAutoConfiguration}.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class JtaAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@Before
	public void cleanUpLogs() {
		FileSystemUtils.deleteRecursively(new File("target/transaction-logs"));
	}

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void customPlatformTransactionManager() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				CustomTransactionManagerConfig.class, JtaAutoConfiguration.class);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(JtaTransactionManager.class);
	}

	@Test
	public void disableJtaSupport() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "spring.jta.enabled:false");
		this.context.register(JtaAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeansOfType(JtaTransactionManager.class).size());
		assertEquals(0, this.context.getBeansOfType(XADataSourceWrapper.class).size());
		assertEquals(0, this.context.getBeansOfType(XAConnectionFactoryWrapper.class)
				.size());
	}

	@Test
	public void atomikosSanityCheck() throws Exception {
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
	public void bitronixSanityCheck() throws Exception {
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
				JtaPropertiesConfiguration.class, BitronixJtaConfiguration.class);
		String serverId = this.context.getBean(bitronix.tm.Configuration.class)
				.getServerId();
		assertThat(serverId, is(equalTo(InetAddress.getLocalHost().getHostAddress())));
	}

	@Test
	public void customBitronixServerId() throws UnknownHostException {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.transactionManagerId:custom");
		this.context.register(JtaPropertiesConfiguration.class,
				BitronixJtaConfiguration.class);
		this.context.refresh();
		String serverId = this.context.getBean(bitronix.tm.Configuration.class)
				.getServerId();
		assertThat(serverId, is(equalTo("custom")));
	}

	@Test
	public void defaultAtomikosTransactionManagerName() throws UnknownHostException {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.logDir:target/transaction-logs");
		this.context.register(JtaPropertiesConfiguration.class,
				AtomikosJtaConfiguration.class);
		this.context.refresh();

		File epochFile = new File("target/transaction-logs/"
				+ InetAddress.getLocalHost().getHostAddress() + ".tm0.epoch");
		assertTrue(epochFile.isFile());
	}

	@Test
	public void customAtomikosTransactionManagerName() throws BeansException, Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.transactionManagerId:custom",
				"spring.jta.logDir:target/transaction-logs");
		this.context.register(JtaPropertiesConfiguration.class,
				AtomikosJtaConfiguration.class);
		this.context.refresh();

		File epochFile = new File("target/transaction-logs/custom0.epoch");
		assertTrue(epochFile.isFile());
	}

	@Test
	public void atomikosConnectionFactoryPoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.atomikos.connectionfactory.minPoolSize:5",
				"spring.jta.atomikos.connectionfactory.maxPoolSize:10");
		this.context.register(JtaPropertiesConfiguration.class,
				AtomikosJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		AtomikosConnectionFactoryBean connectionFactory = this.context
				.getBean(AtomikosConnectionFactoryBean.class);
		assertThat(connectionFactory.getMinPoolSize(), is(equalTo(5)));
		assertThat(connectionFactory.getMaxPoolSize(), is(equalTo(10)));
	}

	@Test
	public void bitronixConnectionFactoryPoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.bitronix.connectionfactory.minPoolSize:5",
				"spring.jta.bitronix.connectionfactory.maxPoolSize:10");
		this.context.register(JtaPropertiesConfiguration.class,
				BitronixJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		PoolingConnectionFactoryBean connectionFactory = this.context
				.getBean(PoolingConnectionFactoryBean.class);
		assertThat(connectionFactory.getMinPoolSize(), is(equalTo(5)));
		assertThat(connectionFactory.getMaxPoolSize(), is(equalTo(10)));
	}

	@Test
	public void atomikosDataSourcePoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.atomikos.datasource.minPoolSize:5",
				"spring.jta.atomikos.datasource.maxPoolSize:10");
		this.context.register(JtaPropertiesConfiguration.class,
				AtomikosJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		AtomikosDataSourceBean dataSource = this.context
				.getBean(AtomikosDataSourceBean.class);
		assertThat(dataSource.getMinPoolSize(), is(equalTo(5)));
		assertThat(dataSource.getMaxPoolSize(), is(equalTo(10)));
	}

	@Test
	public void bitronixDataSourcePoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.bitronix.datasource.minPoolSize:5",
				"spring.jta.bitronix.datasource.maxPoolSize:10");
		this.context.register(JtaPropertiesConfiguration.class,
				BitronixJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		PoolingDataSourceBean dataSource = this.context
				.getBean(PoolingDataSourceBean.class);
		assertThat(dataSource.getMinPoolSize(), is(equalTo(5)));
		assertThat(dataSource.getMaxPoolSize(), is(equalTo(10)));
	}

	@Configuration
	@EnableConfigurationProperties(JtaProperties.class)
	public static class JtaPropertiesConfiguration {

	}

	@Configuration
	public static class CustomTransactionManagerConfig {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

	@Configuration
	public static class PoolConfiguration {

		@Bean
		public ConnectionFactory pooledConnectionFactory(
				XAConnectionFactoryWrapper wrapper) throws Exception {
			XAConnectionFactory connectionFactory = mock(XAConnectionFactory.class);
			XAConnection connection = mock(XAConnection.class);
			XASession session = mock(XASession.class);
			TemporaryQueue queue = mock(TemporaryQueue.class);
			XAResource resource = mock(XAResource.class);
			when(connectionFactory.createXAConnection()).thenReturn(connection);
			when(connection.createXASession()).thenReturn(session);
			when(session.createTemporaryQueue()).thenReturn(queue);
			when(session.getXAResource()).thenReturn(resource);
			return wrapper.wrapConnectionFactory(connectionFactory);
		}

		@Bean
		public DataSource pooledDataSource(XADataSourceWrapper wrapper) throws Exception {
			XADataSource dataSource = mock(XADataSource.class);
			return wrapper.wrapDataSource(dataSource);
		}
	}

}
