/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link BindHandler} implementations.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public abstract class AbstractBindHandler implements BindHandler {

	private final BindHandler parent;

	/**
	 * Create a new binding handler instance.
	 */
	public AbstractBindHandler() {
		this(BindHandler.DEFAULT);
	}

	/**
	 * Create a new binding handler instance with a specific parent.
	 * @param parent the parent handler
	 */
	public AbstractBindHandler(BindHandler parent) {
		Assert.notNull(parent, "Parent must not be null");
		this.parent = parent;
	}

	/**
     * {@inheritDoc}
     * 
     * This method is called when the binding process starts for a specific configuration property.
     * It delegates the call to the parent bind handler's onStart method.
     * 
     * @param name the name of the configuration property
     * @param target the bindable target object
     * @param context the bind context
     * @return the bindable target object after the onStart method is called
     */
    @Override
	public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
		return this.parent.onStart(name, target, context);
	}

	/**
     * This method is called when the binding process is successful.
     * 
     * @param name     the name of the configuration property being bound
     * @param target   the bindable target object
     * @param context  the bind context
     * @param result   the result of the binding process
     * @return         the result of the binding process
     */
    @Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		return this.parent.onSuccess(name, target, context, result);
	}

	/**
     * This method is called when a binding operation fails.
     * 
     * @param name    the name of the configuration property that failed to bind
     * @param target  the bindable target that failed to bind
     * @param context the bind context
     * @param error   the exception that occurred during the binding operation
     * @return the result of the parent's onFailure method
     * @throws Exception if an error occurs during the parent's onFailure method
     */
    @Override
	public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
			throws Exception {
		return this.parent.onFailure(name, target, context, error);
	}

	/**
     * Called when the binding process finishes.
     * 
     * @param name the name of the configuration property being bound
     * @param target the bindable target object
     * @param context the bind context
     * @param result the result of the binding process
     * @throws Exception if an error occurs during the binding process
     */
    @Override
	public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
			throws Exception {
		this.parent.onFinish(name, target, context, result);
	}

}
