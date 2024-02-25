/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.artemis;

import org.apache.activemq.artemis.spi.core.naming.BindingRegistry;

/**
 * A no-op implementation of the {@link BindingRegistry}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ArtemisNoOpBindingRegistry implements BindingRegistry {

	/**
     * Looks up a value based on the given string.
     * 
     * @param s the string to lookup
     * @return the value associated with the given string, or null if not found
     */
    @Override
	public Object lookup(String s) {
		return null;
	}

	/**
     * Binds the specified object to the specified string key.
     *
     * @param s the string key to bind the object to
     * @param o the object to be bound
     * @return true if the object was successfully bound, false otherwise
     */
    @Override
	public boolean bind(String s, Object o) {
		return false;
	}

	/**
     * Unbinds the specified string from the ArtemisNoOpBindingRegistry.
     * 
     * @param s the string to unbind
     */
    @Override
	public void unbind(String s) {
	}

	/**
     * Closes the ArtemisNoOpBindingRegistry.
     */
    @Override
	public void close() {
	}

}
