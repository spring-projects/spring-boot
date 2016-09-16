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

package org.springframework.boot.autoconfigure.social;

import org.springframework.core.env.Environment;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.ConnectionFactory;

/**
 * Base class for auto-configured {@link SocialConfigurerAdapter}s.
 *
 * @author Craig Walls
 * @author Phillip Webb
 * @since 1.4.0
 */
public abstract class SocialAutoConfigurerAdapter extends SocialConfigurerAdapter {

	@Override
	public void addConnectionFactories(ConnectionFactoryConfigurer configurer,
			Environment environment) {
		configurer.addConnectionFactory(createConnectionFactory());
	}

	protected abstract ConnectionFactory<?> createConnectionFactory();

}
