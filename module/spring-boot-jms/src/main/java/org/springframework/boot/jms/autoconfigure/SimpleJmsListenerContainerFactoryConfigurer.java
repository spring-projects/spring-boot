/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jms.autoconfigure;

import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

/**
 * Configure {@link SimpleJmsListenerContainerFactory} with sensible defaults. In contrast
 * to {@link DefaultMessageListenerContainer} that uses a pull-based mechanism (polling)
 * to process messages, the {@link SimpleMessageListenerContainer} instances created by
 * this factory use a push-based mechanism that's very close to the spirit of the
 * standalone JMS specification.
 * <p>
 * As such, concurrency-related configuration properties from the {@code spring.jms}
 * namespace are not taken into account by this implementation.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code SimpleJmsListenerContainerFactory} whose configuration is based upon that
 * produced by auto-configuration.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see DefaultJmsListenerContainerFactoryConfigurer
 */
public final class SimpleJmsListenerContainerFactoryConfigurer
		extends AbstractJmsListenerContainerFactoryConfigurer<SimpleJmsListenerContainerFactory> {

}
