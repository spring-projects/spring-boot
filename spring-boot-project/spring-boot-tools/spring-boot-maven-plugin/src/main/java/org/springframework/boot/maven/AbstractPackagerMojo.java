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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
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
 * @since 2.3.0
 */
public abstract class AbstractPackagerMojo extends AbstractDependencyFilterMojo {

	private static final org.springframework.boot.loader.tools.Layers IMPLICIT_LAYERS = org.springframework.boot.loader.tools.Layers.IMPLICIT;

	/**
	 * The Maven project.
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	/**
	 * Maven project helper utils.
	 * @since 1.0.0
	 */
	@Component
	protected MavenProjectHelper projectHelper;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 * @since 1.0.0
	 */
	@Parameter
	private String mainClass;

	/**
	 * The type of archive (which corresponds to how the dependencies are laid out inside
	 * it). Possible values are JAR, WAR, ZIP, DIR, NONE. Defaults to a guess based on the
	 * archive type.
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-boot.repackage.layout")
	private LayoutType layout;

	/**
	 * The layout factory that will be used to create the executable archive if no
	 * explicit layout is set. Alternative layouts implementations can be provided by 3rd
	 * parties.
	 * @since 1.5.0
	 */
	@Parameter
	private LayoutFactory layoutFactory;

	/**
	 * Exclude Spring Boot devtools from the repackaged archive.
	 * @since 1.3.0
	 */
	@Parameter(property = "spring-boot.repackage.excludeDevtools", defaultValue = "true")
	private boolean excludeDevtools = true;

	/**
	 * Include system scoped dependencies.
	 * @since 1.4.0
	 */
	@Parameter(defaultValue = "false")
	public boolean includeSystemScope;

	/**
	 * Layer configuration with the option to exclude layer tools jar.
	 * @since 2.3.0
	 */
	@Parameter
	private Layers layers;

	/**
	 * Return a {@link Packager} configured for this MOJO.
	 * @param <P> the packager type
	 * @param supplier a packager supplier
	 * @return a configured packager
	 */
	protected <P extends Packager> P getConfiguredPackager(Supplier<P> supplier) {
		P packager = supplier.get();
		packager.setLayoutFactory(this.layoutFactory);
		packager.addMainClassTimeoutWarningListener(new LoggingMainClassTimeoutWarningListener(this::getLog));
		packager.setMainClass(this.mainClass);
		if (this.layout != null) {
			getLog().info("Layout: " + this.layout);
			packager.setLayout(this.layout.layout());
		}
		if (this.layers == null) {
			packager.setLayers(IMPLICIT_LAYERS);
		}
		else if (this.layers.isEnabled()) {
			packager.setLayers((this.layers.getConfiguration() != null)
					? getCustomLayers(this.layers.getConfiguration()) : IMPLICIT_LAYERS);
			packager.setIncludeRelevantJarModeJars(this.layers.isIncludeLayerTools());
		}
		return packager;
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
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(inputSource);
	}

	/**
	 * Return {@link Libraries} that the packager can use.
	 * @param unpacks any libraries that require unpack
	 * @return the libraries to use
	 * @throws MojoExecutionException on execution error
	 */
	protected final Libraries getLibraries(Collection<Dependency> unpacks) throws MojoExecutionException {
		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), getFilters(getAdditionalFilters()));
		return new ArtifactsLibraries(artifacts, unpacks, getLog());
	}

	private ArtifactsFilter[] getAdditionalFilters() {
		List<ArtifactsFilter> filters = new ArrayList<>();
		if (this.excludeDevtools) {
			Exclude exclude = new Exclude();
			exclude.setGroupId("org.springframework.boot");
			exclude.setArtifactId("spring-boot-devtools");
			ExcludeFilter filter = new ExcludeFilter(exclude);
			filters.add(filter);
		}
		if (!this.includeSystemScope) {
			filters.add(new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
		}
		return filters.toArray(new ArtifactsFilter[0]);
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
		 * Dir Layout.
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
