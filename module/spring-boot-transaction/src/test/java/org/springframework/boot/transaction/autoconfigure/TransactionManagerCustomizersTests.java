/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionManagerCustomizers}.
 *
 * @author Phillip Webb
 */
class TransactionManagerCustomizersTests {

	@Test
	void customizeWithNullCustomizersShouldDoNothing() {
		TransactionManagerCustomizers.of(null).customize(mock(TransactionManager.class));
	}

	@Test
	void customizeShouldCheckGeneric() {
		List<TestCustomizer<?>> list = new ArrayList<>();
		list.add(new TestCustomizer<>());
		list.add(new TestJtaCustomizer());
		TransactionManagerCustomizers customizers = TransactionManagerCustomizers.of(list);
		customizers.customize(mock(PlatformTransactionManager.class));
		customizers.customize(mock(JtaTransactionManager.class));
		assertThat(list.get(0).getCount()).isEqualTo(2);
		assertThat(list.get(1).getCount()).isOne();
	}

	static class TestCustomizer<T extends PlatformTransactionManager> implements TransactionManagerCustomizer<T> {

		private int count;

		@Override
		public void customize(T transactionManager) {
			this.count++;
		}

		int getCount() {
			return this.count;
		}

	}

	static class TestJtaCustomizer extends TestCustomizer<JtaTransactionManager> {

	}

}
