/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.transaction.TransactionManager;

/**
 * A collection of {@link TransactionManagerCustomizer TransactionManagerCustomizers}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public final class TransactionManagerCustomizers {

	private final List<? extends TransactionManagerCustomizer<?>> customizers;

	private TransactionManagerCustomizers(List<? extends TransactionManagerCustomizer<?>> customizers) {
		this.customizers = customizers;
	}

	/**
	 * Customize the given {@code transactionManager}.
	 * @param transactionManager the transaction manager to customize
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
	 */
	public static TransactionManagerCustomizers of(
			@Nullable Collection<? extends TransactionManagerCustomizer<?>> customizers) {
		return new TransactionManagerCustomizers(
				(customizers != null) ? new ArrayList<>(customizers) : Collections.emptyList());
	}

}
