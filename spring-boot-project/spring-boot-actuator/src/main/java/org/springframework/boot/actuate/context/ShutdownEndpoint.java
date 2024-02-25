/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.context;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link Endpoint @Endpoint} to shutdown the {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "shutdown", enableByDefault = false)
public class ShutdownEndpoint implements ApplicationContextAware {

	private ConfigurableApplicationContext context;

	/**
     * Initiates the shutdown process.
     * 
     * @return the shutdown descriptor indicating the status of the shutdown process
     */
    @WriteOperation
	public ShutdownDescriptor shutdown() {
		if (this.context == null) {
			return ShutdownDescriptor.NO_CONTEXT;
		}
		try {
			return ShutdownDescriptor.DEFAULT;
		}
		finally {
			Thread thread = new Thread(this::performShutdown);
			thread.setContextClassLoader(getClass().getClassLoader());
			thread.start();
		}
	}

	/**
     * Performs a shutdown operation.
     * 
     * This method puts the current thread to sleep for 500 milliseconds and then closes the context.
     * If the sleep is interrupted, the current thread's interrupt status is set.
     */
    private void performShutdown() {
		try {
			Thread.sleep(500L);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		this.context.close();
	}

	/**
     * Sets the application context for the ShutdownEndpoint.
     * 
     * @param context the application context to be set
     * @throws BeansException if an error occurs while setting the application context
     */
    @Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (context instanceof ConfigurableApplicationContext configurableContext) {
			this.context = configurableContext;
		}
	}

	/**
	 * Description of the shutdown.
	 */
	public static class ShutdownDescriptor implements OperationResponseBody {

		private static final ShutdownDescriptor DEFAULT = new ShutdownDescriptor("Shutting down, bye...");

		private static final ShutdownDescriptor NO_CONTEXT = new ShutdownDescriptor("No context to shutdown.");

		private final String message;

		/**
         * Constructs a new ShutdownDescriptor with the specified message.
         * 
         * @param message the message to be associated with the ShutdownDescriptor
         */
        ShutdownDescriptor(String message) {
			this.message = message;
		}

		/**
         * Returns the message associated with the ShutdownDescriptor object.
         * 
         * @return the message associated with the ShutdownDescriptor object
         */
        public String getMessage() {
			return this.message;
		}

	}

}
