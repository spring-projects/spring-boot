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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * A collection of {@link PlatformTransactionManagerCustomizer}.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public class TransactionManagerCustomizers {

	private final List<PlatformTransactionManagerCustomizer<?>> customizers;

	public TransactionManagerCustomizers(
			Collection<? extends PlatformTransactionManagerCustomizer<?>> customizers) {
		this.customizers = (customizers != null) ? new ArrayList<>(customizers)
				: Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public void customize(PlatformTransactionManager transactionManager) {
		LambdaSafe
				.callbacks(PlatformTransactionManagerCustomizer.class, this.customizers,
						transactionManager)
				.withLogger(TransactionManagerCustomizers.class)
				.invoke((customizer) -> customizer.customize(transactionManager));
	}

}
