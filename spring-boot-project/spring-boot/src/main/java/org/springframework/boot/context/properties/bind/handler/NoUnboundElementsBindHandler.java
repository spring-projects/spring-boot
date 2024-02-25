/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties.bind.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;

/**
 * {@link BindHandler} to enforce that all configuration properties under the root name
 * have been bound.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class NoUnboundElementsBindHandler extends AbstractBindHandler {

	private final Set<ConfigurationPropertyName> boundNames = new HashSet<>();

	private final Set<ConfigurationPropertyName> attemptedNames = new HashSet<>();

	private final Function<ConfigurationPropertySource, Boolean> filter;

	/**
	 * Constructs a new NoUnboundElementsBindHandler with the default BindHandler and a
	 * configuration property source filter that always returns true.
	 */
	NoUnboundElementsBindHandler() {
		this(BindHandler.DEFAULT, (configurationPropertySource) -> true);
	}

	/**
	 * Constructs a new NoUnboundElementsBindHandler with the specified parent
	 * BindHandler.
	 * @param parent the parent BindHandler
	 */
	public NoUnboundElementsBindHandler(BindHandler parent) {
		this(parent, (configurationPropertySource) -> true);
	}

	/**
	 * Constructs a new NoUnboundElementsBindHandler with the specified parent BindHandler
	 * and filter.
	 * @param parent The parent BindHandler.
	 * @param filter The filter function to determine if a ConfigurationPropertySource
	 * should be bound.
	 */
	public NoUnboundElementsBindHandler(BindHandler parent, Function<ConfigurationPropertySource, Boolean> filter) {
		super(parent);
		this.filter = filter;
	}

	/**
	 * This method is overridden from the superclass BindHandler. It is called when the
	 * binding process starts for a configuration property.
	 * @param name the name of the configuration property
	 * @param target the target bindable object
	 * @param context the bind context
	 * @return the bindable object after processing the start of binding
	 */
	@Override
	public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
		this.attemptedNames.add(name);
		return super.onStart(name, target, context);
	}

	/**
	 * This method is called when the binding process is successful for a configuration
	 * property. It adds the bound property name to the list of bound names and returns
	 * the result of the super method.
	 * @param name the name of the configuration property
	 * @param target the bindable target
	 * @param context the bind context
	 * @param result the result of the binding process
	 * @return the result of the super method
	 */
	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		this.boundNames.add(name);
		return super.onSuccess(name, target, context, result);
	}

	/**
	 * This method is called when a failure occurs during the binding process. It handles
	 * the exception and returns the appropriate response.
	 * @param name the name of the configuration property that caused the failure
	 * @param target the bindable target object
	 * @param context the bind context
	 * @param error the exception that occurred during binding
	 * @return the response object
	 * @throws Exception if an error occurs while handling the failure
	 */
	@Override
	public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
			throws Exception {
		if (error instanceof UnboundConfigurationPropertiesException) {
			throw error;
		}
		return super.onFailure(name, target, context, error);
	}

	/**
	 * This method is called when the binding process is finished. It checks if there are
	 * any unbound elements in the configuration and throws an exception if there are.
	 * @param name the name of the configuration property
	 * @param target the bindable target
	 * @param context the bind context
	 * @param result the result of the binding process
	 * @throws Exception if there are unbound elements in the configuration
	 */
	@Override
	public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
			throws Exception {
		if (context.getDepth() == 0) {
			checkNoUnboundElements(name, context);
		}
	}

	/**
	 * Checks if there are any unbound elements for the given configuration property name
	 * in the provided bind context. Unbound elements refer to configuration properties
	 * that have not been successfully bound to any source.
	 * @param name the configuration property name to check for unbound elements
	 * @param context the bind context containing the sources to check for unbound
	 * elements
	 * @throws UnboundConfigurationPropertiesException if there are any unbound elements
	 * found
	 */
	private void checkNoUnboundElements(ConfigurationPropertyName name, BindContext context) {
		Set<ConfigurationProperty> unbound = new TreeSet<>();
		for (ConfigurationPropertySource source : context.getSources()) {
			if (source instanceof IterableConfigurationPropertySource && this.filter.apply(source)) {
				collectUnbound(name, unbound, (IterableConfigurationPropertySource) source);
			}
		}
		if (!unbound.isEmpty()) {
			throw new UnboundConfigurationPropertiesException(unbound);
		}
	}

	/**
	 * Collects unbound configuration properties from the given source based on the
	 * provided name.
	 * @param name the name of the configuration property to check for unbound properties
	 * @param unbound the set to collect the unbound properties into
	 * @param source the configuration property source to search for unbound properties
	 */
	private void collectUnbound(ConfigurationPropertyName name, Set<ConfigurationProperty> unbound,
			IterableConfigurationPropertySource source) {
		IterableConfigurationPropertySource filtered = source.filter((candidate) -> isUnbound(name, candidate));
		for (ConfigurationPropertyName unboundName : filtered) {
			try {
				unbound.add(
						source.filter((candidate) -> isUnbound(name, candidate)).getConfigurationProperty(unboundName));
			}
			catch (Exception ex) {
				// Ignore
			}
		}
	}

	/**
	 * Checks if the given candidate ConfigurationPropertyName is unbound.
	 * @param name the ancestor ConfigurationPropertyName
	 * @param candidate the candidate ConfigurationPropertyName
	 * @return true if the candidate is unbound, false otherwise
	 */
	private boolean isUnbound(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
		if (name.isAncestorOf(candidate)) {
			return !this.boundNames.contains(candidate) && !isOverriddenCollectionElement(candidate);
		}
		return false;
	}

	/**
	 * Checks if the given ConfigurationPropertyName candidate is an overridden collection
	 * element.
	 * @param candidate The ConfigurationPropertyName to check
	 * @return true if the candidate is an overridden collection element, false otherwise
	 */
	private boolean isOverriddenCollectionElement(ConfigurationPropertyName candidate) {
		int lastIndex = candidate.getNumberOfElements() - 1;
		if (candidate.isLastElementIndexed()) {
			ConfigurationPropertyName propertyName = candidate.chop(lastIndex);
			return this.boundNames.contains(propertyName);
		}
		Indexed indexed = getIndexed(candidate);
		if (indexed != null) {
			String zeroethProperty = indexed.getName() + "[0]";
			if (this.boundNames.contains(ConfigurationPropertyName.of(zeroethProperty))) {
				String nestedZeroethProperty = zeroethProperty + "." + indexed.getNestedPropertyName();
				return isCandidateValidPropertyName(nestedZeroethProperty);
			}
		}
		return false;
	}

	/**
	 * Checks if the given nested zeroeth property is a valid property name.
	 * @param nestedZeroethProperty the nested zeroeth property to be checked
	 * @return true if the nested zeroeth property is a valid property name, false
	 * otherwise
	 */
	private boolean isCandidateValidPropertyName(String nestedZeroethProperty) {
		return this.attemptedNames.contains(ConfigurationPropertyName.of(nestedZeroethProperty));
	}

	/**
	 * Returns an Indexed object based on the given ConfigurationPropertyName candidate.
	 * @param candidate the ConfigurationPropertyName candidate to be processed
	 * @return an Indexed object if a numeric index is found in the candidate, null
	 * otherwise
	 */
	private Indexed getIndexed(ConfigurationPropertyName candidate) {
		for (int i = 0; i < candidate.getNumberOfElements(); i++) {
			if (candidate.isNumericIndex(i)) {
				return new Indexed(candidate.chop(i).toString(),
						candidate.getElement(i + 1, ConfigurationPropertyName.Form.UNIFORM));
			}
		}
		return null;
	}

	/**
	 * Indexed class.
	 */
	private static final class Indexed {

		private final String name;

		private final String nestedPropertyName;

		/**
		 * Constructs a new Indexed object with the specified name and nested property
		 * name.
		 * @param name the name of the Indexed object
		 * @param nestedPropertyName the name of the nested property
		 */
		private Indexed(String name, String nestedPropertyName) {
			this.name = name;
			this.nestedPropertyName = nestedPropertyName;
		}

		/**
		 * Returns the name of the Indexed object.
		 * @return the name of the Indexed object
		 */
		String getName() {
			return this.name;
		}

		/**
		 * Returns the value of the nested property name.
		 * @return the value of the nested property name
		 */
		String getNestedPropertyName() {
			return this.nestedPropertyName;
		}

	}

}
