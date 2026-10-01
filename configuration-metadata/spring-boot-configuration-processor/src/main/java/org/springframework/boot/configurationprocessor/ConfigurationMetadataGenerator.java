/*
 * Copyright 2012-present the original author or authors.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.tools.Diagnostic.Kind;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.InvalidConfigurationMetadataException;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemIgnore;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * Generates the meta-data of {@code @ConfigurationProperties} from a language-neutral
 * model of the source code, so that it can be shared by the processors of several
 * languages.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Kris De Volder
 * @author Jonas Keßler
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Areg Iazychian
 * @since 4.2.0
 */
public class ConfigurationMetadataGenerator {

	private final ProcessingContext context;

	private final MetadataGenerationEnvironment metadataEnv;

	private final String endpointAccessEnum;

	private final MetadataStore metadataStore;

	private final MetadataCollectors metadataCollectors;

	private final MetadataCollector metadataCollector;

	private final DescriptionCache descriptionCache;

	private final Set<String> classFileBackedTypes = new HashSet<>();

	/**
	 * Create a new {@link ConfigurationMetadataGenerator} instance.
	 * @param context the processing context
	 */
	public ConfigurationMetadataGenerator(ProcessingContext context) {
		this(context,
				new MetadataGenerationEnvironment(context,
						ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.NESTED_CONFIGURATION_PROPERTY_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.CONSTRUCTOR_BINDING_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.AUTOWIRED_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.DEFAULT_VALUE_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATIONS,
						ConfigurationMetadataAnnotationProcessor.READ_OPERATION_ANNOTATION,
						ConfigurationMetadataAnnotationProcessor.NAME_ANNOTATION),
				ConfigurationMetadataAnnotationProcessor.ENDPOINT_ACCESS_ENUM);
	}

	ConfigurationMetadataGenerator(ProcessingContext context, MetadataGenerationEnvironment metadataEnv,
			String endpointAccessEnum) {
		this.context = context;
		this.metadataEnv = metadataEnv;
		this.endpointAccessEnum = endpointAccessEnum;
		this.metadataStore = new MetadataStore(context);
		this.metadataCollectors = new MetadataCollectors(context);
		this.metadataCollector = this.metadataCollectors.getModuleMetadataCollector();
		String cacheLocation = context.getOptions()
			.get(ConfigurationMetadataAnnotationProcessor.DESCRIPTION_CACHE_LOCATION_OPTION);
		this.descriptionCache = (cacheLocation != null) ? new DescriptionCache(cacheLocation) : null;
	}

	/**
	 * Collect the meta-data of the declarations of the given round.
	 * @param round the round to process
	 */
	public void process(ProcessingRound round) {
		this.metadataCollectors.processing(round);
		String annotationType = this.metadataEnv.getConfigurationPropertiesAnnotationName();
		for (Declaration element : round.getDeclarationsAnnotatedWith(annotationType)) {
			processElement(element);
		}
		String sourceAnnotationType = this.metadataEnv.getConfigurationPropertiesSourceAnnotationName();
		for (Declaration element : round.getDeclarationsAnnotatedWith(sourceAnnotationType)) {
			if (element instanceof TypeDeclaration typeElement) {
				MetadataCollector metadataCollector = this.metadataCollectors.getMetadataCollector(typeElement);
				processSourceElement(metadataCollector, "", typeElement);
			}
		}
		for (String endpointType : this.metadataEnv.getEndpointAnnotationNames()) {
			if (this.context.getTypeDeclaration(endpointType) != null) { // Is it
																			// available
				getElementsAnnotatedOrMetaAnnotatedWith(round, endpointType).forEach(this::processEndpoint);
			}
		}
	}

	private Map<TypeDeclaration, List<TypeDeclaration>> getElementsAnnotatedOrMetaAnnotatedWith(ProcessingRound round,
			String annotation) {
		Map<TypeDeclaration, List<TypeDeclaration>> result = new LinkedHashMap<>();
		for (TypeDeclaration element : round.getRootTypes()) {
			List<TypeDeclaration> annotations = this.metadataEnv.getElementsAnnotatedOrMetaAnnotatedWith(element,
					annotation);
			if (!annotations.isEmpty()) {
				result.put(element, annotations);
			}
		}
		return result;
	}

