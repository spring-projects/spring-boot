/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;

/**
 * Callback interface for customizing {@code DefaultKafkaProducerFactory} beans.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
@FunctionalInterface
public interface DefaultKafkaProducerFactoryCustomizer {

	/**
	 * Customize the {@link DefaultKafkaProducerFactory}.
	 * @param producerFactory the producer factory to customize
	 */
	void customize(DefaultKafkaProducerFactory<?, ?> producerFactory);

}
