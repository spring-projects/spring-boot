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

package org.springframework.boot.autoconfigure.jta;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

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
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.jta.atomikos.AtomikosProperties;
import org.springframework.boot.jta.bitronix.BitronixDependentBeanFactoryPostProcessor;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.FileSystemUtils;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.jta.UserTransactionManager;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jta.enabled:false");
		this.context.register(JtaAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeansOfType(JtaTransactionManager.class).size());
		assertEquals(0, this.context.getBeansOfType(XADataSourceWrapper.class).size());
		assertEquals(0, this.context.getBeansOfType(XAConnectionFactoryWrapper.class).size());
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

}