	private void processElement(Declaration element) {
		try {
			AnnotationReference annotation = this.metadataEnv.getConfigurationPropertiesAnnotation(element);
			if (annotation != null) {
				String prefix = getPrefix(annotation);
				if (element instanceof TypeDeclaration typeElement) {
					processAnnotatedTypeElement(prefix, typeElement, new ArrayDeque<>());
				}
				else if (element instanceof MethodDeclaration executableElement) {
					processExecutableElement(prefix, executableElement, new ArrayDeque<>());
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processAnnotatedTypeElement(String prefix, TypeDeclaration element, Deque<TypeDeclaration> seen) {
		String type = element.getQualifiedName();
		this.metadataCollector.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element, null, seen);
	}

	private void processExecutableElement(String prefix, MethodDeclaration element, Deque<TypeDeclaration> seen) {
		if (!element.isPrivate() && !element.getReturnType().isVoid()) {
			TypeDeclaration returns = element.getReturnType().getDeclaration();
			if (returns != null) {
				ItemMetadata group = ItemMetadata.newGroup(prefix, returns.getQualifiedName(),
						element.getEnclosingType().getQualifiedName(), element.getSignature());
				if (this.metadataCollector.hasSimilarGroup(group)) {
					this.context.error("Duplicate @ConfigurationProperties definition for prefix '" + prefix + "'",
							element, null);
				}
				else {
					this.metadataCollector.add(group);
					processTypeElement(prefix, returns, element, seen);
				}
			}
		}
	}

	private void processTypeElement(String prefix, TypeDeclaration element, MethodDeclaration source,
			Deque<TypeDeclaration> seen) {
		if (!seen.contains(element)) {
			seen.push(element);
			if (this.descriptionCache != null && !element.hasSource()) {
				this.classFileBackedTypes.add(element.getQualifiedName());
			}
			new PropertyDescriptorResolver(this.metadataEnv).resolve(element, source).forEach((descriptor) -> {
				this.metadataCollector.add(descriptor.resolveItemMetadata(prefix, this.metadataEnv));
				ItemHint itemHint = descriptor.resolveItemHint(prefix, this.metadataEnv);
				if (itemHint != null) {
					this.metadataCollector.add(itemHint);
				}
				if (descriptor.isNested(this.metadataEnv)) {
					TypeDeclaration nestedTypeElement = descriptor.getType().getDeclaration();
					String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.getName());
					processTypeElement(nestedPrefix, nestedTypeElement, source, seen);
				}
			});
			seen.pop();
		}
	}

	private void processSourceElement(MetadataCollector metadataCollector, String prefix, TypeDeclaration element) {
		new PropertyDescriptorResolver(this.metadataEnv).resolve(element, null).forEach((descriptor) -> {
			metadataCollector.add(descriptor.resolveItemMetadata(prefix, this.metadataEnv));
			if (descriptor.isNested(this.metadataEnv)) {
				TypeDeclaration nestedTypeElement = descriptor.getType().getDeclaration();
				String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.getName());
				processSourceElement(metadataCollector, nestedPrefix, nestedTypeElement);
			}
		});
	}

	private void processEndpoint(TypeDeclaration element, List<TypeDeclaration> annotations) {
		try {
			String annotationName = annotations.get(0).getQualifiedName();
			AnnotationReference annotation = this.metadataEnv.getAnnotation(element, annotationName);
			processEndpoint(annotation, element);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processEndpoint(AnnotationReference annotation, TypeDeclaration element) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		String endpointId = (String) elementValues.get("id");
		if (endpointId == null || endpointId.isEmpty()) {
			return; // Can't process that endpoint
		}
		String endpointKey = ItemMetadata.newItemMetadataPrefix("management.endpoint.", endpointId);
		String defaultAccess = elementValues.getOrDefault("defaultAccess", "unrestricted")
			.toString()
			.toLowerCase(Locale.ENGLISH);
		String type = element.getQualifiedName();
		this.metadataCollector.addIfAbsent(ItemMetadata.newGroup(endpointKey, type, type, null));
		ItemMetadata accessProperty = ItemMetadata.newProperty(endpointKey, "access", this.endpointAccessEnum, type,
				null, "Permitted level of access for the %s endpoint.".formatted(endpointId), defaultAccess, null);
		this.metadataCollector.add(accessProperty,
				(existing) -> checkDefaultAccessValueMatchesExisting(existing, defaultAccess, type));
		if (hasMainReadOperation(element)) {
			this.metadataCollector.addIfAbsent(ItemMetadata.newProperty(endpointKey, "cache.time-to-live",
					Duration.class.getName(), type, null, "Maximum time that a response can be cached.", "0ms", null));
		}
	}

	private void checkDefaultAccessValueMatchesExisting(ItemMetadata existing, String defaultAccess,
			String sourceType) {
		String existingDefaultAccess = (String) existing.getDefaultValue();
		if (!Objects.equals(defaultAccess, existingDefaultAccess)) {
			throw new IllegalStateException(
					"Existing property '%s' from type %s has a conflicting value. Existing value: %s, new value from type %s: %s"
						.formatted(existing.getName(), existing.getSourceType(), existingDefaultAccess, sourceType,
								defaultAccess));
		}
	}

	private boolean hasMainReadOperation(TypeDeclaration element) {
		for (MethodDeclaration method : element.getMethods()) {
			if (this.metadataEnv.getReadOperationAnnotation(method) != null && !method.getReturnType().isVoid()
					&& hasNoOrOptionalParameters(method)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNoOrOptionalParameters(MethodDeclaration method) {
		for (VariableDeclaration parameter : method.getParameters()) {
			if (!parameter.getType().isNullable()) {
				return false;
			}
		}
		return true;
	}

	private String getPrefix(AnnotationReference annotation) {
		String prefix = this.metadataEnv.getAnnotationElementStringValue(annotation, "prefix");
		if (prefix != null) {
			return prefix;
		}
		return this.metadataEnv.getAnnotationElementStringValue(annotation, "value");
	}

	/**
	 * Write the meta-data of the types that have been annotated with
	 * {@code @ConfigurationPropertiesSource}.
	 * @throws Exception if the meta-data cannot be written
	 */
	public void writeSourceMetadata() throws Exception {
		for (TypeDeclaration sourceType : this.metadataCollectors.getSourceTypes()) {
			ConfigurationMetadata metadata = this.metadataCollectors.getMetadataCollector(sourceType).getMetadata();
			metadata = mergeAdditionalMetadata(metadata, () -> this.metadataStore.readAdditionalMetadata(sourceType));
			removeIgnored(metadata);
			if (!metadata.getItems().isEmpty()) {
				this.metadataStore.writeMetadata(metadata, sourceType);
			}
		}
	}

	/**
	 * Write the meta-data that has been collected.
	 * @return the meta-data that has been written or {@code null} if there is none
	 * @throws Exception if the meta-data cannot be written
	 */
	public ConfigurationMetadata writeMetadata() throws Exception {
		ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
		metadata = mergeAdditionalMetadata(metadata, () -> this.metadataStore.readAdditionalMetadata());
		fillCachedDescriptions(metadata);
		removeIgnored(metadata);
		if (!metadata.getItems().isEmpty()) {
			this.metadataStore.writeMetadata(metadata);
			updateDescriptionCache(metadata);
			return metadata;
		}
		return null;
	}

	private void fillCachedDescriptions(ConfigurationMetadata metadata) {
		if (this.descriptionCache == null) {
			return;
		}
		for (ItemMetadata item : metadata.getItems()) {
			if (item.isOfItemType(ItemMetadata.ItemType.PROPERTY) && item.getDescription() == null
					&& item.getSourceType() != null && this.classFileBackedTypes.contains(item.getSourceType())) {
				String cached = this.descriptionCache.getDescription(item.getName());
				if (cached != null) {
					item.setDescription(cached);
				}
			}
		}
	}

	private void updateDescriptionCache(ConfigurationMetadata metadata) {
		if (this.descriptionCache == null) {
			return;
		}
		this.descriptionCache.update(metadata);
	}

	private void removeIgnored(ConfigurationMetadata metadata) {
		for (ItemIgnore itemIgnore : metadata.getIgnored()) {
			metadata.removeMetadata(itemIgnore.getType(), itemIgnore.getName());
		}
	}

	private ConfigurationMetadata mergeAdditionalMetadata(ConfigurationMetadata metadata,
			Supplier<ConfigurationMetadata> additionalMetadataSupplier) {
		try {
			ConfigurationMetadata additionalMetadata = additionalMetadataSupplier.get();
			if (additionalMetadata != null) {
				ConfigurationMetadata merged = new ConfigurationMetadata(metadata);
				merged.merge(additionalMetadata);
				return merged;
			}
			return metadata;
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
		if (kind == Kind.ERROR) {
			this.context.error(msg, null, null);
		}
		else {
			this.context.warn(msg);
		}
	}

}
