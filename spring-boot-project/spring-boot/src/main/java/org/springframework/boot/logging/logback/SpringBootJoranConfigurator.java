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
import java.util.stream.Stream;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelUtil;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;

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

/**
 * Extended version of the Logback {@link JoranConfigurator} that adds additional Spring
 * Boot rules.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SpringBootJoranConfigurator extends JoranConfigurator {

	private LoggingInitializationContext initializationContext;

	SpringBootJoranConfigurator(LoggingInitializationContext initializationContext) {
		this.initializationContext = initializationContext;
	}

	@Override
	protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
		super.addModelHandlerAssociations(defaultProcessor);
		defaultProcessor.addHandler(SpringPropertyModel.class,
				(handlerContext, handlerMic) -> new SpringPropertyModelHandler(this.context,
						this.initializationContext.getEnvironment()));
		defaultProcessor.addHandler(SpringProfileModel.class,
				(handlerContext, handlerMic) -> new SpringProfileModelHandler(this.context,
						this.initializationContext.getEnvironment()));
	}

	@Override
	public void addElementSelectorAndActionAssociations(RuleStore ruleStore) {
		super.addElementSelectorAndActionAssociations(ruleStore);
		ruleStore.addRule(new ElementSelector("configuration/springProperty"), SpringPropertyAction::new);
		ruleStore.addRule(new ElementSelector("*/springProfile"), SpringProfileAction::new);
		ruleStore.addTransparentPathPart("springProfile");
	}

	boolean configureUsingAotGeneratedArtifacts() {
		if (!new PatternRules(getContext()).load()) {
			return false;
		}
		Model model = new ModelReader().read();
		processModel(model);
		registerSafeConfiguration(model);
		return true;
	}

	@Override
	public void processModel(Model model) {
		super.processModel(model);
		if (!NativeDetector.inNativeImage() && isAotProcessingInProgress()) {
			getContext().putObject(BeanFactoryInitializationAotContribution.class.getName(),
					new LogbackConfigurationAotContribution(model, getModelInterpretationContext(), getContext()));
		}
	}

	private boolean isAotProcessingInProgress() {
		return Boolean.getBoolean("spring.aot.processing");
	}

	static final class LogbackConfigurationAotContribution implements BeanFactoryInitializationAotContribution {

		private final ModelWriter modelWriter;

		private final PatternRules patternRules;

		private LogbackConfigurationAotContribution(Model model, ModelInterpretationContext interpretationContext,
				Context context) {
			this.modelWriter = new ModelWriter(model, interpretationContext);
			this.patternRules = new PatternRules(context);
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			this.modelWriter.writeTo(generationContext);
			this.patternRules.save(generationContext);
		}

	}

	private static final class ModelWriter {

		private static final String MODEL_RESOURCE_LOCATION = "META-INF/spring/logback-model";

		private final Model model;

		private final ModelInterpretationContext modelInterpretationContext;

		private ModelWriter(Model model, ModelInterpretationContext modelInterpretationContext) {
			this.model = model;
			this.modelInterpretationContext = modelInterpretationContext;
		}

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
			reflectionTypes(this.model).forEach((type) -> generationContext.getRuntimeHints().reflection().registerType(
					TypeReference.of(type), MemberCategory.INTROSPECT_PUBLIC_METHODS,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		}

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

		private Set<String> reflectionTypes(Model model) {
			Set<String> reflectionTypes = new HashSet<>();
			if (model instanceof ComponentModel) {
				String className = ((ComponentModel) model).getClassName();
				processComponent(className, reflectionTypes);
			}
			String tag = model.getTag();
			if (tag != null) {
				String componentType = this.modelInterpretationContext.getDefaultNestedComponentRegistry()
						.findDefaultComponentTypeByTag(tag);
				processComponent(componentType, reflectionTypes);
			}
			for (Model submodel : model.getSubModels()) {
				reflectionTypes.addAll(reflectionTypes(submodel));
			}
			return reflectionTypes;
		}

		private void processComponent(String componentTypeName, Set<String> reflectionTypes) {
			if (componentTypeName != null) {
				componentTypeName = this.modelInterpretationContext.getImport(componentTypeName);
				BeanDescription beanDescription = this.modelInterpretationContext.getBeanDescriptionCache()
						.getBeanDescription(loadComponentType(componentTypeName));
				reflectionTypes.addAll(parameterTypesNames(beanDescription.getPropertyNameToAdder().values()));
				reflectionTypes.addAll(parameterTypesNames(beanDescription.getPropertyNameToSetter().values()));
				reflectionTypes.add(componentTypeName);
			}
		}

		private Class<?> loadComponentType(String componentType) {
			try {
				return ClassUtils.forName(componentType, getClass().getClassLoader());
			}
			catch (Throwable ex) {
				throw new RuntimeException("Failed to load component type '" + componentType + "'", ex);
			}
		}

		private Collection<String> parameterTypesNames(Collection<Method> methods) {
			return methods.stream()
					.filter((method) -> !method.getDeclaringClass().equals(ContextAware.class)
							&& !method.getDeclaringClass().equals(ContextAwareBase.class))
					.map(Method::getParameterTypes).flatMap(Stream::of)
					.filter((type) -> !type.isPrimitive() && !type.equals(String.class)).map(Class::getName).toList();
		}

	}

	private static final class ModelReader {

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

	private static final class PatternRules {

		private static final String RESOURCE_LOCATION = "META-INF/spring/logback-pattern-rules";

		private final Context context;

		private PatternRules(Context context) {
			this.context = context;
		}

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

		private void save(GenerationContext generationContext) {
			Map<String, String> registryMap = getRegistryMap();
			generationContext.getGeneratedFiles().addResourceFile(RESOURCE_LOCATION, () -> asInputStream(registryMap));
			generationContext.getRuntimeHints().resources().registerPattern(RESOURCE_LOCATION);
			for (String ruleClassName : registryMap.values()) {
				generationContext.getRuntimeHints().reflection().registerType(TypeReference.of(ruleClassName),
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			}
		}

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
