/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.jta.bitronix;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;

import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.resource.jms.PoolingConnectionFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Spring friendly version of {@link PoolingConnectionFactory}. Provides sensible defaults
 * and also supports direct wrapping of a {@link XAConnectionFactory} instance.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Andy Wilkinson
 * @since 1.2.0
 */
@SuppressWarnings("serial")
@ConfigurationProperties(prefix = "spring.jta.bitronix.connectionfactory")
public class PoolingConnectionFactoryBean extends PoolingConnectionFactory
		implements BeanNameAware, InitializingBean, DisposableBean {

	private static final ThreadLocal<PoolingConnectionFactoryBean> source = new ThreadLocal<>();

	private String beanName;

	private XAConnectionFactory connectionFactory;

	public PoolingConnectionFactoryBean() {
		setMaxPoolSize(10);
		setTestConnections(true);
		setAutomaticEnlistingEnabled(true);
		setAllowLocalTransactions(true);
	}

	@Override
	public synchronized void init() {
		source.set(this);
		try {
			super.init();
		}
		finally {
			source.remove();
		}
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!StringUtils.hasLength(getUniqueName())) {
			setUniqueName(this.beanName);
		}
		init();
	}

	@Override
	public void destroy() throws Exception {
		close();
	}

	/**
	 * Set the {@link XAConnectionFactory} directly, instead of calling
	 * {@link #setClassName(String)}.
	 * @param connectionFactory the connection factory to use
	 */
	public void setConnectionFactory(XAConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		setClassName(DirectXAConnectionFactory.class.getName());
		setDriverProperties(new Properties());
	}

	protected final XAConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	@Override
	public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean)
			throws Exception {
		if (xaFactory instanceof DirectXAConnectionFactory) {
			xaFactory = ((DirectXAConnectionFactory) xaFactory).getConnectionFactory();
		}
		return super.createPooledConnection(xaFactory, bean);
	}

	/**
	 * A {@link XAConnectionFactory} implementation that delegates to the
	 * {@link ThreadLocal} {@link PoolingConnectionFactoryBean}.
	 *
	 * @see PoolingConnectionFactoryBean#setConnectionFactory(XAConnectionFactory)
	 */
	public static class DirectXAConnectionFactory implements XAConnectionFactory {

		private final XAConnectionFactory connectionFactory;

		public DirectXAConnectionFactory() {
			this.connectionFactory = source.get().connectionFactory;
		}

		@Override
		public XAConnection createXAConnection() throws JMSException {
			return this.connectionFactory.createXAConnection();
		}

		@Override
		public XAConnection createXAConnection(String userName, String password)
				throws JMSException {
			return this.connectionFactory.createXAConnection(userName, password);
		}

		public XAConnectionFactory getConnectionFactory() {
			return this.connectionFactory;
		}

		@Override
		public XAJMSContext createXAContext() {
			return this.connectionFactory.createXAContext();
		}

		@Override
		public XAJMSContext createXAContext(String username, String password) {
			return this.connectionFactory.createXAContext(username, password);
		}

	}

}
