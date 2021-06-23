/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.netty;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for the Netty engine.
 * <p>
 * These properties apply globally to the Netty library, used as a client or a server.
 *
 * @author Brian Clozel
 * @since 2.5.0
 */
@ConfigurationProperties(prefix = "spring.netty")
public class NettyProperties {

	/**
	 * Level of leak detection for reference-counted buffers.
	 */
	private LeakDetection leakDetection = LeakDetection.SIMPLE;

	public LeakDetection getLeakDetection() {
		return this.leakDetection;
	}

	public void setLeakDetection(LeakDetection leakDetection) {
		this.leakDetection = leakDetection;
	}

	public enum LeakDetection {

		/**
		 * Disable leak detection completely.
		 */
		DISABLED,

		/**
		 * Detect leaks for 1% of buffers.
		 */
		SIMPLE,

		/**
		 * Detect leaks for 1% of buffers and track where they were accessed.
		 */
		ADVANCED,

		/**
		 * Detect leaks for 100% of buffers and track where they were accessed.
		 */
		PARANOID

	}

}
