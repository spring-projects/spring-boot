/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.build.antora;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Antora and Asciidoc extensions used by Spring Boot.
 *
 * @author Phillip Webb
 */
public final class Extensions {

	private static final String ROOT_COMPONENT_EXTENSION = "@springio/antora-extensions/root-component-extension";

	private static final List<Extension> antora;
	static {
		List<Extension> extensions = new ArrayList<>();
		extensions.add(new Extension("@springio/antora-extensions", ROOT_COMPONENT_EXTENSION,
				"@springio/antora-extensions/static-page-extension",
				"@springio/antora-extensions/override-navigation-builder-extension"));
		extensions.add(new Extension("@springio/antora-xref-extension"));
		extensions.add(new Extension("@springio/antora-zip-contents-collector-extension"));
		antora = List.copyOf(extensions);
	}

	private static final List<Extension> asciidoc;
	static {
		List<Extension> extensions = new ArrayList<>();
		extensions.add(new Extension("@asciidoctor/tabs"));
		extensions.add(new Extension("@springio/asciidoctor-extensions", "@springio/asciidoctor-extensions",
				"@springio/asciidoctor-extensions/javadoc-extension",
				"@springio/asciidoctor-extensions/configuration-properties-extension",
				"@springio/asciidoctor-extensions/section-ids-extension"));
		asciidoc = List.copyOf(extensions);
	}

	private static final Map<String, String> localOverrides = Collections.emptyMap();

	private Extensions() {
	}

	static List<Map<String, Object>> antora(Consumer<AntoraExtensionsConfiguration> extensions) {
		AntoraExtensionsConfiguration result = new AntoraExtensionsConfiguration(
				antora.stream().flatMap(Extension::names).sorted().toList());
		extensions.accept(result);
		return result.config();
	}

	static List<String> asciidoc() {
		return asciidoc.stream().flatMap(Extension::names).sorted().toList();
	}

	private record Extension(String name, String... includeNames) {

		Stream<String> names() {
			return (this.includeNames.length != 0) ? Arrays.stream(this.includeNames) : Stream.of(this.name);
		}

	}

	static final class AntoraExtensionsConfiguration {

		private final Map<String, Map<String, Object>> extensions = new TreeMap<>();

		private AntoraExtensionsConfiguration(List<String> names) {
			names.forEach((name) -> this.extensions.put(name, null));
		}

		void xref(Consumer<Xref> xref) {
			xref.accept(new Xref());
		}

		void zipContentsCollector(Consumer<ZipContentsCollector> zipContentsCollector) {
			zipContentsCollector.accept(new ZipContentsCollector());
		}

		void rootComponent(Consumer<RootComponent> rootComponent) {
			rootComponent.accept(new RootComponent());
		}

		List<Map<String, Object>> config() {
			List<Map<String, Object>> config = new ArrayList<>();
			Map<String, Map<String, Object>> orderedExtensions = new LinkedHashMap<>(this.extensions);
			// The root component extension must be last
			Map<String, Object> rootComponentConfig = orderedExtensions.remove(ROOT_COMPONENT_EXTENSION);
			orderedExtensions.put(ROOT_COMPONENT_EXTENSION, rootComponentConfig);
			orderedExtensions.forEach((name, customizations) -> {
				Map<String, Object> extensionConfig = new LinkedHashMap<>();
				extensionConfig.put("require", localOverrides.getOrDefault(name, name));
				if (customizations != null) {
					extensionConfig.putAll(customizations);
				}
				config.add(extensionConfig);
			});
			return List.copyOf(config);
		}

		abstract class Customizer {

			private final String name;

			Customizer(String name) {
				this.name = name;
			}

			protected void customize(String key, Object value) {
				AntoraExtensionsConfiguration.this.extensions.computeIfAbsent(this.name, (name) -> new TreeMap<>())
					.put(key, value);
			}

		}

		class Xref extends Customizer {

			Xref() {
				super("@springio/antora-xref-extension");
			}

			void stub(List<String> stub) {
				if (stub != null && !stub.isEmpty()) {
					customize("stub", stub);
				}
			}

		}

		class ZipContentsCollector extends Customizer {

			ZipContentsCollector() {
				super("@springio/antora-zip-contents-collector-extension");
			}

			void versionFile(String versionFile) {
				customize("version_file", versionFile);
			}

			void locations(List<String> locations) {
				customize("locations", locations);
			}

			void alwaysInclude(List<AlwaysInclude> alwaysInclude) {
				if (alwaysInclude != null && !alwaysInclude.isEmpty()) {
					customize("always_include", alwaysInclude.stream().map(AlwaysInclude::asMap).toList());
				}
			}

			record AlwaysInclude(String name, String classifier) implements Serializable {

				private Map<String, String> asMap() {
					return new TreeMap<>(Map.of("name", name(), "classifier", classifier()));
				}

			}

		}

		class RootComponent extends Customizer {

			RootComponent() {
				super(ROOT_COMPONENT_EXTENSION);
			}

			void name(String name) {
				customize("root_component_name", name);
			}

		}

	}

}
