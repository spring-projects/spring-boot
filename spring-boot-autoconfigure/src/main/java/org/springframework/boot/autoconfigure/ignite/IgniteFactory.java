/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;

/**
 * Factory that can be used to create a {@link Ignite}.
 *
 * @author wmz7year
 */
public class IgniteFactory {

	private IgniteConfiguration config;

	public IgniteFactory() {
	}

	public IgniteFactory(IgniteConfiguration config) {
		this.config = config;
	}

	/**
	 * create ignite instance with config .
	 *
	 * @return ignite instance
	 */
	public Ignite getIgniteInstance() {
		if (this.config == null) {
			this.config = new IgniteConfiguration();
			this.config.setGridLogger(new Slf4jLogger());
		}
		return Ignition.start(this.config);
	}
}
