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

package org.springframework.boot.autoconfigure.reactor.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Reactor Core.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.reactor")
public class ReactorCoreProperties {

	private final StacktraceMode stacktraceMode = new StacktraceMode();

	public StacktraceMode getStacktraceMode() {
		return this.stacktraceMode;
	}

	public static class StacktraceMode {

		/**
		 * Set whether Reactor should collect stacktrace information at runtime.
		 */
		private boolean enabled;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
