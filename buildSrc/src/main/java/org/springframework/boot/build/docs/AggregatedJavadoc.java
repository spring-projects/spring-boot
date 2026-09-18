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

package org.springframework.boot.build.docs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.UncheckedIOException;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.util.StringUtils;

/**
 * Specialized {@link Javadoc} task for aggregated javadoc generation.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public abstract class AggregatedJavadoc extends Javadoc {

	private static final Set<String> JAVADOC_LIST_FILES = Set.of("package-list", "element-list");

	private static final Map<Pattern, String> BUNDLED_JAVADOC_LISTS;
	static {
		Map<Pattern, String> bundledJavadocLists = new LinkedHashMap<>();
		bundledJavadocLists.put(Pattern.compile("^tomcat-embed-core-.*-javadoc\\.jar$"), "tomcat");
		bundledJavadocLists.put(Pattern.compile("^log4j-api-.*-javadoc\\.jar$"), "log4j-api");
		bundledJavadocLists.put(Pattern.compile("^log4j-core-.*-javadoc\\.jar$"), "log4j-core");
		BUNDLED_JAVADOC_LISTS = Collections.unmodifiableMap(bundledJavadocLists);
	}

	private static final List<String> IGNORED_PACKAGES;
	static {
		List<String> ignoredPackages = new ArrayList<>();
		ignoredPackages.add("com.datastax.oss.driver.api.core");
		ignoredPackages.add("com.datastax.oss.driver.api.core.config");
		ignoredPackages.add("kotlinx.serialization.json");
		ignoredPackages.add("org.apache.activemq.artemis.core.server.embedded");
		ignoredPackages.add("org.hibernate.engine.transaction.jta.platform.internal");
		IGNORED_PACKAGES = Collections.unmodifiableList(ignoredPackages);
	}

	public void javadocJars(Provider<Set<ResolvedArtifactResult>> artifacts) {
		getJavadocJarFiles().set(artifacts.map(this::asJavadocJarFiles));
		getJavadocJarNames().set(artifacts.map(this::asJavadocJarNames));
	}

	private List<File> asJavadocJarFiles(Set<ResolvedArtifactResult> artifacts) {
		return artifacts.stream().map(ResolvedArtifactResult::getFile).toList();
	}

	private List<String> asJavadocJarNames(Set<ResolvedArtifactResult> artifacts) {
		return artifacts.stream().map(this::asJavadocJarName).toList();
	}

	private String asJavadocJarName(ResolvedArtifactResult artifact) {
		ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
		if (identifier instanceof ModuleComponentIdentifier moduleIdentifier) {
			return "%s-%s-javadoc.jar".formatted(moduleIdentifier.getModule(), moduleIdentifier.getVersion());
		}
		return identifier.getDisplayName();
	}

	@Classpath
	@InputFiles
	public abstract ConfigurableFileCollection getResolvedBom();

	@InputFiles
	abstract ListProperty<File> getJavadocJarFiles();

	@Input
	abstract ListProperty<String> getJavadocJarNames();

	@Override
	protected void generate() {
		StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) getOptions();
		options.doclet("io.spring.javaformat.doclet.OfflineLinksDoclet");
		options.addBooleanOption("quiet", true);
		options.addBooleanOption("Xdoclint:all,-missing", true);
		options.addBooleanOption("Werror", true);
		options.links("https://docs.oracle.com/en/java/javase/17/docs/api/");
		configureOfflineLinks(options);
		super.generate();
	}

	private void configureOfflineLinks(StandardJavadocDocletOptions options) {
		ResolvedBom resolvedBom = ResolvedBom.readFrom(getResolvedBom().getSingleFile());
		File javadocListsDir = getProject().getLayout().getBuildDirectory().get().dir("docs/javadoclists").getAsFile();
		copyElementListResourceIfMissing(javadocListsDir, "javase");
		extractJavdocListFiles(javadocListsDir);
		if (getProject().getGradle().getStartParameter().getLogLevel() == LogLevel.DEBUG) {
			options.addBooleanOption("offlinelinks-debug", true);
		}
		options.addStringOption("-link-modularity-mismatch", "info");
		options.addStringOption("offlinelinks-ignore-packages",
				IGNORED_PACKAGES.stream().collect(Collectors.joining(",")));
		options.addStringOption("offlinelinks-source", new File(javadocListsDir, "@name@").getAbsolutePath());
		options.linksOffline("https://docs.oracle.com/en/java/javase/17/docs/api", "javase");
		resolvedBom.offlineJavadocLinks()
			.forEach((url, jars) -> addOfflineLinksOption(options, javadocListsDir, url, jars));
	}

	private void addOfflineLinksOption(StandardJavadocDocletOptions options, File javadocListsDir, URI url,
			List<String> jars) {
		for (Map.Entry<Pattern, String> bundled : BUNDLED_JAVADOC_LISTS.entrySet()) {
			if (matchesBundled(bundled.getKey(), jars)) {
				copyElementListResourceIfMissing(javadocListsDir, bundled.getValue());
				options.linksOffline(url.toString(), bundled.getValue());
				return;
			}
		}
		String listOfJavadocJars = jars.stream()
			.filter((jar) -> Files.isDirectory(javadocListsDir.toPath().resolve(jar)))
			.collect(Collectors.joining(","));
		if (StringUtils.hasLength(listOfJavadocJars)) {
			options.linksOffline(url.toString(), listOfJavadocJars);
		}
	}

	private boolean matchesBundled(Pattern pattern, List<String> jars) {
		return jars.stream().anyMatch((jar) -> pattern.matcher(jar).matches());
	}

	private void copyElementListResourceIfMissing(File javadocListsDir, String name) {
		try {
			Path directory = javadocListsDir.toPath().resolve(name);
			if (!Files.exists(directory)) {
				Files.createDirectories(directory);
			}
			Path file = directory.resolve("element-list");
			if (!Files.exists(file)) {
				Files.copy(getClass().getResourceAsStream(name + "-element-list"), file);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void extractJavdocListFiles(File javadocListsDir) {
		List<File> files = getJavadocJarFiles().get();
		List<String> names = getJavadocJarNames().get();
		for (int i = 0; i < files.size(); i++) {
			FileCollection source = getProject().zipTree(files.get(i)).filter(this::isJavadocListFile);
			File destination = new File(javadocListsDir, names.get(i));
			getProject().copy((copy) -> copy.from(source).into(destination));
		}
	}

	private boolean isJavadocListFile(File file) {
		return JAVADOC_LIST_FILES.contains(file.getName());
	}

}
