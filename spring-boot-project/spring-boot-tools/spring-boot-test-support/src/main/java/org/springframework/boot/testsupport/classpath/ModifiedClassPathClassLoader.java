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

package org.springframework.boot.testsupport.classpath;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Custom {@link URLClassLoader} that modifies the class path.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 */
final class ModifiedClassPathClassLoader extends URLClassLoader {

	private static final Map<List<AnnotatedElement>, ModifiedClassPathClassLoader> cache = new ConcurrentReferenceHashMap<>();

	private static final Pattern INTELLIJ_CLASSPATH_JAR_PATTERN = Pattern.compile(".*classpath(\\d+)?\\.jar");

	private static final int MAX_RESOLUTION_ATTEMPTS = 5;

	private final Set<String> excludedPackages;

	private final ClassLoader junitLoader;

	/**
	 * Constructs a new ModifiedClassPathClassLoader with the specified URLs, excluded
	 * packages, parent class loader, and JUnit class loader.
	 * @param urls the URLs from which to load classes and resources
	 * @param excludedPackages the set of packages to be excluded from loading
	 * @param parent the parent class loader for delegation
	 * @param junitLoader the class loader for JUnit classes
	 */
	ModifiedClassPathClassLoader(URL[] urls, Set<String> excludedPackages, ClassLoader parent,
			ClassLoader junitLoader) {
		super(urls, parent);
		this.excludedPackages = excludedPackages;
		this.junitLoader = junitLoader;
	}

	/**
	 * Loads the class with the specified name.
	 * @param name the name of the class to be loaded
	 * @return the loaded class
	 * @throws ClassNotFoundException if the class cannot be found
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (name.startsWith("org.junit") || name.startsWith("org.hamcrest")
				|| name.startsWith("io.netty.internal.tcnative")) {
			return Class.forName(name, false, this.junitLoader);
		}
		String packageName = ClassUtils.getPackageName(name);
		if (this.excludedPackages.contains(packageName)) {
			throw new ClassNotFoundException();
		}
		return super.loadClass(name);
	}

	/**
	 * Retrieves a ModifiedClassPathClassLoader based on the provided test class, test
	 * method, and arguments.
	 * @param testClass the test class to consider
	 * @param testMethod the test method to consider
	 * @param arguments the list of arguments to consider
	 * @return a ModifiedClassPathClassLoader if annotated elements are found, otherwise
	 * null
	 */
	static ModifiedClassPathClassLoader get(Class<?> testClass, Method testMethod, List<Object> arguments) {
		Set<AnnotatedElement> candidates = new LinkedHashSet<>();
		candidates.add(testClass);
		candidates.add(testMethod);
		candidates.addAll(getAnnotatedElements(arguments.toArray()));
		List<AnnotatedElement> annotatedElements = candidates.stream()
			.filter(ModifiedClassPathClassLoader::hasAnnotation)
			.toList();
		if (annotatedElements.isEmpty()) {
			return null;
		}
		return cache.computeIfAbsent(annotatedElements, (key) -> compute(testClass.getClassLoader(), key));
	}

	/**
	 * Retrieves all annotated elements from the given array.
	 * @param array the array containing the elements to retrieve annotated elements from
	 * @return a collection of annotated elements found in the array
	 */
	private static Collection<AnnotatedElement> getAnnotatedElements(Object[] array) {
		Set<AnnotatedElement> result = new LinkedHashSet<>();
		for (Object item : array) {
			if (item instanceof AnnotatedElement) {
				result.add((AnnotatedElement) item);
			}
			else if (ObjectUtils.isArray(item)) {
				result.addAll(getAnnotatedElements(ObjectUtils.toObjectArray(item)));
			}
		}
		return result;
	}

