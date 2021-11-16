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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.TemporaryQueue;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.osjava.sj.loader.JndiLoader;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.jta.atomikos.AtomikosProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

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
class JtaAutoConfigurationTests {

	private final File atomikosLogs = new File(new BuildOutput(JtaAutoConfigurationTests.class).getRootLocation(),
			"atomikos-logs");

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}

	}

	@ParameterizedTest
	@ExtendWith(JndiExtension.class)
	@MethodSource("transactionManagerJndiEntries")
	void transactionManagerFromJndi(JndiEntry jndiEntry, InitialContext initialContext) throws NamingException {
		jndiEntry.register(initialContext);
		this.context = new AnnotationConfigApplicationContext(JtaAutoConfiguration.class);
		JtaTransactionManager transactionManager = this.context.getBean(JtaTransactionManager.class);
		if (jndiEntry.value instanceof UserTransaction) {
			assertThat(transactionManager.getUserTransaction()).isEqualTo(jndiEntry.value);
			assertThat(transactionManager.getTransactionManager()).isNull();
		}
		else {
			assertThat(transactionManager.getUserTransaction()).isInstanceOf(UserTransactionAdapter.class);
			assertThat(transactionManager.getTransactionManager()).isEqualTo(jndiEntry.value);
		}
	}

	static List<Arguments> transactionManagerJndiEntries() {
		return Arrays.asList(Arguments.of(new JndiEntry("java:comp/UserTransaction", UserTransaction.class)),
				Arguments.of(new JndiEntry("java:appserver/TransactionManager", TransactionManager.class)),
				Arguments.of(new JndiEntry("java:pm/TransactionManager", TransactionManager.class)),
				Arguments.of(new JndiEntry("java:/TransactionManager", TransactionManager.class)));
	}

	@Test
	void customTransactionManager() {
		this.context = new AnnotationConfigApplicationContext(CustomTransactionManagerConfig.class,
				JtaAutoConfiguration.class);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(JtaTransactionManager.class));
	}

	@Test
	void disableJtaSupport() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.jta.enabled:false").applyTo(this.context);
		this.context.register(JtaAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(JtaTransactionManager.class)).isEmpty();
		assertThat(this.context.getBeansOfType(XADataSourceWrapper.class)).isEmpty();
		assertThat(this.context.getBeansOfType(XAConnectionFactoryWrapper.class)).isEmpty();
	}

	@Test
	void atomikosSanityCheck() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.jta.log-dir:" + this.atomikosLogs).applyTo(this.context);
		this.context.register(JtaProperties.class, AtomikosJtaConfiguration.class);
		this.context.refresh();
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
	void defaultAtomikosTransactionManagerName(@TempDir Path dir) {
		this.context = new AnnotationConfigApplicationContext();
		File logs = new File(dir.toFile(), "jta");
		TestPropertyValues.of("spring.jta.logDir:" + logs.getAbsolutePath()).applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class);
		this.context.refresh();

		File epochFile = new File(logs, "tmlog0.log");
		assertThat(epochFile.isFile()).isTrue();
	}

	@Test
	void atomikosConnectionFactoryPoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.jta.atomikos.connectionfactory.minPoolSize:5",
				"spring.jta.atomikos.connectionfactory.maxPoolSize:10", "spring.jta.log-dir:" + this.atomikosLogs)
				.applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		AtomikosConnectionFactoryBean connectionFactory = this.context.getBean(AtomikosConnectionFactoryBean.class);
		assertThat(connectionFactory.getMinPoolSize()).isEqualTo(5);
		assertThat(connectionFactory.getMaxPoolSize()).isEqualTo(10);
	}

	@Test
	void atomikosDataSourcePoolConfiguration() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.jta.atomikos.datasource.minPoolSize:5",
				"spring.jta.atomikos.datasource.maxPoolSize:10", "spring.jta.log-dir:" + this.atomikosLogs)
				.applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class, PoolConfiguration.class);
		this.context.refresh();
		AtomikosDataSourceBean dataSource = this.context.getBean(AtomikosDataSourceBean.class);
		assertThat(dataSource.getMinPoolSize()).isEqualTo(5);
		assertThat(dataSource.getMaxPoolSize()).isEqualTo(10);
	}

	@Test
	void atomikosCustomizeJtaTransactionManagerUsingProperties() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.transaction.default-timeout:30",
				"spring.transaction.rollback-on-commit-failure:true", "spring.jta.log-dir:" + this.atomikosLogs)
				.applyTo(this.context);
		this.context.register(AtomikosJtaConfiguration.class, TransactionAutoConfiguration.class);
		this.context.refresh();
		JtaTransactionManager transactionManager = this.context.getBean(JtaTransactionManager.class);
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTransactionManagerConfig {

		@Bean
		org.springframework.transaction.TransactionManager testTransactionManager() {
			return mock(org.springframework.transaction.TransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PoolConfiguration {

		@Bean
		ConnectionFactory pooledConnectionFactory(XAConnectionFactoryWrapper wrapper) throws Exception {
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
		DataSource pooledDataSource(XADataSourceWrapper wrapper) throws Exception {
			XADataSource dataSource = mock(XADataSource.class);
			return wrapper.wrapDataSource(dataSource);
		}

	}

	private static final class JndiEntry {

		private final String name;

		private final Class<?> type;

		private final Object value;

		private JndiEntry(String name, Class<?> type) {
			this.name = name;
			this.type = type;
			this.value = mock(type);
		}

		private void register(InitialContext initialContext) throws NamingException {
			String[] components = this.name.split("/");
			String subcontextName = components[0];
			String entryName = components[1];
			Context javaComp = initialContext.createSubcontext(subcontextName);
			JndiLoader loader = new JndiLoader(initialContext.getEnvironment());
			Properties properties = new Properties();
			properties.setProperty(entryName + "/type", this.type.getName());
			properties.put(entryName + "/valueToConvert", this.value);
			loader.load(properties, javaComp);
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

	private static final class JndiExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			Namespace namespace = Namespace.create(getClass(), context.getUniqueId());
			context.getStore(namespace).getOrComputeIfAbsent(InitialContext.class, (k) -> createInitialContext(),
					InitialContext.class);
		}

		private InitialContext createInitialContext() {
			try {
				return new InitialContext();
			}
			catch (Exception ex) {
				throw new RuntimeException();
			}
		}

		@Override
		public void afterEach(ExtensionContext context) throws Exception {
			Namespace namespace = Namespace.create(getClass(), context.getUniqueId());
			InitialContext initialContext = context.getStore(namespace).remove(InitialContext.class,
					InitialContext.class);
			initialContext.removeFromEnvironment("org.osjava.sj.jndi.ignoreClose");
			initialContext.close();
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			return InitialContext.class.isAssignableFrom(parameterContext.getParameter().getType());
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			Namespace namespace = Namespace.create(getClass(), extensionContext.getUniqueId());
			return extensionContext.getStore(namespace).get(InitialContext.class);
		}

	}

}
