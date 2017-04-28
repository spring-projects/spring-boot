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

package org.springframework.boot.autoconfigure.transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ResolvableType;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * A collection of {@link PlatformTransactionManagerCustomizer}.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public class TransactionManagerCustomizers {

	private static final Log logger = LogFactory
			.getLog(TransactionManagerCustomizers.class);

	private final List<PlatformTransactionManagerCustomizer<?>> customizers;

	public TransactionManagerCustomizers(
			Collection<? extends PlatformTransactionManagerCustomizer<?>> customizers) {
		this.customizers = (customizers == null ? null : new ArrayList<>(customizers));
	}

	public void customize(PlatformTransactionManager transactionManager) {
		if (this.customizers != null) {
			for (PlatformTransactionManagerCustomizer<?> customizer : this.customizers) {
				Class<?> generic = ResolvableType
						.forClass(PlatformTransactionManagerCustomizer.class,
								customizer.getClass())
						.resolveGeneric();
				if (generic.isInstance(transactionManager)) {
					customize(transactionManager, customizer);
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void customize(PlatformTransactionManager transactionManager,
			PlatformTransactionManagerCustomizer customizer) {
		try {
			customizer.customize(transactionManager);
		}
		catch (ClassCastException ex) {
			// Possibly a lambda-defined customizer which we could not resolve the generic
			// transaction manager type for
			if (logger.isDebugEnabled()) {
				logger.debug("Non-matching transaction manager type for customizer: "
						+ customizer, ex);
			}
		}
	}

}
