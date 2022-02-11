package org.springframework.boot.autoconfigure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.springframework.core.io.UrlResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class AutoConfigurationLocator {

	private static final String LOCATION = "META-INF/springboot/";

	private static final String COMMENT_START = "#";

	public List<String> locate(Class<?> annotation, @Nullable ClassLoader classLoader) {
		Assert.notNull(annotation, "'annotation' must not be null");

		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = AutoConfigurationLocator.class.getClassLoader();
		}

		String annotationName = annotation.getName();
		String location = LOCATION + annotationName;

		Enumeration<URL> urls = findUrlsInClasspath(classLoaderToUse, location);

		List<String> autoConfigurations = new ArrayList<>();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();

			autoConfigurations.addAll(readAutoConfigurations(url));
		}

		return autoConfigurations;
	}

	private Enumeration<URL> findUrlsInClasspath(ClassLoader classLoader, String location) {
		try {
			return classLoader.getResources(location);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to load autoconfigurations from location [" + location + "]",
					ex);
		}
	}

	private List<String> readAutoConfigurations(URL url) {
		List<String> autoConfigurations = new ArrayList<>();

		// Read file line by line, ignore comments and empty lines
		// A comment can be put after a valid class name
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new UrlResource(url).getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = stripComment(line);
				line = line.trim();

				if (line.isEmpty()) {
					continue;
				}

				autoConfigurations.add(line);
			}

			return autoConfigurations;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load autoconfigurations from location [" + url + "]", ex);
		}
	}

	private String stripComment(String line) {
		int hash = line.indexOf(COMMENT_START);
		if (hash == -1) {
			return line;
		}

		// This will return an empty string if the line starts with a comment symbol
		return line.substring(0, hash);
	}

}
