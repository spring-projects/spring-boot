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

package org.springframework.boot.testsupport.rule;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.ClassUtils;

/**
 * {@link TestRule} for working with an optional Redis server.
 *
 * @author Eric Bottard
 * @author Gary Russell
 * @author Dave Syer
 * @author Phillip Webb
 */
public class RedisTestServer implements TestRule {

	private static final Log logger = LogFactory.getLog(RedisTestServer.class);

	private RedisConnectionFactory connectionFactory;

	@Override
	public Statement apply(final Statement base, Description description) {
		try {
			this.connectionFactory = createConnectionFactory();
			return new RedisStatement(base, this.connectionFactory);
		}
		catch (Exception ex) {
			logger.error("No Redis server available", ex);
			return new SkipStatement();
		}
	}

	private RedisConnectionFactory createConnectionFactory() {
		ClassLoader classLoader = RedisTestServer.class.getClassLoader();
		RedisConnectionFactory cf;
		if (ClassUtils.isPresent("redis.clients.jedis.Jedis", classLoader)) {
			cf = new JedisConnectionFactoryConfiguration().createConnectionFactory();
		}
		else {
			cf = new LettuceConnectionFactoryConfiguration().createConnectionFactory();
		}

		testConnection(cf);
		return cf;
	}

	private void testConnection(RedisConnectionFactory connectionFactory) {
		connectionFactory.getConnection().close();
	}

	/**
	 * Return the Redis connection factory or {@code null} if the factory is not
	 * available.
	 * @return the connection factory or {@code null}
	 */
	public RedisConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	private static class RedisStatement extends Statement {

		private final Statement base;

		private final RedisConnectionFactory connectionFactory;

		RedisStatement(Statement base, RedisConnectionFactory connectionFactory) {
			this.base = base;
			this.connectionFactory = connectionFactory;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				this.base.evaluate();
			}
			finally {
				try {
					if (this.connectionFactory instanceof DisposableBean) {
						((DisposableBean) this.connectionFactory).destroy();
					}
				}
				catch (Exception ex) {
					logger.warn("Exception while trying to cleanup redis resource", ex);
				}
			}
		}

	}

	private static class SkipStatement extends Statement {

		@Override
		public void evaluate() throws Throwable {
			Assume.assumeTrue("Skipping test due to " + "Redis ConnectionFactory"
					+ " not being available", false);
		}

	}

	private static class JedisConnectionFactoryConfiguration {

		RedisConnectionFactory createConnectionFactory() {
			JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
			connectionFactory.afterPropertiesSet();
			return connectionFactory;
		}

	}

	private static class LettuceConnectionFactoryConfiguration {

		RedisConnectionFactory createConnectionFactory() {
			LettuceClientConfiguration config = LettuceClientConfiguration.builder()
					.shutdownTimeout(Duration.ofMillis(0)).build();
			LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
					new RedisStandaloneConfiguration(), config);
			connectionFactory.afterPropertiesSet();
			return connectionFactory;
		}

	}

}
