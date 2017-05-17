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

package org.springframework.boot;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.AbstractPropertyResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Utility class for converting one environment type to another.
 *
 * @author Ethan Rubinson
 * @since 1.5.4
 */
final class EnvironmentConverter {

	private static final Log logger = LogFactory.getLog(EnvironmentConverter.class);

	private static final Set<String> SERVLET_ENVIRONMENT_SOURCE_NAMES;

	static {
		final Set<String> names = new HashSet<String>();
		names.add(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME);
		names.add(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME);
		names.add(StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME);
		SERVLET_ENVIRONMENT_SOURCE_NAMES = Collections.unmodifiableSet(names);
	}

	private EnvironmentConverter() {

	}

	/**
	 * Converts the specified environment to a {@link StandardEnvironment}.
	 *
	 * @param environment The environment to convert.
	 * @return The converted environment.
	 */
	protected static ConfigurableEnvironment convertToStandardEnvironment(
			ConfigurableEnvironment environment) {
		final StandardEnvironment result = new StandardEnvironment();

		/* Handle specific environment type conversions */
		if (environment instanceof AbstractEnvironment) {
			cloneAbstractEnvironmentSpecificConfigurations(
					(AbstractEnvironment) environment, result);
		}

		/* Copy the profiles */
		result.setActiveProfiles(environment.getActiveProfiles());
		result.setDefaultProfiles(environment.getDefaultProfiles());

		/* Copy the conversion service */
		result.setConversionService(environment.getConversionService());

		/*
		 * Copy over all of the property sources except those unrelated to a standard
		 * environment
		 */
		removeAllPropertySources(result.getPropertySources());
		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			if (!SERVLET_ENVIRONMENT_SOURCE_NAMES.contains(propertySource.getName())) {
				result.getPropertySources().addLast(propertySource);
			}
		}

		return result;
	}

	private static void cloneAbstractEnvironmentSpecificConfigurations(
			AbstractEnvironment oldEnv, AbstractEnvironment newEnv) {
		clonePropertyResolverConfiguration(oldEnv, newEnv);
	}

	private static void clonePropertyResolverConfiguration(AbstractEnvironment oldEnv,
			AbstractEnvironment newEnv) {
		try {
			final Field newPropertyResolverField = AbstractEnvironment.class
					.getDeclaredField("propertyResolver");
			newPropertyResolverField.setAccessible(true);
			final ConfigurablePropertyResolver newPropertyResolver = (ConfigurablePropertyResolver) newPropertyResolverField
					.get(newEnv);

			/* Attempt to get the current property resolver of the abstract environment */
			ConfigurablePropertyResolver oldPropertyResolver = null;
			if (oldEnv instanceof AbstractEnvironment) {
				final Field oldPropertyResolverField = AbstractEnvironment.class
						.getDeclaredField("propertyResolver");
				oldPropertyResolverField.setAccessible(true);
				oldPropertyResolver = (ConfigurablePropertyResolver) oldPropertyResolverField
						.get(oldEnv);
			}

			if (oldPropertyResolver instanceof AbstractPropertyResolver) {
				final Field ignoreUnresolvableNestedPlaceholdersField = AbstractPropertyResolver.class
						.getDeclaredField("ignoreUnresolvableNestedPlaceholders");
				final Field placeholderPrefixField = AbstractPropertyResolver.class
						.getDeclaredField("placeholderPrefix");
				final Field placeholderSuffixField = AbstractPropertyResolver.class
						.getDeclaredField("placeholderSuffix");
				final Field valueSeparatorField = AbstractPropertyResolver.class
						.getDeclaredField("valueSeparator");
				final Field requiredPropertiesField = AbstractPropertyResolver.class
						.getDeclaredField("requiredProperties");

				ignoreUnresolvableNestedPlaceholdersField.setAccessible(true);
				placeholderPrefixField.setAccessible(true);
				placeholderSuffixField.setAccessible(true);
				valueSeparatorField.setAccessible(true);
				requiredPropertiesField.setAccessible(true);

				final boolean ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholdersField
						.getBoolean(oldPropertyResolver);
				final String placeholderPrefix = (String) placeholderPrefixField
						.get(oldPropertyResolver);
				final String placeholderSuffix = (String) placeholderSuffixField
						.get(oldPropertyResolver);
				final String valueSeparator = (String) valueSeparatorField
						.get(oldPropertyResolver);
				final Set<?> requiredProperties = (Set<?>) requiredPropertiesField
						.get(oldPropertyResolver);

				newPropertyResolver
						.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
				newPropertyResolver.setPlaceholderPrefix(placeholderPrefix);
				newPropertyResolver.setPlaceholderSuffix(placeholderSuffix);
				newPropertyResolver.setValueSeparator(valueSeparator);
				newPropertyResolver.setRequiredProperties(requiredProperties
						.toArray(new String[0]));
			}
		}
		catch (Exception conversionEx) {
			final String errMsg = "Failed to clone the environment's property resolver configuration";
			if (logger.isErrorEnabled()) {
				logger.error(errMsg, conversionEx);
			}
			throw new RuntimeException(errMsg, conversionEx);
		}
	}

	private static void removeAllPropertySources(MutablePropertySources propertySources) {
		final Set<String> names = new HashSet<String>();
		for (PropertySource<?> propertySource : propertySources) {
			names.add(propertySource.getName());
		}
		for (String name : names) {
			propertySources.remove(name);
		}
	}

}
