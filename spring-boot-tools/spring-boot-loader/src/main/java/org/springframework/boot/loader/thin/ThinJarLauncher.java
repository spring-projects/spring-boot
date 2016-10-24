/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader.thin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

import org.springframework.boot.aether.AetherEngine;
import org.springframework.boot.aether.DependencyManagementContext;
import org.springframework.boot.aether.RepositoryConfigurationFactory;
import org.springframework.boot.loader.ExecutableArchiveLauncher;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.util.MainClassFinder;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.ResourcePatternUtils;

/**
 * Launcher that downloads the dependencies for the app before it starts.
 *
 * @author Dave Syer
 */
public class ThinJarLauncher extends ExecutableArchiveLauncher {

	private static final String DEFAULT_BOM = "org.springframework.boot:spring-boot-dependencies";

	private static final String DEFAULT_VERSION = "1.5.0.BUILD-SNAPSHOT";

	private PomLoader pomLoader = new PomLoader();

	public static void main(String[] args) throws Exception {
		new ThinJarLauncher().launch(args);
	}

	public ThinJarLauncher() throws Exception {
		super(computeArchive());
	}

	@Override
	protected void launch(String[] args) throws Exception {
		String root = System.getProperty("main.root");
		String debug = System.getProperty("debug");
		if (root != null) {
			// There is a grape root that is used by the aether engine internally
			System.setProperty("grape.root", root);
		}
		if (System.getProperty("main.dryrun") != null) {
			getClassPathArchives();
			if (debug != null) {
				System.out.println(
						"Downloaded dependencies" + (root == null ? "" : " to " + root));
			}
			return;
		}
		super.launch(args);
	}

	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new LaunchedURLClassLoader(urls, getClass().getClassLoader().getParent());
	}

	@Override
	protected String getMainClass() throws Exception {
		if (System.getProperty("main.class") != null) {
			return System.getProperty("main.class");
		}
		try {
			return super.getMainClass();
		}
		catch (IllegalStateException e) {
			File root = new File(getArchive().getUrl().toURI());
			if (getArchive() instanceof ExplodedArchive) {
				return MainClassFinder.findSingleMainClass(root);
			}
			else {
				return MainClassFinder.findSingleMainClass(new JarFile(root), "/");
			}
		}
	}

	private static Archive computeArchive() throws Exception {
		File file = new File(findArchive());
		if (file.isDirectory()) {
			return new ExplodedArchive(file);
		}
		return new JarFileArchive(file);
	}

	private static URI findArchive() throws Exception {
		String path = System.getProperty("main.archive");
		URI archive = path == null ? null : new URI(path);
		File dir = new File("target/classes");
		if (archive == null && dir.exists()) {
			archive = dir.toURI();
		}
		if (archive == null) {
			dir = new File("build/classes");
			if (dir.exists()) {
				archive = dir.toURI();
			}
		}
		if (archive == null) {
			dir = new File(".");
			archive = dir.toURI();
		}
		return archive;
	}

	@Override
	protected List<Archive> getClassPathArchives() throws Exception {
		Collection<Dependency> dependencies = new LinkedHashSet<Dependency>();
		Collection<Dependency> boms = new LinkedHashSet<Dependency>();
		// TODO: Maybe use something that conserves order?
		Properties libs = loadLibraryProperties();
		for (String key : libs.stringPropertyNames()) {
			String lib = libs.getProperty(key);
			if (key.startsWith("dependencies")) {
				dependencies.add(dependency(lib));
			}
			if (key.startsWith("boms")) {
				boms.add(dependency(lib));
			}
		}

		boms.addAll(getPomDependencyManagement());
		dependencies.addAll(getPomDependencies());

		if (boms.isEmpty()) {
			boms.add(dependency(getDefaultBom()));
		}

		List<Archive> archives = archives(resolve(new ArrayList<Dependency>(boms),
				new ArrayList<Dependency>(dependencies)));
		if (!archives.isEmpty()) {
			archives.set(0, getArchive());
		}
		else {
			archives.add(getArchive());
		}
		return archives;
	}

	private String getDefaultBom() {
		return DEFAULT_BOM + ":" + getVersion();
	}

	private String getVersion() {
		Package pkg = ThinJarLauncher.class.getPackage();
		return (pkg != null ? pkg.getImplementationVersion() : DEFAULT_VERSION);
	}

	private Properties loadLibraryProperties() throws IOException, MalformedURLException {
		UrlResource resource = new UrlResource(
				getArchive().getUrl() + "META-INF/lib.properties");
		Properties props = resource.exists()
				? PropertiesLoaderUtils.loadProperties(resource) : new Properties();
		FileSystemResource local = new FileSystemResource("lib.properties");
		if (local.exists()) {
			PropertiesLoaderUtils.fillProperties(props, local);
		}
		return props;
	}

	private List<Archive> archives(List<File> files) throws IOException {
		List<Archive> archives = new ArrayList<Archive>();
		for (File file : files) {
			archives.add(new JarFileArchive(file, file.toURI().toURL()));
		}
		return archives;
	}

	private Dependency dependency(String coordinates) {
		String[] parts = coordinates.split(":");
		if (parts.length < 2) {
			throw new IllegalArgumentException(
					"Co-ordinates should contain group:artifact[:extension][:classifier][:version]");
		}
		String extension = "jar";
		String classifier;
		String version;
		String artifactId;
		String groupId;
		if (parts.length > 4) {
			extension = parts[2];
			classifier = parts[3];
			version = parts[4];
		}
		else if (parts.length > 3) {
			if (parts[3].contains(".")) {
				version = parts[3];
				classifier = parts[2];
			}
			else {
				extension = parts[2];
				classifier = parts[3];
				version = null;
			}

		}
		else if (parts.length > 2) {
			if (parts[2].contains(".")) {
				version = parts[2];
				classifier = null;
			}
			else {
				classifier = parts[2];
				version = null;
			}
		}
		else {
			classifier = null;
			version = null;
		}
		groupId = parts[0];
		artifactId = parts[1];
		return new Dependency(
				new DefaultArtifact(groupId, artifactId, classifier, extension, version),
				"compile");
	}

	private List<File> resolve(List<Dependency> boms, List<Dependency> dependencies)
			throws Exception {
		AetherEngine engine = AetherEngine.create(
				RepositoryConfigurationFactory.createDefaultRepositoryConfiguration(),
				new DependencyManagementContext());
		engine.addDependencyManagementBoms(boms);
		List<File> files = engine.resolve(dependencies);
		return files;
	}

	private List<Dependency> getPomDependencies() throws Exception {
		return this.pomLoader.getDependencies(getPom());
	}

	private List<Dependency> getPomDependencyManagement() throws Exception {
		return this.pomLoader.getDependencyManagement(getPom());
	}

	private Resource getPom() throws Exception {
		Resource pom = new UrlResource(getArchive().getUrl() + "pom.xml");
		if (!pom.exists()) {
			for (Resource resource : ResourcePatternUtils
					.getResourcePatternResolver(new DefaultResourceLoader())
					.getResources(getArchive().getUrl() + "META-INF/maven/**/pom.xml")) {
				if (resource.exists()) {
					return resource;
				}
			}
		}
		if (!pom.exists()) {
			pom = new FileSystemResource("./pom.xml");
		}
		return pom;
	}

	@Override
	protected boolean isNestedArchive(Entry entry) {
		return false;
	}

}
