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

package org.springframework.boot.autoconfigureprocessor;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Annotation processor to store certain annotations from auto-configuration classes in a
 * property file.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 1.5.0
 */
@SupportedAnnotationTypes({ "org.springframework.boot.autoconfigure.condition.ConditionalOnClass",
		"org.springframework.boot.autoconfigure.condition.ConditionalOnBean",
		"org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate",
		"org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication",
		"org.springframework.boot.autoconfigure.AutoConfigureBefore",
		"org.springframework.boot.autoconfigure.AutoConfigureAfter",
		"org.springframework.boot.autoconfigure.AutoConfigureOrder",
		"org.springframework.boot.autoconfigure.AutoConfiguration" })
public class AutoConfigureAnnotationProcessor extends AbstractProcessor {

	protected static final String PROPERTIES_PATH = "META-INF/spring-autoconfigure-metadata.properties";

	private final Map<String, String> properties = new TreeMap<>();

	private final List<PropertyGenerator> propertyGenerators;

	/**
     * Constructs a new instance of the {@code AutoConfigureAnnotationProcessor} class.
     * Initializes the {@code propertyGenerators} field with an unmodifiable list of property generators
     * obtained from the {@code getPropertyGenerators} method.
     */
    public AutoConfigureAnnotationProcessor() {
		this.propertyGenerators = Collections.unmodifiableList(getPropertyGenerators());
	}

	/**
     * Returns a list of property generators.
     * 
     * @return the list of property generators
     */
    protected List<PropertyGenerator> getPropertyGenerators() {
		List<PropertyGenerator> generators = new ArrayList<>();
		addConditionPropertyGenerators(generators);
		addAutoConfigurePropertyGenerators(generators);
		return generators;
	}

