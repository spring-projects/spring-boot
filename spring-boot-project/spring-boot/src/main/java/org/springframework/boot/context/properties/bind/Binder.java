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

package org.springframework.boot.context.properties.bind;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.bind.Bindable.BindRestriction;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.env.Environment;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;

/**
 * A container object which Binds objects from one or more
 * {@link ConfigurationPropertySource ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class Binder {

	private static final Set<Class<?>> NON_BEAN_CLASSES = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList(Object.class, Class.class)));

	private final Iterable<ConfigurationPropertySource> sources;

	private final PlaceholdersResolver placeholdersResolver;

	private final BindConverter bindConverter;

	private final BindHandler defaultBindHandler;

	private final Map<BindMethod, List<DataObjectBinder>> dataObjectBinders;

	/**
	 * Create a new {@link Binder} instance for the specified sources. A
	 * {@link DefaultFormattingConversionService} will be used for all conversion.
	 * @param sources the sources used for binding
	 */
	public Binder(ConfigurationPropertySource... sources) {
		this((sources != null) ? Arrays.asList(sources) : null, null, null, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources. A
	 * {@link DefaultFormattingConversionService} will be used for all conversion.
	 * @param sources the sources used for binding
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources) {
		this(sources, null, null, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property placeholders
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver) {
		this(sources, placeholdersResolver, null, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property placeholders
	 * @param conversionService the conversion service to convert values (or {@code null}
	 * to use {@link ApplicationConversionService})
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver,
			ConversionService conversionService) {
		this(sources, placeholdersResolver, conversionService, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property placeholders
	 * @param conversionService the conversion service to convert values (or {@code null}
	 * to use {@link ApplicationConversionService})
	 * @param propertyEditorInitializer initializer used to configure the property editors
	 * that can convert values (or {@code null} if no initialization is required). Often
	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver,
			ConversionService conversionService, Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		this(sources, placeholdersResolver, conversionService, propertyEditorInitializer, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property placeholders
	 * @param conversionService the conversion service to convert values (or {@code null}
	 * to use {@link ApplicationConversionService})
	 * @param propertyEditorInitializer initializer used to configure the property editors
	 * that can convert values (or {@code null} if no initialization is required). Often
	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
	 * @param defaultBindHandler the default bind handler to use if none is specified when
	 * binding
	 * @since 2.2.0
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver,
			ConversionService conversionService, Consumer<PropertyEditorRegistry> propertyEditorInitializer,
			BindHandler defaultBindHandler) {
		this(sources, placeholdersResolver, conversionService, propertyEditorInitializer, defaultBindHandler, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property placeholders
	 * @param conversionService the conversion service to convert values (or {@code null}
	 * to use {@link ApplicationConversionService})
	 * @param propertyEditorInitializer initializer used to configure the property editors
	 * that can convert values (or {@code null} if no initialization is required). Often
	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
	 * @param defaultBindHandler the default bind handler to use if none is specified when
	 * binding
	 * @param constructorProvider the constructor provider which provides the bind
	 * constructor to use when binding
	 * @since 2.2.1
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver,
			ConversionService conversionService, Consumer<PropertyEditorRegistry> propertyEditorInitializer,
			BindHandler defaultBindHandler, BindConstructorProvider constructorProvider) {
		this(sources, placeholdersResolver,
				(conversionService != null) ? Collections.singletonList(conversionService)
						: (List<ConversionService>) null,
				propertyEditorInitializer, defaultBindHandler, constructorProvider);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property placeholders
	 * @param conversionServices the conversion services to convert values (or
	 * {@code null} to use {@link ApplicationConversionService})
	 * @param propertyEditorInitializer initializer used to configure the property editors
	 * that can convert values (or {@code null} if no initialization is required). Often
	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
	 * @param defaultBindHandler the default bind handler to use if none is specified when
	 * binding
	 * @param constructorProvider the constructor provider which provides the bind
	 * constructor to use when binding
	 * @since 2.5.0
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources, PlaceholdersResolver placeholdersResolver,
			List<ConversionService> conversionServices, Consumer<PropertyEditorRegistry> propertyEditorInitializer,
			BindHandler defaultBindHandler, BindConstructorProvider constructorProvider) {
		Assert.notNull(sources, "Sources must not be null");
		for (ConfigurationPropertySource source : sources) {
			Assert.notNull(source, "Sources must not contain null elements");
		}
		this.sources = sources;
		this.placeholdersResolver = (placeholdersResolver != null) ? placeholdersResolver : PlaceholdersResolver.NONE;
		this.bindConverter = BindConverter.get(conversionServices, propertyEditorInitializer);
		this.defaultBindHandler = (defaultBindHandler != null) ? defaultBindHandler : BindHandler.DEFAULT;
		if (constructorProvider == null) {
			constructorProvider = BindConstructorProvider.DEFAULT;
		}
		ValueObjectBinder valueObjectBinder = new ValueObjectBinder(constructorProvider);
		JavaBeanBinder javaBeanBinder = JavaBeanBinder.INSTANCE;
		Map<BindMethod, List<DataObjectBinder>> dataObjectBinders = new HashMap<>();
		dataObjectBinders.put(BindMethod.VALUE_OBJECT, List.of(valueObjectBinder));
		dataObjectBinders.put(BindMethod.JAVA_BEAN, List.of(javaBeanBinder));
		dataObjectBinders.put(null, List.of(valueObjectBinder, javaBeanBinder));
		this.dataObjectBinders = Collections.unmodifiableMap(dataObjectBinders);
	}

	/**
	 * Bind the specified target {@link Class} using this binder's
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target class
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> BindResult<T> bind(String name, Class<T> target) {
		return bind(name, Bindable.of(target));
	}

	/**
	 * Bind the specified target {@link Bindable} using this binder's
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> BindResult<T> bind(String name, Bindable<T> target) {
		return bind(ConfigurationPropertyName.of(name), target, null);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binder's
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target) {
		return bind(name, target, null);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binder's
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param handler the bind handler (may be {@code null})
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 */
	public <T> BindResult<T> bind(String name, Bindable<T> target, BindHandler handler) {
		return bind(ConfigurationPropertyName.of(name), target, handler);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binder's
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param handler the bind handler (may be {@code null})
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 */
	public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler) {
		T bound = bind(name, target, handler, false);
		return BindResult.of(bound);
	}

	/**
	 * Bind the specified target {@link Class} using this binder's
	 * {@link ConfigurationPropertySource property sources} or create a new instance using
	 * the type of the {@link Bindable} if the result of the binding is {@code null}.
	 * @param name the configuration property name to bind
	 * @param target the target class
	 * @param <T> the bound type
	 * @return the bound or created object
	 * @since 2.2.0
	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> T bindOrCreate(String name, Class<T> target) {
		return bindOrCreate(name, Bindable.of(target));
	}

	/**
	 * Bind the specified target {@link Bindable} using this binder's
	 * {@link ConfigurationPropertySource property sources} or create a new instance using
	 * the type of the {@link Bindable} if the result of the binding is {@code null}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param <T> the bound type
	 * @return the bound or created object
	 * @since 2.2.0
	 * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> T bindOrCreate(String name, Bindable<T> target) {
		return bindOrCreate(ConfigurationPropertyName.of(name), target, null);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binder's
	 * {@link ConfigurationPropertySource property sources} or create a new instance using
	 * the type of the {@link Bindable} if the result of the binding is {@code null}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param handler the bind handler
	 * @param <T> the bound type
	 * @return the bound or created object
	 * @since 2.2.0
	 * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> T bindOrCreate(String name, Bindable<T> target, BindHandler handler) {
		return bindOrCreate(ConfigurationPropertyName.of(name), target, handler);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binder's
	 * {@link ConfigurationPropertySource property sources} or create a new instance using
	 * the type of the {@link Bindable} if the result of the binding is {@code null}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param handler the bind handler (may be {@code null})
	 * @param <T> the bound or created type
	 * @return the bound or created object
	 * @since 2.2.0
	 */
	public <T> T bindOrCreate(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler) {
		return bind(name, target, handler, true);
	}

	/**
     * Binds the given configuration property name to the specified target using the provided bind handler.
     * 
     * @param name the configuration property name to bind
     * @param target the target to bind the configuration property to
     * @param handler the bind handler to use for binding
     * @param create a boolean indicating whether to create the target if it does not exist
     * @return the bound configuration property value
     * @throws IllegalArgumentException if the name or target is null
     */
    private <T> T bind(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler, boolean create) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(target, "Target must not be null");
		handler = (handler != null) ? handler : this.defaultBindHandler;
		Context context = new Context();
		return bind(name, target, handler, context, false, create);
	}

	/**
     * Binds the given configuration property name to the specified target using the provided bind handler and context.
     * 
     * @param <T> the type of the target object
     * @param name the configuration property name to bind
     * @param target the target object to bind the configuration property to
     * @param handler the bind handler to use for binding
     * @param context the context for the binding operation
     * @param allowRecursiveBinding flag indicating whether recursive binding is allowed
     * @param create flag indicating whether to create the target object if it does not exist
     * @return the bound object
     */
    private <T> T bind(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler, Context context,
			boolean allowRecursiveBinding, boolean create) {
		try {
			Bindable<T> replacementTarget = handler.onStart(name, target, context);
			if (replacementTarget == null) {
				return handleBindResult(name, target, handler, context, null, create);
			}
			target = replacementTarget;
			Object bound = bindObject(name, target, handler, context, allowRecursiveBinding);
			return handleBindResult(name, target, handler, context, bound, create);
		}
		catch (Exception ex) {
			return handleBindError(name, target, handler, context, ex);
		}
	}

	/**
     * Handles the result of a binding operation.
     * 
     * @param <T> the type of the result
     * @param name the name of the configuration property being bound
     * @param target the bindable target object
     * @param handler the bind handler
     * @param context the binding context
     * @param result the result of the binding operation
     * @param create a flag indicating whether to create a new instance if the result is null
     * @return the converted result
     * @throws Exception if an error occurs during the binding operation
     */
    private <T> T handleBindResult(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
			Context context, Object result, boolean create) throws Exception {
		if (result != null) {
			result = handler.onSuccess(name, target, context, result);
			result = context.getConverter().convert(result, target);
		}
		if (result == null && create) {
			result = fromDataObjectBinders(target.getBindMethod(),
					(dataObjectBinder) -> dataObjectBinder.create(target, context));
			result = handler.onCreate(name, target, context, result);
			result = context.getConverter().convert(result, target);
			if (result == null) {
				IllegalStateException ex = new IllegalStateException(
						"Unable to create instance for " + target.getType());
				this.dataObjectBinders.get(target.getBindMethod())
					.forEach((dataObjectBinder) -> dataObjectBinder.onUnableToCreateInstance(target, context, ex));
				throw ex;
			}
		}
		handler.onFinish(name, target, context, result);
		return context.getConverter().convert(result, target);
	}

	/**
     * Handles a binding error by invoking the appropriate handler and converting the result.
     *
     * @param name     the name of the configuration property
     * @param target   the bindable target
     * @param handler  the bind handler
     * @param context  the binding context
     * @param error    the exception that occurred during binding
     * @param <T>      the type of the bindable target
     * @return         the converted result after handling the error
     * @throws BindException if the error cannot be handled or converted
     */
    private <T> T handleBindError(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
			Context context, Exception error) {
		try {
			Object result = handler.onFailure(name, target, context, error);
			return context.getConverter().convert(result, target);
		}
		catch (Exception ex) {
			if (ex instanceof BindException bindException) {
				throw bindException;
			}
			throw new BindException(name, target, context.getConfigurationProperty(), ex);
		}
	}

	/**
     * Binds the given configuration property name to the target object using the provided bind handler, context, and
     * recursive binding flag.
     *
     * @param <T>                     the type of the target object
     * @param name                    the configuration property name to bind
     * @param target                  the target object to bind the configuration property to
     * @param handler                 the bind handler to use during the binding process
     * @param context                 the context for the binding process
     * @param allowRecursiveBinding   flag indicating whether recursive binding is allowed
     * @return                        the bound object, or null if the property is not found and the context depth is not 0
     * @throws ConverterNotFoundException if a converter is not found for the property and recursive binding is not allowed
     */
    private <T> Object bindObject(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
			Context context, boolean allowRecursiveBinding) {
		ConfigurationProperty property = findProperty(name, target, context);
		if (property == null && context.depth != 0 && containsNoDescendantOf(context.getSources(), name)) {
			return null;
		}
		AggregateBinder<?> aggregateBinder = getAggregateBinder(target, context);
		if (aggregateBinder != null) {
			return bindAggregate(name, target, handler, context, aggregateBinder);
		}
		if (property != null) {
			try {
				return bindProperty(target, context, property);
			}
			catch (ConverterNotFoundException ex) {
				// We might still be able to bind it using the recursive binders
				Object instance = bindDataObject(name, target, handler, context, allowRecursiveBinding);
				if (instance != null) {
					return instance;
				}
				throw ex;
			}
		}
		return bindDataObject(name, target, handler, context, allowRecursiveBinding);
	}

	/**
     * Returns the appropriate AggregateBinder based on the type of the target Bindable and the provided context.
     * 
     * @param target the target Bindable object
     * @param context the context object
     * @return the appropriate AggregateBinder based on the type of the target Bindable and the provided context, or null if no appropriate binder is found
     */
    private AggregateBinder<?> getAggregateBinder(Bindable<?> target, Context context) {
		Class<?> resolvedType = target.getType().resolve(Object.class);
		if (Map.class.isAssignableFrom(resolvedType)) {
			return new MapBinder(context);
		}
		if (Collection.class.isAssignableFrom(resolvedType)) {
			return new CollectionBinder(context);
		}
		if (target.getType().isArray()) {
			return new ArrayBinder(context);
		}
		return null;
	}

	/**
     * Binds an aggregate configuration property to a target object using the provided binder and handler.
     * 
     * @param <T> the type of the target object
     * @param name the name of the configuration property
     * @param target the target object to bind the configuration property to
     * @param handler the bind handler to use for binding
     * @param context the context for the binding operation
     * @param aggregateBinder the aggregate binder to use for binding
     * @return the bound object
     */
    private <T> Object bindAggregate(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
			Context context, AggregateBinder<?> aggregateBinder) {
		AggregateElementBinder elementBinder = (itemName, itemTarget, source) -> {
			boolean allowRecursiveBinding = aggregateBinder.isAllowRecursiveBinding(source);
			Supplier<?> supplier = () -> bind(itemName, itemTarget, handler, context, allowRecursiveBinding, false);
			return context.withSource(source, supplier);
		};
		return context.withIncreasedDepth(() -> aggregateBinder.bind(name, target, elementBinder));
	}

	/**
     * Finds a configuration property with the given name and target bindable in the given context.
     * 
     * @param name the name of the configuration property
     * @param target the target bindable
     * @param context the context containing the configuration property sources
     * @return the configuration property if found, or null if not found
     */
    private <T> ConfigurationProperty findProperty(ConfigurationPropertyName name, Bindable<T> target,
			Context context) {
		if (name.isEmpty() || target.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)) {
			return null;
		}
		for (ConfigurationPropertySource source : context.getSources()) {
			ConfigurationProperty property = source.getConfigurationProperty(name);
			if (property != null) {
				return property;
			}
		}
		return null;
	}

	/**
     * Binds a property to a target object using the provided context and configuration property.
     * 
     * @param <T> the type of the property value
     * @param target the target object to bind the property to
     * @param context the context used for binding
     * @param property the configuration property to bind
     * @return the bound property value
     */
    private <T> Object bindProperty(Bindable<T> target, Context context, ConfigurationProperty property) {
		context.setConfigurationProperty(property);
		Object result = property.getValue();
		result = this.placeholdersResolver.resolvePlaceholders(result);
		result = context.getConverter().convert(result, target);
		return result;
	}

	/**
     * Binds data to the specified target object using the given name, bind handler, context, and recursive binding flag.
     * 
     * @param name The configuration property name to bind data from.
     * @param target The target object to bind data to.
     * @param handler The bind handler to use for binding.
     * @param context The context for the binding operation.
     * @param allowRecursiveBinding Flag indicating whether recursive binding is allowed.
     * @return The bound data object, or null if the target object is unbindable or recursive binding is not allowed.
     */
    private Object bindDataObject(ConfigurationPropertyName name, Bindable<?> target, BindHandler handler,
			Context context, boolean allowRecursiveBinding) {
		if (isUnbindableBean(name, target, context)) {
			return null;
		}
		Class<?> type = target.getType().resolve(Object.class);
		BindMethod bindMethod = target.getBindMethod();
		if (!allowRecursiveBinding && context.isBindingDataObject(type)) {
			return null;
		}
		DataObjectPropertyBinder propertyBinder = (propertyName, propertyTarget) -> bind(name.append(propertyName),
				propertyTarget, handler, context, false, false);
		return context.withDataObject(type, () -> fromDataObjectBinders(bindMethod,
				(dataObjectBinder) -> dataObjectBinder.bind(name, target, context, propertyBinder)));
	}

	/**
     * Binds the given data object using the specified bind method and applies the provided operation on the data object binder.
     * 
     * @param bindMethod The bind method to be used for binding the data object.
     * @param operation The operation to be applied on the data object binder.
     * @return The result of the operation applied on the data object binder, or null if no result is found.
     */
    private Object fromDataObjectBinders(BindMethod bindMethod, Function<DataObjectBinder, Object> operation) {
		return this.dataObjectBinders.get(bindMethod)
			.stream()
			.map(operation)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
     * Determines if a bean can be unbound based on the given configuration property name, target bindable, and context.
     * 
     * @param name the configuration property name
     * @param target the target bindable
     * @param context the context
     * @return {@code true} if the bean can be unbound, {@code false} otherwise
     */
    private boolean isUnbindableBean(ConfigurationPropertyName name, Bindable<?> target, Context context) {
		for (ConfigurationPropertySource source : context.getSources()) {
			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
				// We know there are properties to bind so we can't bypass anything
				return false;
			}
		}
		Class<?> resolved = target.getType().resolve(Object.class);
		if (resolved.isPrimitive() || NON_BEAN_CLASSES.contains(resolved)) {
			return true;
		}
		return resolved.getName().startsWith("java.");
	}

	/**
     * Checks if none of the given configuration property sources contain any descendant of the specified configuration property name.
     * 
     * @param sources the iterable collection of configuration property sources to check
     * @param name the configuration property name to check for descendants
     * @return {@code true} if none of the sources contain any descendant of the specified name, {@code false} otherwise
     */
    private boolean containsNoDescendantOf(Iterable<ConfigurationPropertySource> sources,
			ConfigurationPropertyName name) {
		for (ConfigurationPropertySource source : sources) {
			if (source.containsDescendantOf(name) != ConfigurationPropertyState.ABSENT) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Create a new {@link Binder} instance from the specified environment.
	 * @param environment the environment source (must have attached
	 * {@link ConfigurationPropertySources})
	 * @return a {@link Binder} instance
	 */
	public static Binder get(Environment environment) {
		return get(environment, null);
	}

	/**
	 * Create a new {@link Binder} instance from the specified environment.
	 * @param environment the environment source (must have attached
	 * {@link ConfigurationPropertySources})
	 * @param defaultBindHandler the default bind handler to use if none is specified when
	 * binding
	 * @return a {@link Binder} instance
	 * @since 2.2.0
	 */
	public static Binder get(Environment environment, BindHandler defaultBindHandler) {
		Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
		PropertySourcesPlaceholdersResolver placeholdersResolver = new PropertySourcesPlaceholdersResolver(environment);
		return new Binder(sources, placeholdersResolver, null, null, defaultBindHandler);
	}

	/**
	 * Context used when binding and the {@link BindContext} implementation.
	 */
	final class Context implements BindContext {

		private int depth;

		private final List<ConfigurationPropertySource> source = Arrays.asList((ConfigurationPropertySource) null);

		private int sourcePushCount;

		private final Deque<Class<?>> dataObjectBindings = new ArrayDeque<>();

		private final Deque<Class<?>> constructorBindings = new ArrayDeque<>();

		private ConfigurationProperty configurationProperty;

		/**
         * Increases the depth of the context.
         * This method increments the depth variable by 1.
         */
        private void increaseDepth() {
			this.depth++;
		}

		/**
         * Decreases the depth of the context.
         * This method decrements the depth variable by 1.
         */
        private void decreaseDepth() {
			this.depth--;
		}

		/**
         * Executes the given supplier function with the provided ConfigurationPropertySource.
         * If the source is null, the supplier function is executed without any source.
         * 
         * @param source The ConfigurationPropertySource to be used during the execution of the supplier function.
         * @param supplier The supplier function to be executed.
         * @param <T> The type of the result returned by the supplier function.
         * @return The result returned by the supplier function.
         */
        private <T> T withSource(ConfigurationPropertySource source, Supplier<T> supplier) {
			if (source == null) {
				return supplier.get();
			}
			this.source.set(0, source);
			this.sourcePushCount++;
			try {
				return supplier.get();
			}
			finally {
				this.sourcePushCount--;
			}
		}

		/**
         * Executes the given supplier function with a data object of the specified type.
         * 
         * @param type the class representing the type of the data object
         * @param supplier the supplier function to be executed with the data object
         * @param <T> the type of the data object
         * @return the result of executing the supplier function
         */
        private <T> T withDataObject(Class<?> type, Supplier<T> supplier) {
			this.dataObjectBindings.push(type);
			try {
				return withIncreasedDepth(supplier);
			}
			finally {
				this.dataObjectBindings.pop();
			}
		}

		/**
         * Checks if the given class is a binding data object.
         * 
         * @param type the class to check
         * @return true if the class is a binding data object, false otherwise
         */
        private boolean isBindingDataObject(Class<?> type) {
			return this.dataObjectBindings.contains(type);
		}

		/**
         * Increases the depth of the context and executes the provided supplier.
         * 
         * @param supplier the supplier to be executed
         * @param <T> the type of the result returned by the supplier
         * @return the result returned by the supplier
         */
        private <T> T withIncreasedDepth(Supplier<T> supplier) {
			increaseDepth();
			try {
				return supplier.get();
			}
			finally {
				decreaseDepth();
			}
		}

		/**
         * Sets the configuration property for the context.
         * 
         * @param configurationProperty the configuration property to be set
         */
        void setConfigurationProperty(ConfigurationProperty configurationProperty) {
			this.configurationProperty = configurationProperty;
		}

		/**
         * Clears the configuration property by setting it to null.
         */
        void clearConfigurationProperty() {
			this.configurationProperty = null;
		}

		/**
         * Pushes a constructor bound type onto the stack of constructor bindings.
         * 
         * @param value the class representing the constructor bound type to be pushed
         */
        void pushConstructorBoundTypes(Class<?> value) {
			this.constructorBindings.push(value);
		}

		/**
         * Checks if there are any nested constructor bindings in the context.
         * 
         * @return {@code true} if there are nested constructor bindings, {@code false} otherwise.
         */
        boolean isNestedConstructorBinding() {
			return !this.constructorBindings.isEmpty();
		}

		/**
         * Removes the top element from the constructor bindings stack.
         */
        void popConstructorBoundTypes() {
			this.constructorBindings.pop();
		}

		/**
         * Returns the placeholders resolver used by the binder.
         * 
         * @return the placeholders resolver
         */
        PlaceholdersResolver getPlaceholdersResolver() {
			return Binder.this.placeholdersResolver;
		}

		/**
         * Returns the BindConverter associated with the Binder instance.
         * 
         * @return the BindConverter associated with the Binder instance
         */
        BindConverter getConverter() {
			return Binder.this.bindConverter;
		}

		/**
         * Returns the Binder associated with this Context.
         *
         * @return the Binder associated with this Context
         */
        @Override
		public Binder getBinder() {
			return Binder.this;
		}

		/**
         * Returns the depth of the context.
         *
         * @return the depth of the context
         */
        @Override
		public int getDepth() {
			return this.depth;
		}

		/**
         * Returns an iterable of ConfigurationPropertySource objects.
         * 
         * @return an iterable of ConfigurationPropertySource objects
         */
        @Override
		public Iterable<ConfigurationPropertySource> getSources() {
			if (this.sourcePushCount > 0) {
				return this.source;
			}
			return Binder.this.sources;
		}

		/**
         * Returns the configuration property associated with this context.
         *
         * @return the configuration property
         */
        @Override
		public ConfigurationProperty getConfigurationProperty() {
			return this.configurationProperty;
		}

	}

}
