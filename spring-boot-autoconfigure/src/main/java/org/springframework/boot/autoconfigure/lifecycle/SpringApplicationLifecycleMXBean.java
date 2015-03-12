/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.lifecycle;

/**
 * A simple MBean contract to control the lifecycle of a {@code SpringApplication} via
 * JMX. Intended for internal use only.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public interface SpringApplicationLifecycleMXBean {

	/**
	 * Specify if the application has fully started and is now ready.
	 * @return {@code true} if the application is ready
	 * @see org.springframework.boot.context.event.ApplicationReadyEvent
	 */
	boolean isReady();

	/**
	 * Shutdown the application.
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	void shutdown();

}
