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

package org.springframework.boot.logging.logback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.util.PropertySetter;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelUtil;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.AggregationType;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.CollectionFactory;
import org.springframework.core.NativeDetector;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Extended version of the Logback {@link JoranConfigurator} that adds additional Spring
 * Boot rules.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SpringBootJoranConfigurator extends JoranConfigurator {

	private final LoggingInitializationContext initializationContext;

	/**
	 * Constructs a new SpringBootJoranConfigurator with the specified
	 * LoggingInitializationContext.
	 * @param initializationContext the LoggingInitializationContext to be used for
	 * initialization
	 */
	SpringBootJoranConfigurator(LoggingInitializationContext initializationContext) {
		this.initializationContext = initializationContext;
	}

	/**
	 * Performs a sanity check on the given top model.
	 * @param topModel the top model to perform the sanity check on
	 */
	@Override
	protected void sanityCheck(Model topModel) {
		super.sanityCheck(topModel);
		performCheck(new SpringProfileIfNestedWithinSecondPhaseElementSanityChecker(), topModel);
	}

	/**
	 * Adds model handler associations to the given default processor.
	 * @param defaultProcessor the default processor to add model handler associations to
	 */
	@Override
	protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
		defaultProcessor.addHandler(SpringPropertyModel.class,
				(handlerContext, handlerMic) -> new SpringPropertyModelHandler(this.context,
						this.initializationContext.getEnvironment()));
		defaultProcessor.addHandler(SpringProfileModel.class,
				(handlerContext, handlerMic) -> new SpringProfileModelHandler(this.context,
						this.initializationContext.getEnvironment()));
		super.addModelHandlerAssociations(defaultProcessor);
	}

	/**
	 * Adds element selector and action associations to the given rule store.
	 * @param ruleStore the rule store to add the associations to
	 */
	@Override
	public void addElementSelectorAndActionAssociations(RuleStore ruleStore) {
		super.addElementSelectorAndActionAssociations(ruleStore);
		ruleStore.addRule(new ElementSelector("configuration/springProperty"), SpringPropertyAction::new);
		ruleStore.addRule(new ElementSelector("*/springProfile"), SpringProfileAction::new);
		ruleStore.addTransparentPathPart("springProfile");
	}

	/**
	 * Configures the application using the Ahead-of-Time (AOT) generated artifacts.
	 * @return {@code true} if the configuration was successful, {@code false} otherwise.
	 */
	boolean configureUsingAotGeneratedArtifacts() {
		if (!new PatternRules(getContext()).load()) {
			return false;
		}
		Model model = new ModelReader().read();
		processModel(model);
		registerSafeConfiguration(model);
		return true;
	}

	/**
	 * This method processes the given model by calling the superclass's processModel
	 * method. If the application is not running in a native image and AOT processing is
	 * in progress, it adds a LogbackConfigurationAotContribution object to the context's
	 * object map.
	 * @param model The model to be processed
	 */
	@Override
	public void processModel(Model model) {
		super.processModel(model);
		if (!NativeDetector.inNativeImage() && isAotProcessingInProgress()) {
			getContext().putObject(BeanFactoryInitializationAotContribution.class.getName(),
					new LogbackConfigurationAotContribution(model, getModelInterpretationContext(), getContext()));
		}
	}

	/**
	 * Checks if the Ahead-of-Time (AOT) processing is in progress.
	 * @return {@code true} if AOT processing is in progress, {@code false} otherwise.
	 */
	private boolean isAotProcessingInProgress() {
		return Boolean.getBoolean("spring.aot.processing");
	}

	/**
	 * LogbackConfigurationAotContribution class.
	 */
	static final class LogbackConfigurationAotContribution implements BeanFactoryInitializationAotContribution {

		private final ModelWriter modelWriter;

		private final PatternRules patternRules;

		/**
		 * Constructs a new instance of LogbackConfigurationAotContribution with the
		 * specified parameters.
		 * @param model the model to be used
		 * @param interpretationContext the interpretation context to be used
		 * @param context the context to be used
		 */
		private LogbackConfigurationAotContribution(Model model, ModelInterpretationContext interpretationContext,
				Context context) {
			this.modelWriter = new ModelWriter(model, interpretationContext);
			this.patternRules = new PatternRules(context);
		}

		/**
		 * Applies the Logback configuration AOT contribution to the given generation
		 * context and bean factory initialization code.
		 * @param generationContext The generation context to apply the contribution to.
		 * @param beanFactoryInitializationCode The bean factory initialization code to
		 * apply the contribution to.
		 */
		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			this.modelWriter.writeTo(generationContext);
			this.patternRules.save(generationContext);
		}

	}

	/**
	 * ModelWriter class.
	 */
	private static final class ModelWriter {

		private static final String MODEL_RESOURCE_LOCATION = "META-INF/spring/logback-model";

		private final Model model;

		private final ModelInterpretationContext modelInterpretationContext;

		/**
		 * Constructs a new ModelWriter with the specified model and
		 * modelInterpretationContext.
		 * @param model the model to be written
		 * @param modelInterpretationContext the context for interpreting the model
		 */
		private ModelWriter(Model model, ModelInterpretationContext modelInterpretationContext) {
			this.model = model;
			this.modelInterpretationContext = modelInterpretationContext;
		}

		/**
		 * Writes the model to the specified generation context.
		 * @param generationContext The generation context to write the model to.
		 * @throws RuntimeException if an IOException occurs during the writing process.
		 */
		private void writeTo(GenerationContext generationContext) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
				output.writeObject(this.model);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			Resource modelResource = new ByteArrayResource(bytes.toByteArray());
			generationContext.getGeneratedFiles().addResourceFile(MODEL_RESOURCE_LOCATION, modelResource);
			generationContext.getRuntimeHints().resources().registerPattern(MODEL_RESOURCE_LOCATION);
			SerializationHints serializationHints = generationContext.getRuntimeHints().serialization();
			serializationTypes(this.model).forEach(serializationHints::registerType);
			reflectionTypes(this.model).forEach((type) -> generationContext.getRuntimeHints()
				.reflection()
				.registerType(TypeReference.of(type), MemberCategory.INTROSPECT_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		}

		/**
		 * Returns a set of classes that are serializable and are used in the given model
		 * and its submodels.
		 * @param model the model for which to find the serializable classes
		 * @return a set of classes that are serializable and are used in the given model
		 * and its submodels
		 */
		@SuppressWarnings("unchecked")
		private Set<Class<? extends Serializable>> serializationTypes(Model model) {
			Set<Class<? extends Serializable>> modelClasses = new HashSet<>();
			Class<?> candidate = model.getClass();
			while (Model.class.isAssignableFrom(candidate)) {
				if (modelClasses.add((Class<? extends Model>) candidate)) {
					ReflectionUtils.doWithFields(candidate, (field) -> {
						if (Modifier.isStatic(field.getModifiers())) {
							return;
						}
						ReflectionUtils.makeAccessible(field);
						Object value = field.get(model);
						if (value != null) {
							Class<?> fieldType = value.getClass();
							if (Serializable.class.isAssignableFrom(fieldType)) {
								modelClasses.add((Class<? extends Serializable>) fieldType);
							}
						}
					});
					candidate = candidate.getSuperclass();
				}
			}
			for (Model submodel : model.getSubModels()) {
				modelClasses.addAll(serializationTypes(submodel));
			}
			return modelClasses;
		}

		/**
		 * Returns a set of reflection types based on the given model.
		 * @param model the model to retrieve reflection types from
		 * @return a set of reflection types
		 */
		private Set<String> reflectionTypes(Model model) {
			return reflectionTypes(model, () -> null);
		}

		/**
		 * Returns a set of reflection types for the given model and parent supplier.
		 * @param model the model to process
		 * @param parent the parent supplier
		 * @return a set of reflection types
		 */
		private Set<String> reflectionTypes(Model model, Supplier<Object> parent) {
			Set<String> reflectionTypes = new HashSet<>();
			Class<?> componentType = determineType(model, parent);
			if (componentType != null) {
				processComponent(componentType, reflectionTypes);
			}
			Supplier<Object> componentSupplier = SingletonSupplier.ofNullable(() -> instantiate(componentType));
			for (Model submodel : model.getSubModels()) {
				reflectionTypes.addAll(reflectionTypes(submodel, componentSupplier));
			}
			return reflectionTypes;
		}

		/**
		 * Determines the type of the model based on the provided model and parent
		 * supplier.
		 * @param model the model to determine the type for
		 * @param parentSupplier the supplier for the parent object
		 * @return the determined type of the model, or null if the type cannot be
		 * determined
		 */
		private Class<?> determineType(Model model, Supplier<Object> parentSupplier) {
			String className = (model instanceof ComponentModel componentModel) ? componentModel.getClassName() : null;
			if (className != null) {
				return loadImportType(className);
			}
			String tag = model.getTag();
			if (tag != null) {
				className = this.modelInterpretationContext.getDefaultNestedComponentRegistry()
					.findDefaultComponentTypeByTag(tag);
				if (className != null) {
					return loadImportType(className);
				}
				return inferTypeFromParent(parentSupplier, tag);
			}
			return null;
		}

		/**
		 * Loads the import type for the given class name.
		 * @param className the name of the class to load the import type for
		 * @return the loaded import type as a Class object
		 */
		private Class<?> loadImportType(String className) {
			return loadComponentType(this.modelInterpretationContext.getImport(className));
		}

		/**
		 * Infers the type from the parent object using the given supplier and tag.
		 * @param parentSupplier the supplier for obtaining the parent object
		 * @param tag the tag used for inferring the type
		 * @return the inferred type from the parent object, or null if unable to infer
		 */
		private Class<?> inferTypeFromParent(Supplier<Object> parentSupplier, String tag) {
			Object parent = parentSupplier.get();
			if (parent != null) {
				try {
					PropertySetter propertySetter = new PropertySetter(
							this.modelInterpretationContext.getBeanDescriptionCache(), parent);
					Class<?> typeFromPropertySetter = propertySetter.getClassNameViaImplicitRules(tag,
							AggregationType.AS_COMPLEX_PROPERTY,
							this.modelInterpretationContext.getDefaultNestedComponentRegistry());
					return typeFromPropertySetter;
				}
				catch (Exception ex) {
					return null;
				}
			}
			return null;
		}

		/**
		 * Loads the component type based on the given component type string.
		 * @param componentType the component type string
		 * @return the loaded component type class
		 * @throws RuntimeException if failed to load the component type
		 */
		private Class<?> loadComponentType(String componentType) {
			try {
				return ClassUtils.forName(this.modelInterpretationContext.subst(componentType),
						getClass().getClassLoader());
			}
			catch (Throwable ex) {
				throw new RuntimeException("Failed to load component type '" + componentType + "'", ex);
			}
		}

		/**
		 * Instantiates an object of the specified type.
		 * @param type the class type of the object to be instantiated
		 * @return an instance of the specified type, or null if instantiation fails
		 */
		private Object instantiate(Class<?> type) {
			try {
				return type.getConstructor().newInstance();
			}
			catch (Exception ex) {
				return null;
			}
		}

		/**
		 * Processes a component by retrieving its bean description from the bean
		 * description cache and adding the parameter types of its adder and setter
		 * methods, as well as the component type itself, to the set of reflection types.
		 * @param componentType the class representing the component
		 * @param reflectionTypes the set of reflection types to be updated
		 */
		private void processComponent(Class<?> componentType, Set<String> reflectionTypes) {
			BeanDescription beanDescription = this.modelInterpretationContext.getBeanDescriptionCache()
				.getBeanDescription(componentType);
			reflectionTypes.addAll(parameterTypesNames(beanDescription.getPropertyNameToAdder().values()));
			reflectionTypes.addAll(parameterTypesNames(beanDescription.getPropertyNameToSetter().values()));
			reflectionTypes.add(componentType.getCanonicalName());
		}

		/**
		 * Returns a collection of parameter type names for the given methods.
		 * @param methods the collection of methods to extract parameter type names from
		 * @return a collection of parameter type names
		 */
		private Collection<String> parameterTypesNames(Collection<Method> methods) {
			return methods.stream()
				.filter((method) -> !method.getDeclaringClass().equals(ContextAware.class)
						&& !method.getDeclaringClass().equals(ContextAwareBase.class))
				.map(Method::getParameterTypes)
				.flatMap(Stream::of)
				.filter((type) -> !type.isPrimitive() && !type.equals(String.class))
				.map((type) -> type.isArray() ? type.getComponentType() : type)
				.map(Class::getName)
				.toList();
		}

	}

	/**
	 * ModelReader class.
	 */
	private static final class ModelReader {

		/**
		 * Reads and returns a Model object from the specified resource location.
		 * @return The Model object read from the resource location.
		 * @throws RuntimeException if there is an error loading the model from the
		 * resource location.
		 */
		private Model read() {
			try (InputStream modelInput = getClass().getClassLoader()
				.getResourceAsStream(ModelWriter.MODEL_RESOURCE_LOCATION)) {
				try (ObjectInputStream input = new ObjectInputStream(modelInput)) {
					Model model = (Model) input.readObject();
					ModelUtil.resetForReuse(model);
					return model;
				}
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to load model from '" + ModelWriter.MODEL_RESOURCE_LOCATION + "'",
						ex);
			}
		}

	}

	/**
	 * PatternRules class.
	 */
	private static final class PatternRules {

		private static final String RESOURCE_LOCATION = "META-INF/spring/logback-pattern-rules";

		private final Context context;

		/**
		 * Constructs a new instance of the PatternRules class with the specified context.
		 * @param context the context to be used by the PatternRules class
		 */
		private PatternRules(Context context) {
			this.context = context;
		}

		/**
		 * Loads the pattern rules from a resource file.
		 * @return true if the pattern rules were successfully loaded, false otherwise
		 * @throws RuntimeException if an exception occurs during the loading process
		 */
		private boolean load() {
			try {
				ClassPathResource resource = new ClassPathResource(RESOURCE_LOCATION);
				if (!resource.exists()) {
					return false;
				}
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				Map<String, String> patternRuleRegistry = getRegistryMap();
				for (String word : properties.stringPropertyNames()) {
					patternRuleRegistry.put(word, properties.getProperty(word));
				}
				return true;
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		/**
		 * Retrieves the registry map for pattern rules.
		 * @return The registry map containing pattern rules.
		 */
		@SuppressWarnings("unchecked")
		private Map<String, String> getRegistryMap() {
			Map<String, String> patternRuleRegistry = (Map<String, String>) this.context
				.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
			if (patternRuleRegistry == null) {
				patternRuleRegistry = new HashMap<>();
				this.context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, patternRuleRegistry);
			}
			return patternRuleRegistry;
		}

		/**
		 * Saves the generation context by adding the registry map as a resource file and
		 * registering it as a pattern. Additionally, it registers the rule class names in
		 * the reflection hints for invoking public constructors.
		 * @param generationContext The generation context to be saved
		 */
		private void save(GenerationContext generationContext) {
			Map<String, String> registryMap = getRegistryMap();
			generationContext.getGeneratedFiles().addResourceFile(RESOURCE_LOCATION, () -> asInputStream(registryMap));
			generationContext.getRuntimeHints().resources().registerPattern(RESOURCE_LOCATION);
			for (String ruleClassName : registryMap.values()) {
				generationContext.getRuntimeHints()
					.reflection()
					.registerType(TypeReference.of(ruleClassName), MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			}
		}

		/**
		 * Converts a map of pattern rule registry into an input stream.
		 * @param patternRuleRegistry the map of pattern rule registry
		 * @return an input stream representing the pattern rule registry
		 * @throws RuntimeException if an IO exception occurs during the conversion
		 * process
		 */
		private InputStream asInputStream(Map<String, String> patternRuleRegistry) {
			Properties properties = CollectionFactory.createSortedProperties(true);
			patternRuleRegistry.forEach(properties::setProperty);
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try {
				properties.store(bytes, "");
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			return new ByteArrayInputStream(bytes.toByteArray());
		}

	}

}