	/**
	 * Checks if the given {@link AnnotatedElement} has any of the following annotations:
	 * - {@link ForkedClassPath} - {@link ClassPathOverrides} -
	 * {@link ClassPathExclusions}
	 * @param element the {@link AnnotatedElement} to check for annotations
	 * @return {@code true} if any of the annotations are present, {@code false} otherwise
	 */
	private static boolean hasAnnotation(AnnotatedElement element) {
		MergedAnnotations annotations = MergedAnnotations.from(element,
				MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
		return annotations.isPresent(ForkedClassPath.class) || annotations.isPresent(ClassPathOverrides.class)
				|| annotations.isPresent(ClassPathExclusions.class);
	}

	/**
	 * Computes a ModifiedClassPathClassLoader based on the given class loader and list of
	 * annotated classes.
	 * @param classLoader the original class loader
	 * @param annotatedClasses the list of annotated classes
	 * @return a ModifiedClassPathClassLoader instance
	 */
	private static ModifiedClassPathClassLoader compute(ClassLoader classLoader,
			List<AnnotatedElement> annotatedClasses) {
		List<MergedAnnotations> annotations = annotatedClasses.stream()
			.map((source) -> MergedAnnotations.from(source, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY))
			.toList();
		return new ModifiedClassPathClassLoader(processUrls(extractUrls(classLoader), annotations),
				excludedPackages(annotations), classLoader.getParent(), classLoader);
	}

	/**
	 * Extracts the URLs from the given class loader.
	 * @param classLoader the class loader from which to extract the URLs
	 * @return an array of URLs extracted from the class loader
	 */
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

	/**
	 * Extracts the URLs from the given ClassLoader.
	 * @param classLoader the ClassLoader from which to extract the URLs
	 * @return a Stream of URLs extracted from the ClassLoader
	 */
	private static Stream<URL> doExtractUrls(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			return Stream.of(urlClassLoader.getURLs());
		}
		return Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
			.map(ModifiedClassPathClassLoader::toURL);
	}

