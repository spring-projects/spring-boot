/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.transaction;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionManagerCustomizers}.
 *
 * @author Phillip Webb
 */
public class TransactionManagerCustomizersTests {

	@Test
	public void customizeWithNullCustomizersShouldDoNothing() {
		new TransactionManagerCustomizers(null)
				.customize(mock(PlatformTransactionManager.class));
	}

	@Test
	public void customizeShouldCheckGeneric() {
		List<TestCustomizer<?>> list = new ArrayList<>();
		list.add(new TestCustomizer<>());
		list.add(new TestJtaCustomizer());
		TransactionManagerCustomizers customizers = new TransactionManagerCustomizers(
				list);
		customizers.customize(mock(PlatformTransactionManager.class));
		customizers.customize(mock(JtaTransactionManager.class));
		assertThat(list.get(0).getCount()).isEqualTo(2);
		assertThat(list.get(1).getCount()).isEqualTo(1);
	}

	private static class TestCustomizer<T extends PlatformTransactionManager>
			implements PlatformTransactionManagerCustomizer<T> {

		private int count;

		@Override
		public void customize(T transactionManager) {
			this.count++;
		}

		public int getCount() {
			return this.count;
		}

	}

	private static class TestJtaCustomizer extends TestCustomizer<JtaTransactionManager> {

	}

}
