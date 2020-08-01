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
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Base class for archive (jar or war) related Maven plugin integration tests.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractArchiveIntegrationTests {

	protected String buildLog(File project) {
		return contentOf(new File(project, "target/build.log"));
	}

	protected String launchScript(File jar) {
		String content = contentOf(jar);
		return content.substring(0, content.indexOf(new String(new byte[] { 0x50, 0x4b, 0x03, 0x04 })));
	}

	protected AssertProvider<JarAssert> jar(File file) {
		return new AssertProvider<JarAssert>() {

			@Override
			@Deprecated
			public JarAssert assertThat() {
				return new JarAssert(file);
			}

		};
	}

	static final class JarAssert extends AbstractAssert<JarAssert, File> {

		private JarAssert(File actual) {
			super(actual, JarAssert.class);
			assertThat(actual.exists());
		}

		JarAssert doesNotHaveEntryWithName(String name) {
			withJarFile((jarFile) -> {
				withEntries(jarFile, (entries) -> {
					Optional<JarEntry> match = entries.filter((entry) -> entry.getName().equals(name)).findFirst();
					assertThat(match).isNotPresent();
				});
			});
			return this;
		}

		JarAssert hasEntryWithName(String name) {
			withJarFile((jarFile) -> {
				withEntries(jarFile, (entries) -> {
					Optional<JarEntry> match = entries.filter((entry) -> entry.getName().equals(name)).findFirst();
					assertThat(match).hasValueSatisfying((entry) -> assertThat(entry.getComment()).isNull());
				});
			});
			return this;
		}

		JarAssert hasEntryWithNameStartingWith(String prefix) {
			withJarFile((jarFile) -> {
				withEntries(jarFile, (entries) -> {
					Optional<JarEntry> match = entries.filter((entry) -> entry.getName().startsWith(prefix))
							.findFirst();
					assertThat(match).hasValueSatisfying((entry) -> assertThat(entry.getComment()).isNull());
				});
			});
			return this;
		}

		JarAssert hasUnpackEntryWithNameStartingWith(String prefix) {
			withJarFile((jarFile) -> {
				withEntries(jarFile, (entries) -> {
					Optional<JarEntry> match = entries.filter((entry) -> entry.getName().startsWith(prefix))
							.findFirst();
					assertThat(match).as("Name starting with %s", prefix)
							.hasValueSatisfying((entry) -> assertThat(entry.getComment()).startsWith("UNPACK:"));
				});
			});
			return this;
		}

		JarAssert doesNotHaveEntryWithNameStartingWith(String prefix) {
			withJarFile((jarFile) -> {
				withEntries(jarFile, (entries) -> {
					Optional<JarEntry> match = entries.filter((entry) -> entry.getName().startsWith(prefix))
							.findFirst();
					assertThat(match).isNotPresent();
				});
			});
			return this;
		}

		JarAssert manifest(Consumer<ManifestAssert> consumer) {
			withJarFile((jarFile) -> {
				try {
					consumer.accept(new ManifestAssert(jarFile.getManifest()));
				}
				catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			});
			return this;
		}

		void withJarFile(Consumer<JarFile> consumer) {
			try (JarFile jarFile = new JarFile(this.actual)) {
				consumer.accept(jarFile);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		void withEntries(JarFile jarFile, Consumer<Stream<JarEntry>> entries) {
			entries.accept(Collections.list(jarFile.entries()).stream());
		}

		static final class ManifestAssert extends AbstractAssert<ManifestAssert, Manifest> {

			private ManifestAssert(Manifest actual) {
				super(actual, ManifestAssert.class);
			}

			ManifestAssert hasStartClass(String expected) {
				assertThat(this.actual.getMainAttributes().getValue("Start-Class")).isEqualTo(expected);
				return this;
			}

			ManifestAssert hasMainClass(String expected) {
				assertThat(this.actual.getMainAttributes().getValue("Main-Class")).isEqualTo(expected);
				return this;
			}

			ManifestAssert hasAttribute(String name, String value) {
				assertThat(this.actual.getMainAttributes().getValue(name)).isEqualTo(value);
				return this;
			}

		}

	}

}
