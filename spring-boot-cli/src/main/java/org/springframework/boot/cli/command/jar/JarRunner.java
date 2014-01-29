/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command.jar;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.loader.ArchiveResolver;
import org.springframework.boot.loader.AsciiBytes;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;

/**
 * A runner for a CLI application that has been compiled and packaged as a jar file
 * 
 * @author Andy Wilkinson
 */
public class JarRunner {

	private static final AsciiBytes LIB = new AsciiBytes("lib/");

	public static void main(String[] args) throws URISyntaxException, IOException,
			ClassNotFoundException, SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Archive archive = new ArchiveResolver().resolveArchive(JarRunner.class);

		ClassLoader classLoader = createClassLoader(archive);
		Class<?>[] classes = loadApplicationClasses(archive, classLoader);

		Thread.currentThread().setContextClassLoader(classLoader);

		// Use reflection to load and call Spring
		Class<?> application = classLoader
				.loadClass("org.springframework.boot.SpringApplication");
		Method method = application.getMethod("run", Object[].class, String[].class);
		method.invoke(null, classes, args);

	}

	private static ClassLoader createClassLoader(Archive archive) throws IOException,
			MalformedURLException {
		List<Archive> nestedArchives = archive.getNestedArchives(new EntryFilter() {

			@Override
			public boolean matches(Entry entry) {
				return entry.getName().startsWith(LIB);
			}

		});

		List<URL> urls = new ArrayList<URL>();
		urls.add(archive.getUrl());
		for (Archive nestedArchive : nestedArchives) {
			urls.add(nestedArchive.getUrl());
		}

		ClassLoader classLoader = new LaunchedURLClassLoader(urls.toArray(new URL[urls
				.size()]), JarRunner.class.getClassLoader());
		return classLoader;
	}

	private static Class<?>[] loadApplicationClasses(Archive archive,
			ClassLoader classLoader) throws ClassNotFoundException, IOException {
		String[] classNames = archive.getManifest().getMainAttributes()
				.getValue("Application-Classes").split(",");

		Class<?>[] classes = new Class<?>[classNames.length];

		for (int i = 0; i < classNames.length; i++) {
			Class<?> applicationClass = classLoader.loadClass(classNames[i]);
			classes[i] = applicationClass;
		}
		return classes;
	}
}
