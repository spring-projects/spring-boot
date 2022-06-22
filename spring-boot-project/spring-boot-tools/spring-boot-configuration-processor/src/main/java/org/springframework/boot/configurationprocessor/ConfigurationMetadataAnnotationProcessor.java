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

package org.springframework.boot.configurationprocessor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
@SupportedAnnotationTypes({ ConfigurationMetadataAnnotationProcessor.AUTO_CONFIGURATION_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION,
		"org.springframework.context.annotation.Configuration" })
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {

	static final String ADDITIONAL_METADATA_LOCATIONS_OPTION = "org.springframework.boot.configurationprocessor.additionalMetadataLocations";

	static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot.context.properties.ConfigurationProperties";

	static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.NestedConfigurationProperty";

	static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.DeprecatedConfigurationProperty";

	static final String CONSTRUCTOR_BINDING_ANNOTATION = "org.springframework.boot.context.properties.ConstructorBinding";

	static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";

	static final String DEFAULT_VALUE_ANNOTATION = "org.springframework.boot.context.properties.bind.DefaultValue";

	static final String CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint";

	static final String ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.Endpoint";

	static final String JMX_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint";

	static final String REST_CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint";

	static final String SERVLET_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint";

	static final String WEB_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint";

	static final String READ_OPERATION_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.ReadOperation";

	static final String NAME_ANNOTATION = "org.springframework.boot.context.properties.bind.Name";

	static final String AUTO_CONFIGURATION_ANNOTATION = "org.springframework.boot.autoconfigure.AutoConfiguration";

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

	protected String autowiredAnnotation() {
		return AUTOWIRED_ANNOTATION;
	}

	protected String defaultValueAnnotation() {
		return DEFAULT_VALUE_ANNOTATION;
	}

	protected Set<String> endpointAnnotations() {
		return new HashSet<>(Arrays.asList(CONTROLLER_ENDPOINT_ANNOTATION, ENDPOINT_ANNOTATION, JMX_ENDPOINT_ANNOTATION,
				REST_CONTROLLER_ENDPOINT_ANNOTATION, SERVLET_ENDPOINT_ANNOTATION, WEB_ENDPOINT_ANNOTATION));
	}

	protected String readOperationAnnotation() {
		return READ_OPERATION_ANNOTATION;
	}

	protected String nameAnnotation() {
		return NAME_ANNOTATION;
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
				constructorBindingAnnotation(), autowiredAnnotation(), defaultValueAnnotation(), endpointAnnotations(),
				readOperationAnnotation(), nameAnnotation());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		this.metadataCollector.processing(roundEnv);
		TypeElement annotationType = this.metadataEnv.getConfigurationPropertiesAnnotationElement();
		if (annotationType != null) { // Is @ConfigurationProperties available
			for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
				processElement(element);
			}
		}
		Set<TypeElement> endpointTypes = this.metadataEnv.getEndpointAnnotationElements();
		if (!endpointTypes.isEmpty()) { // Are endpoint annotations available
			for (TypeElement endpointType : endpointTypes) {
				getElementsAnnotatedOrMetaAnnotatedWith(roundEnv, endpointType).forEach(this::processEndpoint);
			}
		}
		if (roundEnv.processingOver()) {
			try {
				writeMetadata();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
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
				if (element instanceof TypeElement typeElement) {
					processAnnotatedTypeElement(prefix, typeElement, new Stack<>());
				}
				else if (element instanceof ExecutableElement executableElement) {
					processExecutableElement(prefix, executableElement, new Stack<>());
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processAnnotatedTypeElement(String prefix, TypeElement element, Stack<TypeElement> seen) {
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element, null, seen);
	}

	private void processExecutableElement(String prefix, ExecutableElement element, Stack<TypeElement> seen) {
		if ((!element.getModifiers().contains(Modifier.PRIVATE))
				&& (TypeKind.VOID != element.getReturnType().getKind())) {
			Element returns = this.processingEnv.getTypeUtils().asElement(element.getReturnType());
			if (returns instanceof TypeElement typeElement) {
				ItemMetadata group = ItemMetadata.newGroup(prefix,
						this.metadataEnv.getTypeUtils().getQualifiedName(returns),
						this.metadataEnv.getTypeUtils().getQualifiedName(element.getEnclosingElement()),
						element.toString());
				if (this.metadataCollector.hasSimilarGroup(group)) {
					this.processingEnv.getMessager().printMessage(Kind.ERROR,
							"Duplicate @ConfigurationProperties definition for prefix '" + prefix + "'", element);
				}
				else {
					this.metadataCollector.add(group);
					processTypeElement(prefix, typeElement, element, seen);
				}
			}
		}
	}

	private void processTypeElement(String prefix, TypeElement element, ExecutableElement source,
			Stack<TypeElement> seen) {
		if (!seen.contains(element)) {
			seen.push(element);
			new PropertyDescriptorResolver(this.metadataEnv).resolve(element, source).forEach((descriptor) -> {
				this.metadataCollector.add(descriptor.resolveItemMetadata(prefix, this.metadataEnv));
				if (descriptor.isNested(this.metadataEnv)) {
					TypeElement nestedTypeElement = (TypeElement) this.metadataEnv.getTypeUtils()
							.asElement(descriptor.getType());
					String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.getName());
					processTypeElement(nestedPrefix, nestedTypeElement, source, seen);
				}
			});
			seen.pop();
		}
	}

	private void processEndpoint(Element element, List<Element> annotations) {
		try {
			String annotationName = this.metadataEnv.getTypeUtils().getQualifiedName(annotations.get(0));
			AnnotationMirror annotation = this.metadataEnv.getAnnotation(element, annotationName);
			if (element instanceof TypeElement typeElement) {
				processEndpoint(annotation, typeElement);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processEndpoint(AnnotationMirror annotation, TypeElement element) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		String endpointId = (String) elementValues.get("id");
		if (endpointId == null || endpointId.isEmpty()) {
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

	protected ConfigurationMetadata writeMetadata() throws Exception {
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
