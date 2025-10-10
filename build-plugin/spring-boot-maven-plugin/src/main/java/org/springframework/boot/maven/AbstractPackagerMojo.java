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

package org.springframework.boot.maven;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LayoutFactory;
import org.springframework.boot.loader.tools.Layouts.Expanded;
import org.springframework.boot.loader.tools.Layouts.Jar;
import org.springframework.boot.loader.tools.Layouts.None;
import org.springframework.boot.loader.tools.Layouts.War;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Packager;
import org.springframework.boot.loader.tools.layer.CustomLayers;

/**
 * Abstract base class for classes that work with an {@link Packager}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 2.3.0
 */
public abstract class AbstractPackagerMojo extends AbstractDependencyFilterMojo {

	private static final org.springframework.boot.loader.tools.Layers IMPLICIT_LAYERS = org.springframework.boot.loader.tools.Layers.IMPLICIT;

	/**
	 * The Maven project.
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	@SuppressWarnings("NullAway.Init")
	protected MavenProject project;

	/**
	 * The Maven session.
	 * @since 2.4.0
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	@SuppressWarnings("NullAway.Init")
	protected MavenSession session;

	/**
	 * Maven project helper utils.
	 * @since 1.0.0
	 */
	protected final MavenProjectHelper projectHelper;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a {@code main} method will be used.
	 * @since 1.0.0
	 */
	@Parameter
	private @Nullable String mainClass;

	/**
	 * Exclude Spring Boot devtools from the repackaged archive.
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.repackage.excludeDevtools", defaultValue = "true")
	private boolean excludeDevtools = true;

	/**
	 * Exclude Spring Boot dev services from the repackaged archive.
	 * @since 3.1.0
	 */
	@Parameter(property = "spring-boot.repackage.excludeDockerCompose", defaultValue = "true")
	private boolean excludeDockerCompose = true;

	/**
	 * Include system scoped dependencies.
	 * @since 1.4.0
	 */
	@Parameter(defaultValue = "false")
	public boolean includeSystemScope;

	/**
	 * Include optional dependencies.
	 * @since 3.5.7
	 */
	@Parameter(defaultValue = "false")
	public boolean includeOptional;

	/**
	 * Include JAR tools.
	 * @since 3.3.0
	 */
	@Parameter(defaultValue = "true")
	public boolean includeTools = true;

	/**
	 * Layer configuration with options to disable layer creation, exclude layer tools
	 * jar, and provide a custom layers configuration file.
	 * @since 2.3.0
	 */
	@Parameter
	private Layers layers = new Layers();

	protected AbstractPackagerMojo(MavenProjectHelper projectHelper) {
		this.projectHelper = projectHelper;
	}

	/**
	 * Return the type of archive that should be packaged by this MOJO.
	 * @return {@code null}, indicating a layout type will be chosen based on the original
	 * archive type
	 */
	protected @Nullable LayoutType getLayout() {
		return null;
	}

	/**
	 * Return the layout factory that will be used to determine the {@link LayoutType} if
	 * no explicit layout is set.
	 * @return {@code null}, indicating a default layout factory will be chosen
	 */
	protected @Nullable LayoutFactory getLayoutFactory() {
		return null;
	}

	/**
	 * Return a {@link Packager} configured for this MOJO.
	 * @param <P> the packager type
	 * @param supplier a packager supplier
	 * @return a configured packager
	 */
	protected <P extends Packager> P getConfiguredPackager(Supplier<P> supplier) {
		P packager = supplier.get();
		packager.setLayoutFactory(getLayoutFactory());
		packager.addMainClassTimeoutWarningListener(new LoggingMainClassTimeoutWarningListener(this::getLog));
		packager.setMainClass(this.mainClass);
		LayoutType layout = getLayout();
		if (layout != null) {
			getLog().info("Layout: " + layout);
			packager.setLayout(layout.layout());
		}
		if (this.layers.isEnabled()) {
			packager.setLayers((this.layers.getConfiguration() != null)
					? getCustomLayers(this.layers.getConfiguration()) : IMPLICIT_LAYERS);
		}
		packager.setIncludeRelevantJarModeJars(getIncludeRelevantJarModeJars());
		return packager;
	}

