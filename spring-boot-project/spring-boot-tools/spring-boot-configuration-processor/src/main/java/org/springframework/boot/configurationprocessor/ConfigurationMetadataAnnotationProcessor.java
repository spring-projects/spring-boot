/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * @author Scott Frederick
 * @author Moritz Halbritter
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

	static final String CONSTRUCTOR_BINDING_ANNOTATION = "org.springframework.boot.context.properties.bind.ConstructorBinding";

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

	private static final Set<String> SUPPORTED_OPTIONS = Collections.singleton(ADDITIONAL_METADATA_LOCATIONS_OPTION);

	private MetadataStore metadataStore;

	private MetadataCollector metadataCollector;

	private MetadataGenerationEnvironment metadataEnv;

	/**
     * Returns the configuration properties annotation.
     *
     * @return the configuration properties annotation
     */
    protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	/**
     * Returns the value of the nested configuration property annotation.
     *
     * @return the value of the nested configuration property annotation
     */
    protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	/**
     * Returns the value of the deprecated configuration property annotation.
     *
     * @return the value of the deprecated configuration property annotation
     */
    protected String deprecatedConfigurationPropertyAnnotation() {
		return DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	/**
     * Returns the constructor binding annotation.
     *
     * @return the constructor binding annotation
     */
    protected String constructorBindingAnnotation() {
		return CONSTRUCTOR_BINDING_ANNOTATION;
	}

	/**
     * Returns the value of the AUTOWIRED_ANNOTATION constant.
     *
     * @return the value of the AUTOWIRED_ANNOTATION constant
     */
    protected String autowiredAnnotation() {
		return AUTOWIRED_ANNOTATION;
	}

	/**
     * Returns the default value annotation.
     *
     * @return the default value annotation
     */
    protected String defaultValueAnnotation() {
		return DEFAULT_VALUE_ANNOTATION;
	}

	/**
     * Returns a set of endpoint annotations.
     *
     * @return the set of endpoint annotations
     */
    protected Set<String> endpointAnnotations() {
		return new HashSet<>(Arrays.asList(CONTROLLER_ENDPOINT_ANNOTATION, ENDPOINT_ANNOTATION, JMX_ENDPOINT_ANNOTATION,
				REST_CONTROLLER_ENDPOINT_ANNOTATION, SERVLET_ENDPOINT_ANNOTATION, WEB_ENDPOINT_ANNOTATION));
	}

	/**
     * Returns the value of the read operation annotation.
     *
     * @return the value of the read operation annotation
     */
    protected String readOperationAnnotation() {
		return READ_OPERATION_ANNOTATION;
	}

	/**
     * Returns the name annotation.
     *
     * @return the name annotation
     */
    protected String nameAnnotation() {
		return NAME_ANNOTATION;
	}

	/**
     * Returns the latest supported source version.
     *
     * @return the latest supported source version
     */
    @Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	/**
     * Returns a set of supported options for the ConfigurationMetadataAnnotationProcessor.
     *
     * @return a set of supported options
     */
    @Override
	public Set<String> getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}

	/**
     * Initializes the ConfigurationMetadataAnnotationProcessor.
     * 
     * @param env the ProcessingEnvironment object used for processing annotations
     */
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

	/**
     * Processes the annotations and generates metadata for configuration properties and endpoints.
     * 
     * @param annotations the set of annotations to process
     * @param roundEnv the round environment for the current processing round
     * @return false to indicate that the processing is not complete
     */
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

	/**
     * Retrieves a map of elements annotated or meta-annotated with the specified annotation.
     * 
     * @param roundEnv the round environment
     * @param annotation the annotation to search for
     * @return a map of elements and their corresponding annotations
     */
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

	/**
     * Processes the given element by extracting configuration meta-data.
     * 
     * @param element The element to process.
     * @throws IllegalStateException If an error occurs while processing the configuration meta-data.
     */
    private void processElement(Element element) {
		try {
			AnnotationMirror annotation = this.metadataEnv.getConfigurationPropertiesAnnotation(element);
			if (annotation != null) {
				String prefix = getPrefix(annotation);
				if (element instanceof TypeElement typeElement) {
					processAnnotatedTypeElement(prefix, typeElement, new ArrayDeque<>());
				}
				else if (element instanceof ExecutableElement executableElement) {
					processExecutableElement(prefix, executableElement, new ArrayDeque<>());
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	/**
     * Processes an annotated type element.
     * 
     * @param prefix the prefix to be added to the group name
     * @param element the type element to be processed
     * @param seen a deque of type elements that have been seen
     */
    private void processAnnotatedTypeElement(String prefix, TypeElement element, Deque<TypeElement> seen) {
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element, null, seen);
	}

	/**
     * Processes an executable element.
     * 
     * @param prefix the prefix for the configuration properties
     * @param element the executable element to process
     * @param seen a deque of type elements that have been seen during processing
     */
    private void processExecutableElement(String prefix, ExecutableElement element, Deque<TypeElement> seen) {
		if ((!element.getModifiers().contains(Modifier.PRIVATE))
				&& (TypeKind.VOID != element.getReturnType().getKind())) {
			Element returns = this.processingEnv.getTypeUtils().asElement(element.getReturnType());
			if (returns instanceof TypeElement typeElement) {
				ItemMetadata group = ItemMetadata.newGroup(prefix,
						this.metadataEnv.getTypeUtils().getQualifiedName(returns),
						this.metadataEnv.getTypeUtils().getQualifiedName(element.getEnclosingElement()),
						element.toString());
				if (this.metadataCollector.hasSimilarGroup(group)) {
					this.processingEnv.getMessager()
						.printMessage(Kind.ERROR,
								"Duplicate @ConfigurationProperties definition for prefix '" + prefix + "'", element);
				}
				else {
					this.metadataCollector.add(group);
					processTypeElement(prefix, typeElement, element, seen);
				}
			}
		}
	}

	/**
     * Processes a TypeElement and its nested elements to resolve property descriptors and collect metadata.
     * 
     * @param prefix the prefix for the property descriptors
     * @param element the TypeElement to process
     * @param source the ExecutableElement representing the source of the processing
     * @param seen a Deque of TypeElements that have already been processed
     */
    private void processTypeElement(String prefix, TypeElement element, ExecutableElement source,
			Deque<TypeElement> seen) {
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

	/**
     * Processes the endpoint by retrieving the annotation name and annotation mirror.
     * If the element is an instance of TypeElement, it calls the processEndpoint method
     * to further process the annotation and type element.
     *
     * @param element     the element to process
     * @param annotations the list of annotations associated with the element
     * @throws IllegalStateException if there is an error processing the configuration meta-data
     */
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

	/**
     * Processes the given endpoint annotation and element.
     * 
     * @param annotation the annotation mirror
     * @param element the type element
     */
    private void processEndpoint(AnnotationMirror annotation, TypeElement element) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		String endpointId = (String) elementValues.get("id");
		if (endpointId == null || endpointId.isEmpty()) {
			return; // Can't process that endpoint
		}
		String endpointKey = ItemMetadata.newItemMetadataPrefix("management.endpoint.", endpointId);
		boolean enabledByDefault = (boolean) elementValues.getOrDefault("enableByDefault", true);
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.addIfAbsent(ItemMetadata.newGroup(endpointKey, type, type, null));
		this.metadataCollector.add(
				ItemMetadata.newProperty(endpointKey, "enabled", Boolean.class.getName(), type, null,
						"Whether to enable the %s endpoint.".formatted(endpointId), enabledByDefault, null),
				(existing) -> checkEnabledValueMatchesExisting(existing, enabledByDefault, type));
		if (hasMainReadOperation(element)) {
			this.metadataCollector.addIfAbsent(ItemMetadata.newProperty(endpointKey, "cache.time-to-live",
					Duration.class.getName(), type, null, "Maximum time that a response can be cached.", "0ms", null));
		}
	}

	/**
     * Checks if the enabled value matches the existing value for an item metadata.
     * 
     * @param existing The existing item metadata.
     * @param enabledByDefault The new enabled value.
     * @param sourceType The source type of the new enabled value.
     * @throws IllegalStateException if the existing value conflicts with the new value.
     */
    private void checkEnabledValueMatchesExisting(ItemMetadata existing, boolean enabledByDefault, String sourceType) {
		boolean existingDefaultValue = (boolean) existing.getDefaultValue();
		if (enabledByDefault != existingDefaultValue) {
			throw new IllegalStateException(
					"Existing property '%s' from type %s has a conflicting value. Existing value: %b, new value from type %s: %b"
						.formatted(existing.getName(), existing.getSourceType(), existingDefaultValue, sourceType,
								enabledByDefault));
		}
	}

	/**
     * Checks if the given TypeElement has a main read operation.
     * 
     * @param element the TypeElement to check
     * @return true if the TypeElement has a main read operation, false otherwise
     */
    private boolean hasMainReadOperation(TypeElement element) {
		for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
			if (this.metadataEnv.getReadOperationAnnotation(method) != null
					&& (TypeKind.VOID != method.getReturnType().getKind()) && hasNoOrOptionalParameters(method)) {
				return true;
			}
		}
		return false;
	}

	/**
     * Checks if the given method has no or optional parameters.
     * 
     * @param method the ExecutableElement representing the method to check
     * @return true if the method has no or optional parameters, false otherwise
     */
    private boolean hasNoOrOptionalParameters(ExecutableElement method) {
		for (VariableElement parameter : method.getParameters()) {
			if (!this.metadataEnv.hasNullableAnnotation(parameter)) {
				return false;
			}
		}
		return true;
	}

	/**
     * Returns the prefix value from the given annotation.
     * 
     * @param annotation the annotation mirror
     * @return the prefix value if found, otherwise the value
     */
    private String getPrefix(AnnotationMirror annotation) {
		String prefix = this.metadataEnv.getAnnotationElementStringValue(annotation, "prefix");
		if (prefix != null) {
			return prefix;
		}
		return this.metadataEnv.getAnnotationElementStringValue(annotation, "value");
	}

	/**
     * Writes the metadata to the metadata store.
     * 
     * @return The configuration metadata that was written, or null if no metadata items were present.
     * @throws Exception if an error occurs while writing the metadata.
     */
    protected ConfigurationMetadata writeMetadata() throws Exception {
		ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
		metadata = mergeAdditionalMetadata(metadata);
		if (!metadata.getItems().isEmpty()) {
			this.metadataStore.writeMetadata(metadata);
			return metadata;
		}
		return null;
	}

	/**
     * Merges additional metadata with the given ConfigurationMetadata object.
     * 
     * @param metadata The ConfigurationMetadata object to merge additional metadata with.
     * @return The merged ConfigurationMetadata object.
     */
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

	/**
     * Returns the stack trace of the given exception as a string.
     *
     * @param ex the exception for which to retrieve the stack trace
     * @return the stack trace of the exception as a string
     */
    private String getStackTrace(Exception ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer, true));
		return writer.toString();
	}

	/**
     * Logs a warning message.
     *
     * @param msg the warning message to be logged
     */
    private void logWarning(String msg) {
		log(Kind.WARNING, msg);
	}

	/**
     * Logs a message with the specified kind.
     * 
     * @param kind the kind of message to be logged
     * @param msg the message to be logged
     */
    private void log(Kind kind, String msg) {
		this.processingEnv.getMessager().printMessage(kind, msg);
	}

}
