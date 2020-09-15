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

package org.springframework.boot.configurationprocessor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.InvalidConfigurationMetadataException;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Annotation {@link Processor} that writes meta-data file for
 * {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Kris De Volder
 * @author Jonas Keßler
 * @since 1.2.0
 */
@SupportedAnnotationTypes({ "*" })
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {

	static final String ADDITIONAL_METADATA_LOCATIONS_OPTION = "org.springframework.boot.configurationprocessor.additionalMetadataLocations";

	static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot.context.properties.ConfigurationProperties";

	static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.NestedConfigurationProperty";

	static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.DeprecatedConfigurationProperty";

	static final String CONSTRUCTOR_BINDING_ANNOTATION = "org.springframework.boot.context.properties.ConstructorBinding";

	static final String DEFAULT_VALUE_ANNOTATION = "org.springframework.boot.context.properties.bind.DefaultValue";

	static final String ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.Endpoint";

	static final String READ_OPERATION_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.ReadOperation";

	static final String NAME_ANNOTATION = "org.springframework.boot.context.properties.bind.Name";

	static final String IMPORT_AS_CONFIGURATION_PROPERTIES_BEAN_ANNOATION = "org.springframework.boot.context.properties.ImportAsConfigurationPropertiesBean";

	static final String IMPORT_AS_CONFIGURATION_PROPERTIES_BEANS_ANNOATION = "org.springframework.boot.context.properties.ImportAsConfigurationPropertiesBeans";

	private static final Set<String> SUPPORTED_OPTIONS = Collections
			.unmodifiableSet(Collections.singleton(ADDITIONAL_METADATA_LOCATIONS_OPTION));

	private MetadataStore metadataStore;

	private MetadataCollector metadataCollector;

	private MetadataGenerationEnvironment metadataEnv;

	protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	protected String deprecatedConfigurationPropertyAnnotation() {
		return DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	protected String constructorBindingAnnotation() {
		return CONSTRUCTOR_BINDING_ANNOTATION;
	}

	protected String defaultValueAnnotation() {
		return DEFAULT_VALUE_ANNOTATION;
	}

	protected String endpointAnnotation() {
		return ENDPOINT_ANNOTATION;
	}

	protected String readOperationAnnotation() {
		return READ_OPERATION_ANNOTATION;
	}

	protected String nameAnnotation() {
		return NAME_ANNOTATION;
	}

	protected String importAsConfigurationPropertiesBeanAnnotation() {
		return IMPORT_AS_CONFIGURATION_PROPERTIES_BEAN_ANNOATION;
	}

	protected String importAsConfigurationPropertiesBeansAnnotation() {
		return IMPORT_AS_CONFIGURATION_PROPERTIES_BEANS_ANNOATION;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public Set<String> getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		this.metadataStore = new MetadataStore(env);
		this.metadataCollector = new MetadataCollector(env, this.metadataStore.readMetadata());
		this.metadataEnv = new MetadataGenerationEnvironment(env, configurationPropertiesAnnotation(),
				nestedConfigurationPropertyAnnotation(), deprecatedConfigurationPropertyAnnotation(),
				constructorBindingAnnotation(), defaultValueAnnotation(), endpointAnnotation(),
				readOperationAnnotation(), nameAnnotation(), importAsConfigurationPropertiesBeanAnnotation(),
				importAsConfigurationPropertiesBeansAnnotation());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		this.metadataCollector.processing(roundEnv);
		processConfigurationProperties(roundEnv);
		processEndpoint(roundEnv);
		processImportAsConfigurationProperties(roundEnv);
		if (roundEnv.processingOver()) {
			try {
				writeMetaData();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	private void processConfigurationProperties(RoundEnvironment roundEnv) {
		TypeElement annotationType = this.metadataEnv.getConfigurationPropertiesAnnotationElement();
		if (annotationType != null) {
			for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
				processElement(element);
			}
		}
	}

	private void processEndpoint(RoundEnvironment roundEnv) {
		TypeElement endpointType = this.metadataEnv.getEndpointAnnotationElement();
		if (endpointType != null) {
			getElementsAnnotatedOrMetaAnnotatedWith(roundEnv, endpointType).forEach(this::processEndpoint);
		}
	}

	private void processImportAsConfigurationProperties(RoundEnvironment roundEnv) {
		TypeElement importAsConfigurationPropertiesBeanType = this.metadataEnv
				.getImportAsConfigurationPropertiesBeansAnnotation();
		TypeElement importAsConfigurationPropertiesBeansType = this.metadataEnv
				.getImportAsConfigurationPropertiesBeansAnnotationElement();
		if (importAsConfigurationPropertiesBeanType == null && importAsConfigurationPropertiesBeansType == null) {
			return;
		}
		Set<Element> elements = new LinkedHashSet<>();
		if (importAsConfigurationPropertiesBeanType != null) {
			elements.addAll(roundEnv.getElementsAnnotatedWith(importAsConfigurationPropertiesBeanType));
		}
		if (importAsConfigurationPropertiesBeansType != null) {
			elements.addAll(roundEnv.getElementsAnnotatedWith(importAsConfigurationPropertiesBeansType));
		}
		elements.forEach(this::processImportAsConfigurationPropertiesBean);
	}

	private Map<Element, List<Element>> getElementsAnnotatedOrMetaAnnotatedWith(RoundEnvironment roundEnv,
			TypeElement annotation) {
		Map<Element, List<Element>> result = new LinkedHashMap<>();
		for (Element element : roundEnv.getRootElements()) {
			List<Element> annotations = this.metadataEnv.getElementsAnnotatedOrMetaAnnotatedWith(element, annotation);
			if (!annotations.isEmpty()) {
				result.put(element, annotations);
			}
		}
		return result;
	}

	private void processElement(Element element) {
		try {
			AnnotationMirror annotation = this.metadataEnv.getConfigurationPropertiesAnnotation(element);
			if (annotation != null) {
				String prefix = getPrefix(annotation);
				if (element instanceof TypeElement) {
					processAnnotatedTypeElement(prefix, (TypeElement) element, false, new Stack<>());
				}
				else if (element instanceof ExecutableElement) {
					processExecutableElement(prefix, (ExecutableElement) element, new Stack<>());
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processAnnotatedTypeElement(String prefix, TypeElement element, boolean fromImport,
			Stack<TypeElement> seen) {
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element, fromImport, null, seen);
	}

	private void processExecutableElement(String prefix, ExecutableElement element, Stack<TypeElement> seen) {
		if ((!element.getModifiers().contains(Modifier.PRIVATE))
				&& (TypeKind.VOID != element.getReturnType().getKind())) {
			Element returns = this.processingEnv.getTypeUtils().asElement(element.getReturnType());
			if (returns instanceof TypeElement) {
				ItemMetadata group = ItemMetadata.newGroup(prefix,
						this.metadataEnv.getTypeUtils().getQualifiedName(returns),
						this.metadataEnv.getTypeUtils().getQualifiedName(element.getEnclosingElement()),
						element.toString());
				if (this.metadataCollector.hasSimilarGroup(group)) {
					this.processingEnv.getMessager().printMessage(Kind.ERROR,
							"Duplicate `@ConfigurationProperties` definition for prefix '" + prefix + "'", element);
				}
				else {
					this.metadataCollector.add(group);
					processTypeElement(prefix, (TypeElement) returns, false, element, seen);
				}
			}
		}
	}

	private void processTypeElement(String prefix, TypeElement element, boolean fromImport, ExecutableElement source,
			Stack<TypeElement> seen) {
		if (!seen.contains(element)) {
			seen.push(element);
			new PropertyDescriptorResolver(this.metadataEnv).resolve(element, fromImport, source)
					.forEach((descriptor) -> {
						this.metadataCollector.add(descriptor.resolveItemMetadata(prefix, this.metadataEnv));
						if (descriptor.isNested(this.metadataEnv)) {
							TypeElement nestedTypeElement = (TypeElement) this.metadataEnv.getTypeUtils()
									.asElement(descriptor.getType());
							String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.getName());
							processTypeElement(nestedPrefix, nestedTypeElement, false, source, seen);
						}
					});
			seen.pop();
		}
	}

	private void processEndpoint(Element element, List<Element> annotations) {
		try {
			String annotationName = this.metadataEnv.getTypeUtils().getQualifiedName(annotations.get(0));
			AnnotationMirror annotation = this.metadataEnv.getAnnotation(element, annotationName);
			if (element instanceof TypeElement) {
				processEndpoint(annotation, (TypeElement) element);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processEndpoint(AnnotationMirror annotation, TypeElement element) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		String endpointId = (String) elementValues.get("id");
		if (endpointId == null || "".equals(endpointId)) {
			return; // Can't process that endpoint
		}
		String endpointKey = ItemMetadata.newItemMetadataPrefix("management.endpoint.", endpointId);
		Boolean enabledByDefault = (Boolean) elementValues.get("enableByDefault");
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.add(ItemMetadata.newGroup(endpointKey, type, type, null));
		this.metadataCollector.add(ItemMetadata.newProperty(endpointKey, "enabled", Boolean.class.getName(), type, null,
				String.format("Whether to enable the %s endpoint.", endpointId),
				(enabledByDefault != null) ? enabledByDefault : true, null));
		if (hasMainReadOperation(element)) {
			this.metadataCollector.add(ItemMetadata.newProperty(endpointKey, "cache.time-to-live",
					Duration.class.getName(), type, null, "Maximum time that a response can be cached.", "0ms", null));
		}
	}

	private void processImportAsConfigurationPropertiesBean(Element element) {
		this.metadataEnv.getImportAsConfigurationPropertiesBeanAnnotations(element)
				.forEach(this::processImportAsConfigurationPropertiesBean);
	}

	@SuppressWarnings("unchecked")
	private void processImportAsConfigurationPropertiesBean(AnnotationMirror annotation) {
		String prefix = getPrefix(annotation);
		processImportAsConfigurationPropertiesBeanTypes(prefix,
				(List<TypeMirror>) this.metadataEnv.getAnnotationElementValues(annotation).get("type"));
		processImportAsConfigurationPropertiesBeanTypes(prefix,
				(List<TypeMirror>) this.metadataEnv.getAnnotationElementValues(annotation).get("value"));
	}

	private void processImportAsConfigurationPropertiesBeanTypes(String prefix, List<TypeMirror> types) {
		if (types != null) {
			for (TypeMirror type : types) {
				Element element = this.metadataEnv.getTypeUtils().asElement(type);
				AnnotationMirror annotation = this.metadataEnv.getConfigurationPropertiesAnnotation(element);
				prefix = (annotation != null) ? getPrefix(annotation) : prefix;
				processAnnotatedTypeElement(prefix, (TypeElement) element, true, new Stack<>());
			}
		}
	}

	private boolean hasMainReadOperation(TypeElement element) {
		for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
			if (this.metadataEnv.getReadOperationAnnotation(method) != null
					&& (TypeKind.VOID != method.getReturnType().getKind()) && hasNoOrOptionalParameters(method)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNoOrOptionalParameters(ExecutableElement method) {
		for (VariableElement parameter : method.getParameters()) {
			if (!this.metadataEnv.hasNullableAnnotation(parameter)) {
				return false;
			}
		}
		return true;
	}

	private String getPrefix(AnnotationMirror annotation) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		Object prefix = elementValues.get("prefix");
		if (prefix != null && !"".equals(prefix)) {
			return (String) prefix;
		}
		Object value = elementValues.get("value");
		if (value != null && !"".equals(value)) {
			return (String) value;
		}
		return null;
	}

	protected ConfigurationMetadata writeMetaData() throws Exception {
		ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
		metadata = mergeAdditionalMetadata(metadata);
		if (!metadata.getItems().isEmpty()) {
			this.metadataStore.writeMetadata(metadata);
			return metadata;
		}
		return null;
	}

	private ConfigurationMetadata mergeAdditionalMetadata(ConfigurationMetadata metadata) {
		try {
			ConfigurationMetadata merged = new ConfigurationMetadata(metadata);
			merged.merge(this.metadataStore.readAdditionalMetadata());
			return merged;
		}
		catch (FileNotFoundException ex) {
			// No additional metadata
		}
		catch (InvalidConfigurationMetadataException ex) {
			log(ex.getKind(), ex.getMessage());
		}
		catch (Exception ex) {
			logWarning("Unable to merge additional metadata");
			logWarning(getStackTrace(ex));
		}
		return metadata;
	}

	private String getStackTrace(Exception ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer, true));
		return writer.toString();
	}

	private void logWarning(String msg) {
		log(Kind.WARNING, msg);
	}

	private void log(Kind kind, String msg) {
		this.processingEnv.getMessager().printMessage(kind, msg);
	}

}
