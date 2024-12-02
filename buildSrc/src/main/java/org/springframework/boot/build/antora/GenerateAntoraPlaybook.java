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

package org.springframework.boot.build.antora;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.boot.build.AntoraConventions;
import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration.ZipContentsCollector.AlwaysInclude;

/**
 * Task to generate a local Antora playbook.
 *
 * @author Phillip Webb
 */
public abstract class GenerateAntoraPlaybook extends DefaultTask {

	private static final String GENERATED_DOCS = "build/generated/docs/";

	private final Path root;

	private final Provider<String> playbookOutputDir;

	private final String version;

	private final AntoraExtensions antoraExtensions;

	private final AsciidocExtensions asciidocExtensions;

	private final ContentSource contentSource;

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	public GenerateAntoraPlaybook() {
		this.root = toRealPath(getProject().getRootDir().toPath());
		this.antoraExtensions = getProject().getObjects().newInstance(AntoraExtensions.class, this.root);
		this.asciidocExtensions = getProject().getObjects().newInstance(AsciidocExtensions.class);
		this.version = getProject().getVersion().toString();
		this.playbookOutputDir = configurePlaybookOutputDir(getProject());
		this.contentSource = getProject().getObjects().newInstance(ContentSource.class, this.root);
		setGroup("Documentation");
		setDescription("Generates an Antora playbook.yml file for local use");
		getOutputFile().convention(getProject().getLayout()
			.getBuildDirectory()
			.file("generated/docs/antora-playbook/antora-playbook.yml"));
		this.contentSource.addStartPath(getProject()
			.provider(() -> getProject().getLayout().getProjectDirectory().dir(AntoraConventions.ANTORA_SOURCE_DIR)));
	}

	@Nested
	public AntoraExtensions getAntoraExtensions() {
		return this.antoraExtensions;
	}

	@Nested
	public AsciidocExtensions getAsciidocExtensions() {
		return this.asciidocExtensions;
	}

	@Nested
	public ContentSource getContentSource() {
		return this.contentSource;
	}

	private Provider<String> configurePlaybookOutputDir(Project project) {
		Path siteDirectory = getProject().getLayout().getBuildDirectory().dir("site").get().getAsFile().toPath();
		return project.provider(() -> {
			Path playbookDir = toRealPath(getOutputFile().get().getAsFile().toPath()).getParent();
			Path outputDir = toRealPath(siteDirectory);
			return "." + File.separator + playbookDir.relativize(outputDir).toString();
		});
	}

	@TaskAction
	public void writePlaybookYml() throws IOException {
		File file = getOutputFile().get().getAsFile();
		file.getParentFile().mkdirs();
		try (FileWriter out = new FileWriter(file)) {
			createYaml().dump(getData(), out);
		}
	}

