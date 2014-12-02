/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;
import org.springframework.boot.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 * Annotation {@link Processor} that writes meta-data file for
 * {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
@SupportedAnnotationTypes({ "*" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {

	static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot."
			+ "context.properties.ConfigurationProperties";

	static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot."
			+ "context.properties.NestedConfigurationProperty";

	private ConfigurationMetadata metadata;

	private TypeUtils typeUtils;

	private FieldValuesParser fieldValuesParser;

	private ElementExcludeFilter elementExcludeFilter = new ElementExcludeFilter();

	protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		this.metadata = new ConfigurationMetadata();
		this.typeUtils = new TypeUtils(env);
		try {
			this.fieldValuesParser = new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			this.fieldValuesParser = FieldValuesParser.NONE;
			logWarning("Field value processing of @ConfigurationProperty meta-data is "
					+ "not supported");
		}
	}

	private void logWarning(String msg) {
		this.processingEnv.getMessager().printMessage(Kind.WARNING, msg);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		Elements elementUtils = this.processingEnv.getElementUtils();
		for (Element element : roundEnv.getElementsAnnotatedWith(elementUtils
				.getTypeElement(configurationPropertiesAnnotation()))) {
			processElement(element);
		}
		if (roundEnv.processingOver()) {
			writeMetaData(this.metadata);
		}
		return false;
	}

	private void processElement(Element element) {
		AnnotationMirror annotation = getAnnotation(element,
				configurationPropertiesAnnotation());
		String prefix = getPrefix(annotation);
		if (annotation != null) {
			if (element instanceof TypeElement) {
				processAnnotatedTypeElement(prefix, (TypeElement) element);
			}
			else if (element instanceof ExecutableElement) {
				processExecutableElement(prefix, (ExecutableElement) element);
			}
		}
	}

	private void processAnnotatedTypeElement(String prefix, TypeElement element) {
		String type = this.typeUtils.getType(element);
		this.metadata.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element);
	}

	private void processExecutableElement(String prefix, ExecutableElement element) {
		if (element.getModifiers().contains(Modifier.PUBLIC)
				&& (TypeKind.VOID != element.getReturnType().getKind())) {
			Element returns = this.processingEnv.getTypeUtils().asElement(
					element.getReturnType());
			if (returns instanceof TypeElement) {
				this.metadata.add(ItemMetadata.newGroup(prefix,
						this.typeUtils.getType(returns),
						this.typeUtils.getType(element.getEnclosingElement()),
						element.toString()));
				processTypeElement(prefix, (TypeElement) returns);
			}
		}
	}

	private void processTypeElement(String prefix, TypeElement element) {
		TypeElementMembers members = new TypeElementMembers(this.processingEnv, element);
		Map<String, Object> fieldValues = getFieldValues(element);
		processSimpleTypes(prefix, element, members, fieldValues);
		processNestedTypes(prefix, element, members);
	}

	private Map<String, Object> getFieldValues(TypeElement element) {
		try {
			return this.fieldValuesParser.getFieldValues(element);
		}
		catch (Exception ex) {
			return Collections.emptyMap();
		}
	}

	private void processSimpleTypes(String prefix, TypeElement element,
			TypeElementMembers members, Map<String, Object> fieldValues) {
		for (Map.Entry<String, ExecutableElement> entry : members.getPublicGetters()
				.entrySet()) {
			String name = entry.getKey();
			ExecutableElement getter = entry.getValue();
			ExecutableElement setter = members.getPublicSetters().get(name);
			VariableElement field = members.getFields().get(name);
			Element returnType = this.processingEnv.getTypeUtils().asElement(
					getter.getReturnType());
			boolean isExcluded = this.elementExcludeFilter.isExcluded(returnType);
			boolean isNested = isNested(returnType, field, element);
			boolean isCollection = this.typeUtils.isCollectionOrMap(getter
					.getReturnType());
			if (!isExcluded && !isNested && (setter != null || isCollection)) {
				String dataType = this.typeUtils.getType(getter.getReturnType());
				String sourceType = this.typeUtils.getType(element);
				String description = this.typeUtils.getJavaDoc(field);
				Object defaultValue = fieldValues.get(name);
				boolean deprecated = hasDeprecateAnnotation(getter)
						|| hasDeprecateAnnotation(setter)
						|| hasDeprecateAnnotation(element);
				this.metadata.add(ItemMetadata.newProperty(prefix, name, dataType,
						sourceType, null, description, defaultValue, deprecated));
			}
		}
	}

	private void processNestedTypes(String prefix, TypeElement element,
			TypeElementMembers members) {
		for (Map.Entry<String, ExecutableElement> entry : members.getPublicGetters()
				.entrySet()) {
			String name = entry.getKey();
			ExecutableElement getter = entry.getValue();
			VariableElement field = members.getFields().get(name);
			Element returnType = this.processingEnv.getTypeUtils().asElement(
					getter.getReturnType());
			AnnotationMirror annotation = getAnnotation(getter,
					configurationPropertiesAnnotation());
			boolean isNested = isNested(returnType, field, element);
			if (returnType != null && returnType instanceof TypeElement
					&& annotation == null && isNested) {
				String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, name);
				this.metadata.add(ItemMetadata.newGroup(nestedPrefix,
						this.typeUtils.getType(returnType),
						this.typeUtils.getType(element), getter.toString()));
				processTypeElement(nestedPrefix, (TypeElement) returnType);
			}
		}
	}

	private boolean isNested(Element returnType, VariableElement field,
			TypeElement element) {
		if (getAnnotation(field, nestedConfigurationPropertyAnnotation()) != null) {
			return true;
		}
		return this.typeUtils.isEnclosedIn(returnType, element)
				&& returnType.getKind() != ElementKind.ENUM;
	}

	private boolean hasDeprecateAnnotation(Element element) {
		return getAnnotation(element, "java.lang.Deprecated") != null;
	}

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

	private String getPrefix(AnnotationMirror annotation) {
		Map<String, Object> elementValues = getAnnotationElementValues(annotation);
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

	private Map<String, Object> getAnnotationElementValues(AnnotationMirror annotation) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation
				.getElementValues().entrySet()) {
			values.put(entry.getKey().getSimpleName().toString(), entry.getValue()
					.getValue());
		}
		return values;
	}

	protected void writeMetaData(ConfigurationMetadata metadata) {
		metadata = mergeManualMetadata(metadata);
		if (!metadata.getItems().isEmpty()) {
			try {
				FileObject resource = this.processingEnv.getFiler().createResource(
						StandardLocation.CLASS_OUTPUT, "",
						"META-INF/spring-configuration-metadata.json");
				OutputStream outputStream = resource.openOutputStream();
				try {
					new JsonMarshaller().write(metadata, outputStream);
				}
				finally {
					outputStream.close();
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	private ConfigurationMetadata mergeManualMetadata(ConfigurationMetadata metadata) {
		try {
			FileObject manualMetadata = this.processingEnv.getFiler().getResource(
					StandardLocation.CLASS_PATH, "",
					"META-INF/additional-spring-configuration-metadata.json");
			if (!"file".equals(manualMetadata.toUri().getScheme())) {
				// We only want local files, not any classpath jars
				return metadata;
			}
			InputStream inputStream = manualMetadata.openInputStream();
			try {
				ConfigurationMetadata merged = new ConfigurationMetadata(metadata);
				try {
					merged.addAll(new JsonMarshaller().read(inputStream));
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
				return merged;
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			return metadata;
		}
	}

}
