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

package org.springframework.boot.test.data.mongo;

import com.mongodb.MongoClient;

import org.springframework.beans.BeansException;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@link TestExecutionListener} to trigger {@link MongoFixtureRunner}.
 *
 * @author Andrii Pohrebniak
 */
public class MongoFixtureTestExecutionListener extends AbstractTestExecutionListener {

	private static final String DEFAULT_MONGO_CLIENT_BEAN_NAME = "mongo";
	private static final String DEFAULT_DATABASE = "test";

	private static final MongoFixtureRunner runner = new MongoFixtureRunner();

	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		prepareMongoClient(testContext);
		runner.beforeClass(testContext);
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		runner.beforeMethod(testContext);
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		runner.afterMethod(testContext);
	}

	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		runner.afterClass(testContext);
	}

	/**
	 * Prepares {@link MongoClient} for {@link MongoFixtureRunner}.
	 *
	 * @param testContext current test context
	 * @throws IllegalStateException when can't find {@link MongoClient} in application
	 * context
	 */
	private void prepareMongoClient(TestContext testContext) {
		try {
			MongoClient mongoClient = (MongoClient) testContext.getApplicationContext()
					.getBean(DEFAULT_MONGO_CLIENT_BEAN_NAME);
			runner.setDatabase(mongoClient.getDatabase(DEFAULT_DATABASE));
		}
		catch (BeansException e) {
			throw new IllegalStateException(
					String.format("Bean with name %s is not in application context!",
							DEFAULT_MONGO_CLIENT_BEAN_NAME));
		}
	}
}
