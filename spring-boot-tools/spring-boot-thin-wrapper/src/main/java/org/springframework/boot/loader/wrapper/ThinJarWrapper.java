/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Very thin main class that downloads another library to be the real launcher.
 *
 * @author Dave Syer
 *
 */
public class ThinJarWrapper {

	/**
	 * System property key for the main library where the launcher class is located.
	 */
	public static final String MAIN_LIBRARY = "main.library";

	/**
	 * System property key used to store the local file system location of the main
	 * archive (the one that this class is found in).
	 */
	public static final String MAIN_ARCHIVE = "main.archive";

	/**
	 * System property key for remote location of main archive (the one that this class is
	 * found in).
	 */
	public static final String MAIN_REPO = "main.repo";

	/**
	 * System property key used to override the launcher main class if necessary. Defaults
	 * to <code>ThinJarLauncher</code>.
	 */
	public static final String MAIN_LAUNCHER = "main.launcher";

	private static final String DEFAULT_LAUNCHER_CLASS = "org.springframework.boot.loader.thin.ThinJarLauncher";

	private static final String DEFAULT_LIBRARY = "org.springframework.boot:spring-boot-aether:exec:1.5.0.BUILD-SNAPSHOT";

	private Library library;

	public static void main(String[] args) throws Exception {
		Class<?> launcher = ThinJarWrapper.class;
		System.setProperty(MAIN_ARCHIVE, launcher.getProtectionDomain().getCodeSource()
				.getLocation().toURI().toString());
		new ThinJarWrapper().launch(args);
	}

	public ThinJarWrapper() {
		this.library = library();
	}

	private Library library() {
		String coordinates = System.getProperty(MAIN_LIBRARY);
		return new Library(coordinates == null ? DEFAULT_LIBRARY : coordinates);
	}

	private void launch(String... args) throws Exception {
		ClassLoader classLoader = getClassLoader();
		Class<?> launcher = classLoader.loadClass(launcherClass());
		findMainMethod(launcher).invoke(null, new Object[] { args });
	}

	private String launcherClass() {
		String launcher = System.getProperty(MAIN_LAUNCHER);
		return launcher == null ? DEFAULT_LAUNCHER_CLASS : launcher;
	}

	private Method findMainMethod(Class<?> launcher) throws NoSuchMethodException {
		return launcher.getMethod("main", String[].class);
	}

	private ClassLoader getClassLoader() throws Exception {
		URL[] urls = getUrls();
		URLClassLoader classLoader = new URLClassLoader(urls,
				ThinJarWrapper.class.getClassLoader().getParent());
		Thread.currentThread().setContextClassLoader(classLoader);
		return classLoader;
	}

	private URL[] getUrls() throws Exception {
		this.library.download(mavenLocal());
		return new URL[] {
				new File(mavenLocal() + this.library.getPath()).toURI().toURL() };
	}

	private String mavenLocal() {
		return home() + "/.m2/repository";
	}

	private String home() {
		String home = System.getProperty("user.home");
		return home == null ? "." : home;
	}

	/**
	 * Convenience class to hold the co-ordinates of the library to be downloaded.
	 *
	 */
	static class Library {

		private String coordinates;
		private String groupId;
		private String artifactId;
		private String version;
		private String classifier;

		Library(String coordinates) {
			this.coordinates = coordinates;
			String[] parts = coordinates.split(":");
			if (parts.length < 3) {
				throw new IllegalArgumentException(
						"Co-ordinates should contain group:artifact[:classifier]:version");
			}
			if (parts.length > 3) {
				this.classifier = parts[2];
				this.version = parts[3];
			}
			else {
				this.version = parts[2];
			}
			this.groupId = parts[0];
			this.artifactId = parts[1];
		}

		public void download(String path) {
			File target = new File(path + getPath());
			if (!target.exists()) {
				String repo = repo();
				InputStream input = null;
				OutputStream output = null;
				try {
					input = new URL(repo + getPath()).openStream();
					if (target.getParentFile().mkdirs()) {
						output = new FileOutputStream(target);
						byte[] bytes = new byte[4096];
						int count = input.read(bytes);
						while (count > 0) {
							output.write(bytes, 0, count);
							count = input.read(bytes);
						}
					}
				}
				catch (Exception e) {
					throw new IllegalStateException(
							"Cannot download library for launcher " + coordinates, e);
				}
				finally {
					if (input != null) {
						try {
							input.close();
						}
						catch (Exception e) {
						}
					}
					if (output != null) {
						try {
							output.close();
						}
						catch (Exception e) {
						}
					}
				}
			}
		}

		private static String repo() {
			String repo = System.getProperty(MAIN_REPO);
			return repo != null ? repo : "https://repo.spring.io/libs-snapshot";
		}

		public String getCoordinates() {
			return this.coordinates;
		}

		public String getGroupId() {
			return this.groupId;
		}

		public String getArtifactId() {
			return this.artifactId;
		}

		public String getVersion() {
			return this.version;
		}

		public String getClassifier() {
			return this.classifier;
		}

		public String getPath() {
			return "/" + this.groupId.replace(".", "/") + "/" + this.artifactId + "/"
					+ this.version + "/" + this.artifactId + "-" + this.version
					+ (this.classifier != null ? "-" + this.classifier : "") + ".jar";
		}

	}

}