	private Map<String, Object> getData() throws IOException {
		Map<String, Object> data = loadPlaybookTemplate();
		addExtensions(data);
		addSources(data);
		addDir(data);
		return data;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadPlaybookTemplate() throws IOException {
		try (InputStream resource = getClass().getResourceAsStream("antora-playbook-template.yml")) {
			return createYaml().loadAs(resource, LinkedHashMap.class);
		}
	}

	@SuppressWarnings("unchecked")
	private void addExtensions(Map<String, Object> data) {
		Map<String, Object> antora = (Map<String, Object>) data.get("antora");
		antora.put("extensions", Extensions.antora((extensions) -> {
			extensions.xref(
					(xref) -> xref.stub(this.antoraExtensions.getXref().getStubs().getOrElse(Collections.emptyList())));
			extensions.zipContentsCollector((zipContentsCollector) -> {
				zipContentsCollector.versionFile("gradle.properties");
				zipContentsCollector.locations(this.antoraExtensions.getZipContentsCollector()
					.getLocations()
					.getOrElse(Collections.emptyList()));
				zipContentsCollector
					.alwaysInclude(this.antoraExtensions.getZipContentsCollector().getAlwaysInclude().getOrNull());
			});
			extensions.rootComponent((rootComponent) -> rootComponent.name("boot"));
		}));
		Map<String, Object> asciidoc = (Map<String, Object>) data.get("asciidoc");
		List<String> asciidocExtensions = Extensions.asciidoc();
		if (this.asciidocExtensions.getExcludeJavadocExtension().getOrElse(Boolean.FALSE)) {
			asciidocExtensions = new ArrayList<>(asciidocExtensions);
			asciidocExtensions.remove("@springio/asciidoctor-extensions/javadoc-extension");
		}
		asciidoc.put("extensions", asciidocExtensions);
	}

	private void addSources(Map<String, Object> data) {
		List<Map<String, Object>> contentSources = getList(data, "content.sources");
		contentSources.add(createContentSource());
	}

	private Map<String, Object> createContentSource() {
		Map<String, Object> source = new LinkedHashMap<>();
		Path playbookPath = getOutputFile().get().getAsFile().toPath().getParent();
		StringBuilder url = new StringBuilder(".");
		this.root.relativize(playbookPath).normalize().forEach((path) -> url.append(File.separator).append(".."));
		source.put("url", url.toString());
		source.put("branches", "HEAD");
		source.put("version", this.version);
		source.put("start_paths", this.contentSource.getStartPaths().get());
		return source;
	}

	private void addDir(Map<String, Object> data) {
		data.put("output", Map.of("dir", this.playbookOutputDir.get()));
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getList(Map<String, Object> data, String location) {
		return (List<T>) get(data, location);
	}

	@SuppressWarnings("unchecked")
	private Object get(Map<String, Object> data, String location) {
		Object result = data;
		String[] keys = location.split("\\.");
		for (String key : keys) {
			result = ((Map<String, Object>) result).get(key);
		}
		return result;
	}

	private Yaml createYaml() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		return new Yaml(options);
	}

	private static Path toRealPath(Path path) {
		try {
			return Files.exists(path) ? path.toRealPath() : path;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public abstract static class AntoraExtensions {

		private final Xref xref;

		private final ZipContentsCollector zipContentsCollector;

		@Inject
		public AntoraExtensions(ObjectFactory objects, Path root) {
			this.xref = objects.newInstance(Xref.class);
			this.zipContentsCollector = objects.newInstance(ZipContentsCollector.class, root);
		}

		@Nested
		public Xref getXref() {
			return this.xref;
		}

		@Nested
		public ZipContentsCollector getZipContentsCollector() {
			return this.zipContentsCollector;
		}

		public abstract static class Xref {

			@Input
			@Optional
			public abstract ListProperty<String> getStubs();

		}

		public abstract static class ZipContentsCollector {

			private final Provider<List<String>> locations;

			@Inject
			public ZipContentsCollector(Project project, Path root) {
				this.locations = configureZipContentCollectorLocations(project, root);
			}

			private Provider<List<String>> configureZipContentCollectorLocations(Project project, Path root) {
				ListProperty<String> locations = project.getObjects().listProperty(String.class);
				Path relativeProjectPath = relativize(root, project.getProjectDir().toPath());
				String locationName = project.getName() + "-${version}-${name}-${classifier}.zip";
				locations.add(project
					.provider(() -> relativeProjectPath.resolve(GENERATED_DOCS + "antora-content/" + locationName)
						.toString()));
				locations.addAll(getDependencies().map((dependencies) -> dependencies.stream()
					.map((dependency) -> relativeProjectPath
						.resolve(GENERATED_DOCS + "antora-dependencies-content/" + dependency + "/" + locationName))
					.map(Path::toString)
					.toList()));
				return locations;
			}

			private static Path relativize(Path root, Path subPath) {
				return toRealPath(root).relativize(toRealPath(subPath)).normalize();
			}

			@Input
			@Optional
			public abstract ListProperty<AlwaysInclude> getAlwaysInclude();

			@Input
			@Optional
			public Provider<List<String>> getLocations() {
				return this.locations;
			}

			@Input
			@Optional
			public abstract SetProperty<String> getDependencies();

		}

	}

	public abstract static class AsciidocExtensions {

		@Inject
		public AsciidocExtensions() {

		}

		@Input
		@Optional
		public abstract Property<Boolean> getExcludeJavadocExtension();

	}

	public abstract static class ContentSource {

		private final Path root;

		@Inject
		public ContentSource(Path root) {
			this.root = root;
		}

		@Input
		public abstract ListProperty<String> getStartPaths();

		void addStartPath(Provider<Directory> startPath) {
			getStartPaths()
				.add(startPath.map((dir) -> this.root.relativize(toRealPath(dir.getAsFile().toPath())).toString()));
		}

	}

}
