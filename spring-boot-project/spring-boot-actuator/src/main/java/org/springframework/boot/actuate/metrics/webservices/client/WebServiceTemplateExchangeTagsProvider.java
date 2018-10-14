/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.actuate.metrics.webservices.client;

import io.micrometer.core.instrument.Tag;

import org.springframework.ws.context.MessageContext;

/**
 * Provides {@link Tag Tags} for an exchange performed by a
 * {@link org.springframework.ws.client.core.WebServiceTemplate}.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
@FunctionalInterface
public interface WebServiceTemplateExchangeTagsProvider {

	/**
	 * Provides the tags to be associated with metrics that are recorded for the given
	 * {@code request} and {@code response} exchange.
	 * @param messageContext messageContext
	 * @param exception the exception (may be {@code null} if the exchange succeed)
	 * @return the tags
	 */
	Iterable<Tag> getTags(MessageContext messageContext, Exception exception);

}