	/**
     * Adds condition property generators to the given list.
     * 
     * @param generators the list of property generators to add to
     */
    private void addConditionPropertyGenerators(List<PropertyGenerator> generators) {
		String annotationPackage = "org.springframework.boot.autoconfigure.condition";
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnClass")
			.withAnnotation(new OnClassConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnBean")
			.withAnnotation(new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnSingleCandidate")
			.withAnnotation(new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnWebApplication")
			.withAnnotation(ValueExtractor.allFrom("type")));
	}

	/**
     * Adds auto-configure property generators to the given list.
     *
     * @param generators the list of property generators to add to
     */
    private void addAutoConfigurePropertyGenerators(List<PropertyGenerator> generators) {
		String annotationPackage = "org.springframework.boot.autoconfigure";
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureBefore", true)
			.withAnnotation(ValueExtractor.allFrom("value", "name"))
			.withAnnotation("AutoConfiguration", ValueExtractor.allFrom("before", "beforeName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureAfter", true)
			.withAnnotation(ValueExtractor.allFrom("value", "name"))
			.withAnnotation("AutoConfiguration", ValueExtractor.allFrom("after", "afterName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureOrder")
			.withAnnotation(ValueExtractor.allFrom("value")));
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
     * Processes the annotations and generates properties for the specified generators.
     * 
     * @param annotations the set of annotations to process
     * @param roundEnv the round environment
     * @return false
     * @throws IllegalStateException if failed to write metadata
     */
    @Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (PropertyGenerator generator : this.propertyGenerators) {
			process(roundEnv, generator);
		}
		if (roundEnv.processingOver()) {
			try {
				writeProperties();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	/**
     * Processes the given round environment and property generator.
     * 
     * @param roundEnv the round environment to process
     * @param generator the property generator to use
     */
    private void process(RoundEnvironment roundEnv, PropertyGenerator generator) {
		for (String annotationName : generator.getSupportedAnnotations()) {
			TypeElement annotationType = this.processingEnv.getElementUtils().getTypeElement(annotationName);
			if (annotationType != null) {
				for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
					processElement(element, generator, annotationName);
				}
			}
		}
	}

	/**
     * Processes the given element by applying the specified property generator to the properties map.
     * 
     * @param element         the element to be processed
     * @param generator       the property generator to be applied
     * @param annotationName  the name of the annotation to be retrieved
     * @throws IllegalStateException if an error occurs while processing the configuration meta-data
     */
    private void processElement(Element element, PropertyGenerator generator, String annotationName) {
		try {
			String qualifiedName = Elements.getQualifiedName(element);
			AnnotationMirror annotation = getAnnotation(element, annotationName);
			if (qualifiedName != null && annotation != null) {
				List<Object> values = getValues(generator, annotationName, annotation);
				generator.applyToProperties(this.properties, qualifiedName, values);
				this.properties.put(qualifiedName, "");
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	/**
     * Retrieves the specified annotation mirror from the given element.
     * 
     * @param element the element from which to retrieve the annotation mirror
     * @param type the fully qualified name of the annotation type
     * @return the annotation mirror if found, null otherwise
     */
    private AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
     * Retrieves the values associated with a specific annotation from the given annotation mirror.
     * 
     * @param generator the PropertyGenerator instance used for extracting values
     * @param annotationName the name of the annotation to retrieve values from
     * @param annotation the AnnotationMirror instance containing the values
     * @return a List of Object values extracted from the annotation, or an empty List if no values are found
     */
    private List<Object> getValues(PropertyGenerator generator, String annotationName, AnnotationMirror annotation) {
		ValueExtractor extractor = generator.getValueExtractor(annotationName);
		if (extractor == null) {
			return Collections.emptyList();
		}
		return extractor.getValues(annotation);
	}

	/**
     * Writes the properties to a file.
     *
     * @throws IOException if an I/O error occurs while writing the properties
     */
    private void writeProperties() throws IOException {
		if (!this.properties.isEmpty()) {
			Filer filer = this.processingEnv.getFiler();
			FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", PROPERTIES_PATH);
			try (Writer writer = new OutputStreamWriter(file.openOutputStream(), StandardCharsets.UTF_8)) {
				for (Map.Entry<String, String> entry : this.properties.entrySet()) {
					writer.append(entry.getKey());
					writer.append("=");
					writer.append(entry.getValue());
					writer.append(System.lineSeparator());
				}
			}
		}
	}

	@FunctionalInterface
	interface ValueExtractor {

		List<Object> getValues(AnnotationMirror annotation);

		static ValueExtractor allFrom(String... names) {
			return new NamedValuesExtractor(names);
		}

	}

	/**
     * AbstractValueExtractor class.
     */
    private abstract static class AbstractValueExtractor implements ValueExtractor {

		/**
         * Extracts values from an AnnotationValue object.
         * 
         * @param annotationValue the AnnotationValue object to extract values from
         * @return a Stream of extracted values
         */
        @SuppressWarnings("unchecked")
		protected Stream<Object> extractValues(AnnotationValue annotationValue) {
			if (annotationValue == null) {
				return Stream.empty();
			}
			Object value = annotationValue.getValue();
			if (value instanceof List) {
				return ((List<AnnotationValue>) value).stream()
					.map((annotation) -> extractValue(annotation.getValue()));
			}
			return Stream.of(extractValue(value));
		}

		/**
         * Extracts the value from the given object.
         * 
         * @param value the object from which to extract the value
         * @return the extracted value, or the original object if it cannot be extracted
         */
        private Object extractValue(Object value) {
			if (value instanceof DeclaredType declaredType) {
				return Elements.getQualifiedName(declaredType.asElement());
			}
			return value;
		}

	}

	/**
     * NamedValuesExtractor class.
     */
    private static class NamedValuesExtractor extends AbstractValueExtractor {

		private final Set<String> names;

		/**
         * Constructs a new NamedValuesExtractor object with the specified names.
         * 
         * @param names the names of the values to be extracted
         */
        NamedValuesExtractor(String... names) {
			this.names = new HashSet<>(Arrays.asList(names));
		}

		/**
         * Retrieves the values from the given annotation.
         * 
         * @param annotation the annotation mirror to extract values from
         * @return a list of objects containing the extracted values
         */
        @Override
		public List<Object> getValues(AnnotationMirror annotation) {
			List<Object> result = new ArrayList<>();
			annotation.getElementValues().forEach((key, value) -> {
				if (this.names.contains(key.getSimpleName().toString())) {
					extractValues(value).forEach(result::add);
				}
			});
			return result;
		}

	}

	/**
     * OnBeanConditionValueExtractor class.
     */
    static class OnBeanConditionValueExtractor extends AbstractValueExtractor {

		/**
         * Retrieves the values from the given annotation mirror.
         * 
         * @param annotation the annotation mirror to extract values from
         * @return a list of objects representing the extracted values
         */
        @Override
		public List<Object> getValues(AnnotationMirror annotation) {
			Map<String, AnnotationValue> attributes = new LinkedHashMap<>();
			annotation.getElementValues()
				.forEach((key, value) -> attributes.put(key.getSimpleName().toString(), value));
			if (attributes.containsKey("name")) {
				return Collections.emptyList();
			}
			List<Object> result = new ArrayList<>();
			extractValues(attributes.get("value")).forEach(result::add);
			extractValues(attributes.get("type")).forEach(result::add);
			return result;
		}

	}

	/**
     * OnClassConditionValueExtractor class.
     */
    static class OnClassConditionValueExtractor extends NamedValuesExtractor {

		/**
         * Constructs a new OnClassConditionValueExtractor with the specified value and name.
         */
        OnClassConditionValueExtractor() {
			super("value", "name");
		}

		/**
         * Retrieves the values of the given annotation and sorts them in ascending order.
         * 
         * @param annotation the annotation mirror from which to retrieve the values
         * @return a list of values sorted in ascending order
         */
        @Override
		public List<Object> getValues(AnnotationMirror annotation) {
			List<Object> values = super.getValues(annotation);
			values.sort(this::compare);
			return values;
		}

		/**
         * Compares two objects based on the following criteria:
         * 1. Checks if the objects belong to the Spring class using the isSpringClass method.
         * 2. If both objects belong to the Spring class, compares them based on their string representation in a case-insensitive manner.
         * 3. If either object does not belong to the Spring class, compares them based on their string representation in a case-insensitive manner.
         *
         * @param o1 the first object to be compared
         * @param o2 the second object to be compared
         * @return a negative integer, zero, or a positive integer as the first object is less than, equal to, or greater than the second object
         */
        private int compare(Object o1, Object o2) {
			return Comparator.comparing(this::isSpringClass)
				.thenComparing(String.CASE_INSENSITIVE_ORDER)
				.compare(o1.toString(), o2.toString());
		}

		/**
         * Checks if the given type is a Spring class.
         * 
         * @param type the fully qualified name of the class to check
         * @return true if the type is a Spring class, false otherwise
         */
        private boolean isSpringClass(String type) {
			return type.startsWith("org.springframework");
		}

	}

	/**
     * PropertyGenerator class.
     */
    static final class PropertyGenerator {

		private final String annotationPackage;

		private final String propertyName;

		private final boolean omitEmptyValues;

		private final Map<String, ValueExtractor> valueExtractors;

		/**
         * Constructs a new PropertyGenerator with the specified annotation package, property name,
         * omit empty values flag, and value extractors.
         *
         * @param annotationPackage the package name where the annotations are located
         * @param propertyName the name of the property
         * @param omitEmptyValues flag indicating whether to omit empty values
         * @param valueExtractors the map of value extractors to be used
         */
        private PropertyGenerator(String annotationPackage, String propertyName, boolean omitEmptyValues,
				Map<String, ValueExtractor> valueExtractors) {
			this.annotationPackage = annotationPackage;
			this.propertyName = propertyName;
			this.omitEmptyValues = omitEmptyValues;
			this.valueExtractors = valueExtractors;
		}

		/**
         * Returns a new PropertyGenerator with the specified ValueExtractor annotation.
         * 
         * @param valueExtractor the ValueExtractor annotation to be used
         * @return a new PropertyGenerator with the specified ValueExtractor annotation
         */
        PropertyGenerator withAnnotation(ValueExtractor valueExtractor) {
			return withAnnotation(this.propertyName, valueExtractor);
		}

		/**
         * Adds a new value extractor to the property generator with the specified annotation name.
         * 
         * @param name The name of the annotation.
         * @param valueExtractor The value extractor to be added.
         * @return A new PropertyGenerator instance with the added value extractor.
         */
        PropertyGenerator withAnnotation(String name, ValueExtractor ValueExtractor) {
			Map<String, ValueExtractor> valueExtractors = new LinkedHashMap<>(this.valueExtractors);
			valueExtractors.put(this.annotationPackage + "." + name, ValueExtractor);
			return new PropertyGenerator(this.annotationPackage, this.propertyName, this.omitEmptyValues,
					valueExtractors);
		}

		/**
         * Returns a set of supported annotations.
         * 
         * @return a set of supported annotations
         */
        Set<String> getSupportedAnnotations() {
			return this.valueExtractors.keySet();
		}

		/**
         * Returns the ValueExtractor associated with the given annotation.
         * 
         * @param annotation the annotation for which the ValueExtractor is to be retrieved
         * @return the ValueExtractor associated with the given annotation, or null if not found
         */
        ValueExtractor getValueExtractor(String annotation) {
			return this.valueExtractors.get(annotation);
		}

		/**
         * Applies the given annotation values to the properties map.
         * 
         * @param properties the map of properties to apply the values to
         * @param className the name of the class
         * @param annotationValues the list of annotation values to apply
         */
        void applyToProperties(Map<String, String> properties, String className, List<Object> annotationValues) {
			if (this.omitEmptyValues && annotationValues.isEmpty()) {
				return;
			}
			mergeProperties(properties, className + "." + this.propertyName, toCommaDelimitedString(annotationValues));
		}

		/**
         * Merges the given key-value pair into the properties map.
         * If the key does not exist in the map or its value is empty, the key-value pair is added as is.
         * If the key exists and its value is not empty, the new value is appended to the existing value with a comma separator.
         *
         * @param properties the map of properties to merge into
         * @param key the key of the property to merge
         * @param value the value of the property to merge
         */
        private void mergeProperties(Map<String, String> properties, String key, String value) {
			String existingKey = properties.get(key);
			if (existingKey == null || existingKey.isEmpty()) {
				properties.put(key, value);
			}
			else if (!value.isEmpty()) {
				properties.put(key, existingKey + "," + value);
			}
		}

		/**
         * Converts a list of objects to a comma-delimited string.
         * 
         * @param list the list of objects to be converted
         * @return the comma-delimited string representation of the list
         */
        private String toCommaDelimitedString(List<Object> list) {
			if (list.isEmpty()) {
				return "";
			}
			StringBuilder result = new StringBuilder();
			for (Object item : list) {
				result.append((!result.isEmpty()) ? "," : "");
				result.append(item);
			}
			return result.toString();
		}

		/**
         * Returns a PropertyGenerator object with the specified annotation package and property name.
         * 
         * @param annotationPackage the package name of the annotations to be generated
         * @param propertyName the name of the property to be generated
         * @return a PropertyGenerator object
         */
        static PropertyGenerator of(String annotationPackage, String propertyName) {
			return of(annotationPackage, propertyName, false);
		}

		/**
         * Creates a new instance of PropertyGenerator with the specified parameters.
         *
         * @param annotationPackage the package name of the annotations to be generated
         * @param propertyName the name of the property to be generated
         * @param omitEmptyValues flag indicating whether to omit empty values or not
         * @return a new instance of PropertyGenerator
         */
        static PropertyGenerator of(String annotationPackage, String propertyName, boolean omitEmptyValues) {
			return new PropertyGenerator(annotationPackage, propertyName, omitEmptyValues, Collections.emptyMap());
		}

	}

}
