/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.testsupport.classpath;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * Custom {@link URLClassLoader} that modifies the class path.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 */
final class ModifiedClassPathClassLoader extends URLClassLoader {

	private static final Map<Class<?>, ModifiedClassPathClassLoader> cache = new ConcurrentReferenceHashMap<>();

	private static final Pattern INTELLIJ_CLASSPATH_JAR_PATTERN = Pattern.compile(".*classpath(\\d+)?\\.jar");

	private final ClassLoader junitLoader;

	ModifiedClassPathClassLoader(URL[] urls, ClassLoader parent, ClassLoader junitLoader) {
		super(urls, parent);
		this.junitLoader = junitLoader;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (name.startsWith("org.junit") || name.startsWith("org.hamcrest")) {
			return this.junitLoader.loadClass(name);
		}
		return super.loadClass(name);
	}

	static ModifiedClassPathClassLoader get(Class<?> testClass) {
		return cache.computeIfAbsent(testClass, ModifiedClassPathClassLoader::compute);
	}

	private static ModifiedClassPathClassLoader compute(Class<?> testClass) {
		ClassLoader classLoader = testClass.getClassLoader();
		return new ModifiedClassPathClassLoader(processUrls(extractUrls(classLoader), testClass),
				classLoader.getParent(), classLoader);
	}

	private static URL[] extractUrls(ClassLoader classLoader) {
		List<URL> extractedUrls = new ArrayList<>();
		doExtractUrls(classLoader).forEach((URL url) -> {
			if (isManifestOnlyJar(url)) {
				extractedUrls.addAll(extractUrlsFromManifestClassPath(url));
			}
			else {
				extractedUrls.add(url);
			}
		});
		return extractedUrls.toArray(new URL[0]);
	}

	private static Stream<URL> doExtractUrls(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader) {
			return Stream.of(((URLClassLoader) classLoader).getURLs());
		}
		return Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
				.map(ModifiedClassPathClassLoader::toURL);
	}

	private static URL toURL(String entry) {
		try {
			return new File(entry).toURI().toURL();
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private static boolean isManifestOnlyJar(URL url) {
		return isSurefireBooterJar(url) || isShortenedIntelliJJar(url);
	}

	private static boolean isSurefireBooterJar(URL url) {
		return url.getPath().contains("surefirebooter");
	}

	private static boolean isShortenedIntelliJJar(URL url) {
		String urlPath = url.getPath();
		boolean isCandidate = INTELLIJ_CLASSPATH_JAR_PATTERN.matcher(urlPath).matches();
		if (isCandidate) {
			try {
				Attributes attributes = getManifestMainAttributesFromUrl(url);
				String createdBy = attributes.getValue("Created-By");
				return createdBy != null && createdBy.contains("IntelliJ");
			}
			catch (Exception ex) {
			}
		}
		return false;
	}

	private static List<URL> extractUrlsFromManifestClassPath(URL booterJar) {
		List<URL> urls = new ArrayList<>();
		try {
			for (String entry : getClassPath(booterJar)) {
				urls.add(new URL(entry));
			}
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return urls;
	}

	private static String[] getClassPath(URL booterJar) throws Exception {
		Attributes attributes = getManifestMainAttributesFromUrl(booterJar);
		return StringUtils.delimitedListToStringArray(attributes.getValue(Attributes.Name.CLASS_PATH), " ");
	}

	private static Attributes getManifestMainAttributesFromUrl(URL url) throws Exception {
		try (JarFile jarFile = new JarFile(new File(url.toURI()))) {
			return jarFile.getManifest().getMainAttributes();
		}
	}

	private static URL[] processUrls(URL[] urls, Class<?> testClass) {
		MergedAnnotations annotations = MergedAnnotations.from(testClass,
				MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
		ClassPathEntryFilter filter = new ClassPathEntryFilter(annotations.get(ClassPathExclusions.class));
		List<URL> processedUrls = new ArrayList<>();
		List<URL> additionalUrls = getAdditionalUrls(annotations.get(ClassPathOverrides.class));
		processedUrls.addAll(additionalUrls);
		for (URL url : urls) {
			if (!filter.isExcluded(url)) {
				processedUrls.add(url);
			}
		}
		return processedUrls.toArray(new URL[0]);
	}

	private static List<URL> getAdditionalUrls(MergedAnnotation<ClassPathOverrides> annotation) {
		if (!annotation.isPresent()) {
			return Collections.emptyList();
		}
		return resolveCoordinates(annotation.getStringArray(MergedAnnotation.VALUE));
	}

	private static List<URL> resolveCoordinates(String[] coordinates) {
		DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
		serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		RepositorySystem repositorySystem = serviceLocator.getService(RepositorySystem.class);
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		LocalRepository localRepository = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
		session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
		CollectRequest collectRequest = new CollectRequest(null, Arrays.asList(
				new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()));

		collectRequest.setDependencies(createDependencies(coordinates));
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
		try {
			DependencyResult result = repositorySystem.resolveDependencies(session, dependencyRequest);
			List<URL> resolvedArtifacts = new ArrayList<>();
			for (ArtifactResult artifact : result.getArtifactResults()) {
				resolvedArtifacts.add(artifact.getArtifact().getFile().toURI().toURL());
			}
			return resolvedArtifacts;
		}
		catch (Exception ignored) {
			return Collections.emptyList();
		}
	}

	private static List<Dependency> createDependencies(String[] allCoordinates) {
		List<Dependency> dependencies = new ArrayList<>();
		for (String coordinate : allCoordinates) {
			dependencies.add(new Dependency(new DefaultArtifact(coordinate), null));
		}
		return dependencies;
	}

	/**
	 * Filter for class path entries.
	 */
	private static final class ClassPathEntryFilter {

		private final List<String> exclusions;

		private final AntPathMatcher matcher = new AntPathMatcher();

		private ClassPathEntryFilter(MergedAnnotation<ClassPathExclusions> annotation) {
			this.exclusions = new ArrayList<>();
			this.exclusions.add("log4j-*.jar");
			if (annotation.isPresent()) {
				this.exclusions.addAll(Arrays.asList(annotation.getStringArray(MergedAnnotation.VALUE)));
			}
		}

		private boolean isExcluded(URL url) {
			if ("file".equals(url.getProtocol())) {
				try {
					String name = new File(url.toURI()).getName();
					for (String exclusion : this.exclusions) {
						if (this.matcher.match(exclusion, name)) {
							return true;
						}
					}
				}
				catch (URISyntaxException ex) {
				}
			}
			return false;
		}

	}

}
