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

package org.springframework.boot.autoconfigure.interfaceclients.http;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.service.invoker.HttpExchangeAdapter;

/**
 * Provides an {@link HttpExchangeAdapter} instance for the {@code clientId} specified in
 * the argument.
 *
 * @author Josh Long
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
@FunctionalInterface
public interface HttpExchangeAdapterProvider {

	HttpExchangeAdapter get(ListableBeanFactory beanFactory, String clientId);

}
