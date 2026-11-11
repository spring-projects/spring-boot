/*
 * Copyright 2012-present the original author or authors.
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
