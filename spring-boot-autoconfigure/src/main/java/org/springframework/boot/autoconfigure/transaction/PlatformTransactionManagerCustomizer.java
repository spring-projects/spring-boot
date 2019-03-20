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

import org.springframework.transaction.PlatformTransactionManager;

/**
 * Callback interface that can be implemented by beans wishing to customize
 * {@link PlatformTransactionManager PlatformTransactionManagers} whilst retaining default
 * auto-configuration.
 *
 * @param <T> the transaction manager type
 * @author Phillip Webb
 * @since 1.5.0
 */
public interface PlatformTransactionManagerCustomizer<T extends PlatformTransactionManager> {

	/**
	 * Customize the given transaction manager.
	 * @param transactionManager the transaction manager to customize
	 */
	void customize(T transactionManager);

}
