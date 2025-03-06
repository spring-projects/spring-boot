/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.transaction.TransactionManager;

/**
 * A collection of {@link TransactionManagerCustomizer TransactionManagerCustomizers}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.5.0
 */
public final class TransactionManagerCustomizers {

	private final List<? extends TransactionManagerCustomizer<?>> customizers;

	private TransactionManagerCustomizers(List<? extends TransactionManagerCustomizer<?>> customizers) {
		this.customizers = customizers;
	}

	/**
	 * Customize the given {@code transactionManager}.
	 * @param transactionManager the transaction manager to customize
	 * @since 3.2.0
	 */
	@SuppressWarnings("unchecked")
	public void customize(TransactionManager transactionManager) {
		LambdaSafe.callbacks(TransactionManagerCustomizer.class, this.customizers, transactionManager)
			.withLogger(TransactionManagerCustomizers.class)
			.invoke((customizer) -> customizer.customize(transactionManager));
	}

	/**
	 * Returns a new {@code TransactionManagerCustomizers} instance containing the given
	 * {@code customizers}.
	 * @param customizers the customizers
	 * @return the new instance
	 * @since 3.2.0
	 */
	public static TransactionManagerCustomizers of(Collection<? extends TransactionManagerCustomizer<?>> customizers) {
		return new TransactionManagerCustomizers(
				(customizers != null) ? new ArrayList<>(customizers) : Collections.emptyList());
	}

}
