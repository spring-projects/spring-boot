/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.batch.jdbc.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BatchJdbcProperties}.
 *
 * @author Andy Wilkinson
 */
class BatchJdbcPropertiesTests {

	@Test
	void validateTransactionStateDefaultMatchesSpringBatchDefault() {
		assertThat(new BatchJdbcProperties().isValidateTransactionState())
			.isEqualTo(new TestBatchConfiguration().getValidateTransactionState());
	}

	static class TestBatchConfiguration extends JdbcDefaultBatchConfiguration {

		@Override
		public boolean getValidateTransactionState() {
			return super.getValidateTransactionState();
		}

	}

}
