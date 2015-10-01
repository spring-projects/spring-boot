/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.hornetq;

import org.hornetq.core.config.Configuration;
import org.hornetq.jms.server.embedded.EmbeddedJMS;

/**
 * Callback interface that can be implemented by beans wishing to customize the HornetQ
 * JMS server {@link Configuration} before it is used by an auto-configured
 * {@link EmbeddedJMS} instance.
 *
 * @author Phillip Webb
 * @since 1.1.0
 * @see HornetQAutoConfiguration
 */
public interface HornetQConfigurationCustomizer {

	/**
	 * Customize the configuration.
	 * @param configuration the configuration to customize
	 */
	void customize(Configuration configuration);

}