	private boolean getIncludeRelevantJarModeJars() {
		return this.includeTools;
	}

	private CustomLayers getCustomLayers(File configuration) {
		try {
			Document document = getDocumentIfAvailable(configuration);
			return new CustomLayersProvider().getLayers(document);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Failed to process custom layers configuration " + configuration.getAbsolutePath(), ex);
		}
	}

	private Document getDocumentIfAvailable(File xmlFile) throws Exception {
		InputSource inputSource = new InputSource(new FileInputStream(xmlFile));
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(inputSource);
	}

	/**
	 * Return {@link Libraries} that the packager can use.
	 * @param unpacks any libraries that require unpack
	 * @return the libraries to use
	 * @throws MojoExecutionException on execution error
	 */
	protected final Libraries getLibraries(@Nullable Collection<Dependency> unpacks) throws MojoExecutionException {
		Set<Artifact> artifacts = this.project.getArtifacts();
		Set<Artifact> includedArtifacts = filterDependencies(artifacts, getAdditionalFilters());
		return new ArtifactsLibraries(artifacts, includedArtifacts, this.session.getProjects(), unpacks, getLog());
	}

	private ArtifactsFilter[] getAdditionalFilters() {
		List<ArtifactsFilter> filters = new ArrayList<>();
		if (this.excludeDevtools) {
			filters.add(DEVTOOLS_EXCLUDE_FILTER);
		}
		if (this.excludeDockerCompose) {
			filters.add(DOCKER_COMPOSE_EXCLUDE_FILTER);
		}
		if (!this.includeSystemScope) {
			filters.add(new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
		}
		if (!this.includeOptional) {
			filters.add(DependencyFilter.exclude(Artifact::isOptional));
		}
		return filters.toArray(new ArtifactsFilter[0]);
	}

	/**
	 * Return the source {@link Artifact} to repackage. If a classifier is specified and
	 * an artifact with that classifier exists, it is used. Otherwise, the main artifact
	 * is used.
	 * @param classifier the artifact classifier
	 * @return the source artifact to repackage
	 */
	protected Artifact getSourceArtifact(@Nullable String classifier) {
		Artifact sourceArtifact = getArtifact(classifier);
		return (sourceArtifact != null) ? sourceArtifact : this.project.getArtifact();
	}

	private @Nullable Artifact getArtifact(@Nullable String classifier) {
		if (classifier != null) {
			for (Artifact attachedArtifact : this.project.getAttachedArtifacts()) {
				if (classifier.equals(attachedArtifact.getClassifier()) && attachedArtifact.getFile() != null
						&& attachedArtifact.getFile().isFile()) {
					return attachedArtifact;
				}
			}
		}
		return null;
	}

	protected File getTargetFile(String finalName, @Nullable String classifier, File targetDirectory) {
		String classifierSuffix = (classifier != null) ? classifier.trim() : "";
		if (!classifierSuffix.isEmpty() && !classifierSuffix.startsWith("-")) {
			classifierSuffix = "-" + classifierSuffix;
		}
		if (!targetDirectory.exists()) {
			targetDirectory.mkdirs();
		}
		return new File(targetDirectory,
				finalName + classifierSuffix + "." + this.project.getArtifact().getArtifactHandler().getExtension());
	}

	/**
	 * Archive layout types.
	 */
	public enum LayoutType {

		/**
		 * Jar Layout.
		 */
		JAR(new Jar()),

		/**
		 * War Layout.
		 */
		WAR(new War()),

		/**
		 * Zip Layout.
		 */
		ZIP(new Expanded()),

		/**
		 * Directory Layout.
		 */
		DIR(new Expanded()),

		/**
		 * No Layout.
		 */
		NONE(new None());

		private final Layout layout;

		LayoutType(Layout layout) {
			this.layout = layout;
		}

		public Layout layout() {
			return this.layout;
		}

	}

}