	/**
	 * Converts the given entry string to a URL.
	 * @param entry the entry string to convert
	 * @return the URL representation of the entry string
	 * @throws IllegalArgumentException if an error occurs during the conversion
	 */
	private static URL toURL(String entry) {
		try {
			return new File(entry).toURI().toURL();
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Checks if the given URL points to a manifest-only JAR file.
	 * @param url the URL to check
	 * @return true if the URL points to a manifest-only JAR file, false otherwise
	 */
	private static boolean isManifestOnlyJar(URL url) {
		return isShortenedIntelliJJar(url);
	}

	/**
	 * Checks if the given URL is a shortened IntelliJ JAR.
	 * @param url the URL to check
	 * @return true if the URL is a shortened IntelliJ JAR, false otherwise
	 */
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
				// Ignore
			}
		}
		return false;
	}

	/**
	 * Extracts URLs from the manifest classpath of the specified booter JAR.
	 * @param booterJar the URL of the booter JAR
	 * @return a list of URLs extracted from the manifest classpath
	 * @throws RuntimeException if an exception occurs during the extraction process
	 */
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

	/**
	 * Retrieves the class path from the specified booter JAR file.
	 * @param booterJar the URL of the booter JAR file
	 * @return an array of strings representing the class path entries
	 * @throws Exception if an error occurs while retrieving the class path
	 */
	private static String[] getClassPath(URL booterJar) throws Exception {
		Attributes attributes = getManifestMainAttributesFromUrl(booterJar);
		return StringUtils.delimitedListToStringArray(attributes.getValue(Attributes.Name.CLASS_PATH), " ");
	}

	/**
	 * Retrieves the main attributes from the manifest file of a JAR file located at the
	 * specified URL.
	 * @param url the URL of the JAR file
	 * @return the main attributes from the manifest file
	 * @throws Exception if an error occurs while retrieving the main attributes
	 */
	private static Attributes getManifestMainAttributesFromUrl(URL url) throws Exception {
		try (JarFile jarFile = new JarFile(new File(url.toURI()))) {
			return jarFile.getManifest().getMainAttributes();
		}
	}

	/**
	 * Processes the given array of URLs and filters them based on the provided list of
	 * merged annotations.
	 * @param urls the array of URLs to be processed
	 * @param annotations the list of merged annotations used for filtering
	 * @return an array of processed URLs after filtering
	 */
	private static URL[] processUrls(URL[] urls, List<MergedAnnotations> annotations) {
		ClassPathEntryFilter filter = new ClassPathEntryFilter(annotations);
		List<URL> additionalUrls = getAdditionalUrls(annotations);
		List<URL> processedUrls = new ArrayList<>(additionalUrls);
		for (URL url : urls) {
			if (!filter.isExcluded(url)) {
				processedUrls.add(url);
			}
		}
		return processedUrls.toArray(new URL[0]);
	}

	/**
	 * Retrieves additional URLs from a list of merged annotations.
	 * @param annotations the list of merged annotations
	 * @return a list of additional URLs
	 */
	private static List<URL> getAdditionalUrls(List<MergedAnnotations> annotations) {
		Set<URL> urls = new LinkedHashSet<>();
		for (MergedAnnotations candidate : annotations) {
			MergedAnnotation<ClassPathOverrides> annotation = candidate.get(ClassPathOverrides.class);
			if (annotation.isPresent()) {
				urls.addAll(resolveCoordinates(annotation.getStringArray(MergedAnnotation.VALUE)));
			}
		}
		return urls.stream().toList();
	}

	/**
	 * Resolves the coordinates of the artifacts and returns a list of URLs.
	 * @param coordinates an array of coordinates representing the artifacts to be
	 * resolved
	 * @return a list of URLs representing the resolved artifacts
	 * @throws IllegalStateException if resolution fails after the maximum number of
	 * attempts
	 */
	private static List<URL> resolveCoordinates(String[] coordinates) {
		Exception latestFailure = null;
		RepositorySystem repositorySystem = createRepositorySystem();
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setSystemProperties(System.getProperties());
		LocalRepository localRepository = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
		RemoteRepository remoteRepository = new RemoteRepository.Builder("central", "default",
				"https://repo.maven.apache.org/maven2")
			.build();
		session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
		for (int i = 0; i < MAX_RESOLUTION_ATTEMPTS; i++) {
			CollectRequest collectRequest = new CollectRequest(null, Arrays.asList(remoteRepository));
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
			catch (Exception ex) {
				latestFailure = ex;
			}
		}
		throw new IllegalStateException("Resolution failed after " + MAX_RESOLUTION_ATTEMPTS + " attempts",
				latestFailure);
	}

	/**
	 * Creates a repository system.
	 * @return The created repository system.
	 */
	@SuppressWarnings("deprecation")
	private static RepositorySystem createRepositorySystem() {
		org.eclipse.aether.impl.DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
		serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		RepositorySystem repositorySystem = serviceLocator.getService(RepositorySystem.class);
		return repositorySystem;
	}

	/**
	 * Creates a list of dependencies based on the given array of coordinates.
	 * @param allCoordinates the array of coordinates representing the dependencies
	 * @return the list of dependencies created
	 */
	private static List<Dependency> createDependencies(String[] allCoordinates) {
		List<Dependency> dependencies = new ArrayList<>();
		for (String coordinate : allCoordinates) {
			dependencies.add(new Dependency(new DefaultArtifact(coordinate), null));
		}
		return dependencies;
	}

	/**
	 * Returns a set of excluded packages based on the provided list of merged
	 * annotations.
	 * @param annotations the list of merged annotations to process
	 * @return a set of excluded packages
	 */
	private static Set<String> excludedPackages(List<MergedAnnotations> annotations) {
		Set<String> excludedPackages = new HashSet<>();
		for (MergedAnnotations candidate : annotations) {
			MergedAnnotation<ClassPathExclusions> annotation = candidate.get(ClassPathExclusions.class);
			if (annotation.isPresent()) {
				excludedPackages.addAll(Arrays.asList(annotation.getStringArray("packages")));
			}
		}
		return excludedPackages;
	}

	/**
	 * Filter for class path entries.
	 */
	private static final class ClassPathEntryFilter {

		private final List<String> exclusions;

		private final AntPathMatcher matcher = new AntPathMatcher();

		/**
		 * Constructs a new ClassPathEntryFilter with the given list of merged
		 * annotations.
		 * @param annotations the list of merged annotations to be used for filtering
		 * class path entries
		 */
		private ClassPathEntryFilter(List<MergedAnnotations> annotations) {
			Set<String> exclusions = new LinkedHashSet<>();
			for (MergedAnnotations candidate : annotations) {
				MergedAnnotation<ClassPathExclusions> annotation = candidate.get(ClassPathExclusions.class);
				if (annotation.isPresent()) {
					exclusions.addAll(Arrays.asList(annotation.getStringArray(MergedAnnotation.VALUE)));
				}
			}
			this.exclusions = exclusions.stream().toList();
		}

		/**
		 * Checks if the given URL is excluded based on the specified exclusions.
		 * @param url the URL to check
		 * @return true if the URL is excluded, false otherwise
		 */
		private boolean isExcluded(URL url) {
			if ("file".equals(url.getProtocol())) {
				try {
					URI uri = url.toURI();
					File file = new File(uri);
					String name = (!uri.toString().endsWith("/")) ? file.getName()
							: file.getParentFile().getParentFile().getName();
					for (String exclusion : this.exclusions) {
						if (this.matcher.match(exclusion, name)) {
							return true;
						}
					}
				}
				catch (URISyntaxException ex) {
					// Ignore
				}
			}
			return false;
		}

	}

}
