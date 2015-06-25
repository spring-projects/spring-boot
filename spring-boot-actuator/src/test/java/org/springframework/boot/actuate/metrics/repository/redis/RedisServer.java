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

package org.springframework.boot.actuate.metrics.repository.redis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import static org.junit.Assert.fail;

/**
 * @author Eric Bottard
 * @author Gary Russell
 * @author Dave Syer
 */
public class RedisServer implements TestRule {

	private static final String EXTERNAL_SERVERS_REQUIRED = "EXTERNAL_SERVERS_REQUIRED";

	protected JedisConnectionFactory resource;

	private final String resourceDescription = "Redis ConnectionFactory";

	private static final Log logger = LogFactory.getLog(RedisServer.class);

	public static RedisServer running() {
		return new RedisServer();
	}

	private RedisServer() {
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		try {
			this.resource = obtainResource();
		}
		catch (Exception ex) {
			maybeCleanup();
			return failOrSkip(ex);
		}

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				try {
					base.evaluate();
				}
				finally {
					try {
						cleanupResource();
					}
					catch (Exception ignored) {
						RedisServer.logger.warn(
								"Exception while trying to cleanup proper resource",
								ignored);
					}
				}
			}

		};
	}

	private Statement failOrSkip(Exception exception) {
		String serversRequired = System.getenv(EXTERNAL_SERVERS_REQUIRED);
		if ("true".equalsIgnoreCase(serversRequired)) {
			logger.error(this.resourceDescription + " IS REQUIRED BUT NOT AVAILABLE",
					exception);
			fail(this.resourceDescription + " IS NOT AVAILABLE");
			// Never reached, here to satisfy method signature
			return null;
		}
		else {
			logger.error(this.resourceDescription + " IS NOT AVAILABLE, SKIPPING TESTS",
					exception);
			return new Statement() {

				@Override
				public void evaluate() throws Throwable {
					Assume.assumeTrue("Skipping test due to "
							+ RedisServer.this.resourceDescription
							+ " not being available", false);
				}
			};
		}
	}

	private void maybeCleanup() {
		if (this.resource != null) {
			try {
				cleanupResource();
			}
			catch (Exception ignored) {
				logger.warn("Exception while trying to cleanup failed resource", ignored);
			}
		}
	}

	public RedisConnectionFactory getResource() {
		return this.resource;
	}

	/**
	 * Perform cleanup of the {@link #resource} field, which is guaranteed to be non null.
	 *
	 * @throws Exception any exception thrown by this method will be logged and swallowed
	 */
	protected void cleanupResource() throws Exception {
		this.resource.destroy();
	}

	/**
	 * Try to obtain and validate a resource. Implementors should either set the
	 * {@link #resource} field with a valid resource and return normally, or throw an
	 * exception.
	 * @return the jedis connection factory
	 * @throws Exception if the factory cannot be obtained
	 */
	protected JedisConnectionFactory obtainResource() throws Exception {
		JedisConnectionFactory resource = new JedisConnectionFactory();
		resource.afterPropertiesSet();
		resource.getConnection().close();
		return resource;
	}

}
