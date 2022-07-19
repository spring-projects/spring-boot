/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.image.paketo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import com.github.dockerjava.api.model.ContainerConfig;
import org.assertj.core.api.Condition;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.image.assertions.ImageAssertions;
import org.springframework.boot.image.junit.GradleBuildInjectionExtension;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuildExtension;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Integration tests for the Paketo builder and buildpacks.
 *
 * See
 * https://paketo.io/docs/buildpacks/language-family-buildpacks/java/#additional-metadata
 *
 * @author Scott Frederick
 */
@ExtendWith({ GradleBuildInjectionExtension.class, GradleBuildExtension.class })
@EnabledForJreRange(min = JRE.JAVA_8, max = JRE.JAVA_17)
class PaketoBuilderTests {

	GradleBuild gradleBuild;

	@BeforeEach
	void configureGradleBuild() {
		this.gradleBuild.scriptProperty("systemTestMavenRepository",
				new File("build/system-test-maven-repository").getAbsoluteFile().toURI().toASCIIString());
		this.gradleBuild.expectDeprecationMessages("BPL_SPRING_CLOUD_BINDINGS_ENABLED.*true.*Deprecated");
	}

	@Test
	void executableJarApp() throws Exception {
		writeMainClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withExposedPorts(8080);
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			assertLabelsMatchManifestAttributes(config);
			ImageAssertions.assertThat(config).buildMetadata((metadata) -> {
				metadata.buildpacks().contains("paketo-buildpacks/ca-certificates",
						"paketo-buildpacks/bellsoft-liberica", "paketo-buildpacks/executable-jar",
						"paketo-buildpacks/dist-zip", "paketo-buildpacks/spring-boot");
				metadata.processOfType("web").extracting("command", "args").containsExactly("java",
						Collections.singletonList("org.springframework.boot.loader.JarLauncher"));
				metadata.processOfType("executable-jar").extracting("command", "args").containsExactly("java",
						Collections.singletonList("org.springframework.boot.loader.JarLauncher"));
			});
			assertImageHasSbomLayer(imageReference, config, "executable-jar");
			assertImageLayersMatchLayersIndex(imageReference, config);
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void executableJarAppWithAdditionalArgs() throws Exception {
		writeMainClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withCommand("--server.port=9090");
			container.withExposedPorts(9090);
			container.waitingFor(Wait.forHttp("/test")).start();
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void executableJarAppBuiltTwiceWithCaching() throws Exception {
		writeMainClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withExposedPorts(8080);
			container.waitingFor(Wait.forHttp("/test")).start();
			container.stop();
		}
		this.gradleBuild.expectDeprecationMessages("BOM table is deprecated");
		result = buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withExposedPorts(8080);
			container.waitingFor(Wait.forHttp("/test")).start();
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void bootDistZipJarApp() throws Exception {
		writeMainClass();
		String projectName = this.gradleBuild.getProjectDir().getName();
		String imageName = "paketo-integration/" + projectName;
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = buildImage(imageName, "assemble", "bootDistZip");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withExposedPorts(8080);
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			ImageAssertions.assertThat(config).buildMetadata((metadata) -> {
				metadata.buildpacks().contains("paketo-buildpacks/ca-certificates",
						"paketo-buildpacks/bellsoft-liberica", "paketo-buildpacks/dist-zip",
						"paketo-buildpacks/spring-boot");
				metadata.processOfType("web").extracting("command", "args").containsExactly(
						"/workspace/" + projectName + "-boot/bin/" + projectName, Collections.emptyList());
				metadata.processOfType("dist-zip").extracting("command", "args").containsExactly(
						"/workspace/" + projectName + "-boot/bin/" + projectName, Collections.emptyList());
			});
			assertImageHasSbomLayer(imageReference, config, "dist-zip");
			DigestCapturingCondition digest = new DigestCapturingCondition();
			ImageAssertions.assertThat(config)
					.lifecycleMetadata((metadata) -> metadata.appLayerShas().haveExactly(1, digest));
			ImageAssertions.assertThat(imageReference).layer(digest.getDigest(),
					(layer) -> layer.entries().contains(projectName + "-boot/bin/" + projectName,
							projectName + "-boot/lib/" + projectName + ".jar"));
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void plainDistZipJarApp() throws Exception {
		writeMainClass();
		String projectName = this.gradleBuild.getProjectDir().getName();
		String imageName = "paketo-integration/" + projectName;
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = buildImage(imageName, "assemble", "bootDistZip");
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withExposedPorts(8080);
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			ImageAssertions.assertThat(config).buildMetadata((metadata) -> {
				metadata.buildpacks().contains("paketo-buildpacks/ca-certificates",
						"paketo-buildpacks/bellsoft-liberica", "paketo-buildpacks/dist-zip",
						"paketo-buildpacks/spring-boot");
				metadata.processOfType("web").extracting("command", "args")
						.containsExactly("/workspace/" + projectName + "/bin/" + projectName, Collections.emptyList());
				metadata.processOfType("dist-zip").extracting("command", "args")
						.containsExactly("/workspace/" + projectName + "/bin/" + projectName, Collections.emptyList());
			});
			assertImageHasSbomLayer(imageReference, config, "dist-zip");
			DigestCapturingCondition digest = new DigestCapturingCondition();
			ImageAssertions.assertThat(config)
					.lifecycleMetadata((metadata) -> metadata.appLayerShas().haveExactly(1, digest));
			ImageAssertions.assertThat(imageReference).layer(digest.getDigest(), (layer) -> layer.entries()
					.contains(projectName + "/bin/" + projectName, projectName + "/lib/" + projectName + "-plain.jar")
					.anyMatch((s) -> s.startsWith(projectName + "/lib/spring-boot-"))
					.anyMatch((s) -> s.startsWith(projectName + "/lib/spring-core-"))
					.anyMatch((s) -> s.startsWith(projectName + "/lib/spring-web-")));
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void executableWarApp() throws Exception {
		writeMainClass();
		writeServletInitializerClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withExposedPorts(8080);
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			assertLabelsMatchManifestAttributes(config);
			ImageAssertions.assertThat(config).buildMetadata((metadata) -> {
				metadata.buildpacks().contains("paketo-buildpacks/ca-certificates",
						"paketo-buildpacks/bellsoft-liberica", "paketo-buildpacks/executable-jar",
						"paketo-buildpacks/dist-zip", "paketo-buildpacks/spring-boot");
				metadata.processOfType("web").extracting("command", "args").containsExactly("java",
						Collections.singletonList("org.springframework.boot.loader.WarLauncher"));
				metadata.processOfType("executable-jar").extracting("command", "args").containsExactly("java",
						Collections.singletonList("org.springframework.boot.loader.WarLauncher"));
			});
			assertImageHasSbomLayer(imageReference, config, "executable-jar");
			assertImageLayersMatchLayersIndex(imageReference, config);
		}
		finally {
			removeImage(imageReference);
		}
	}

	@Test
	void plainWarApp() throws Exception {
		writeMainClass();
		writeServletInitializerClass();
		String imageName = "paketo-integration/" + this.gradleBuild.getProjectDir().getName();
		ImageReference imageReference = ImageReference.of(ImageName.of(imageName));
		BuildResult result = buildImage(imageName);
		assertThat(result.task(":bootBuildImage").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (GenericContainer<?> container = new GenericContainer<>(imageName)) {
			container.withExposedPorts(8080);
			container.waitingFor(Wait.forHttp("/test")).start();
			ContainerConfig config = container.getContainerInfo().getConfig();
			ImageAssertions.assertThat(config).buildMetadata((metadata) -> {
				metadata.buildpacks().contains("paketo-buildpacks/ca-certificates",
						"paketo-buildpacks/bellsoft-liberica", "paketo-buildpacks/apache-tomcat",
						"paketo-buildpacks/dist-zip", "paketo-buildpacks/spring-boot");
				metadata.processOfType("web").extracting("command", "args").containsExactly("bash",
						Arrays.asList("catalina.sh", "run"));
				metadata.processOfType("tomcat").extracting("command", "args").containsExactly("bash",
						Arrays.asList("catalina.sh", "run"));
			});
			assertImageHasSbomLayer(imageReference, config, "apache-tomcat");
			DigestCapturingCondition digest = new DigestCapturingCondition();
			ImageAssertions.assertThat(config)
					.lifecycleMetadata((metadata) -> metadata.appLayerShas().haveExactly(1, digest));
			ImageAssertions.assertThat(imageReference).layer(digest.getDigest(),
					(layer) -> layer.entries()
							.contains("WEB-INF/classes/example/ExampleApplication.class",
									"WEB-INF/classes/example/HelloController.class", "META-INF/MANIFEST.MF")
							.anyMatch((s) -> s.startsWith("WEB-INF/lib/spring-boot-"))
							.anyMatch((s) -> s.startsWith("WEB-INF/lib/spring-core-"))
							.anyMatch((s) -> s.startsWith("WEB-INF/lib/spring-web-")));
		}
		finally {
			removeImage(imageReference);
		}
	}

	private BuildResult buildImage(String imageName, String... arguments) {
		String[] buildImageArgs = { "bootBuildImage", "--imageName=" + imageName, "--pullPolicy=IF_NOT_PRESENT" };
		String[] args = StringUtils.concatenateStringArrays(arguments, buildImageArgs);
		return this.gradleBuild.build(args);
	}

	private void writeMainClass() throws IOException {
		writeProjectFile("ExampleApplication.java", (writer) -> {
			writer.println("package example;");
			writer.println();
			writer.println("import org.springframework.boot.SpringApplication;");
			writer.println("import org.springframework.boot.autoconfigure.SpringBootApplication;");
			writer.println("import org.springframework.stereotype.Controller;");
			writer.println("import org.springframework.web.bind.annotation.RequestMapping;");
			writer.println("import org.springframework.web.bind.annotation.ResponseBody;");
			writer.println();
			writer.println("@SpringBootApplication");
			writer.println("public class ExampleApplication {");
			writer.println();
			writer.println("    public static void main(String[] args) {");
			writer.println("        SpringApplication.run(ExampleApplication.class, args);");
			writer.println("    }");
			writer.println();
			writer.println("}");
			writer.println();
			writer.println("@Controller");
			writer.println("class HelloController {");
			writer.println();
			writer.println("    @RequestMapping(\"/test\")");
			writer.println("    @ResponseBody");
			writer.println("    String home() {");
			writer.println("        return \"Hello, world!\";");
			writer.println("    }");
			writer.println();
			writer.println("}");
		});
	}

	private void writeServletInitializerClass() throws IOException {
		writeProjectFile("ServletInitializer.java", (writer) -> {
			writer.println("package example;");
			writer.println();
			writer.println("import org.springframework.boot.builder.SpringApplicationBuilder;");
			writer.println("import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;");
			writer.println();
			writer.println("public class ServletInitializer extends SpringBootServletInitializer {");
			writer.println();
			writer.println("    @Override");
			writer.println("    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {");
			writer.println("        return application.sources(ExampleApplication.class);");
			writer.println("    }");
			writer.println();
			writer.println("}");
		});
	}

	private void writeProjectFile(String fileName, Consumer<PrintWriter> consumer) throws IOException {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/example");
		examplePackage.mkdirs();
		File main = new File(examplePackage, fileName);
		try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
			consumer.accept(writer);
		}
	}

	private void assertLabelsMatchManifestAttributes(ContainerConfig config) throws IOException {
		JarFile jarFile = new JarFile(projectArchiveFile());
		Attributes attributes = jarFile.getManifest().getMainAttributes();
		ImageAssertions.assertThat(config).labels((labels) -> {
			labels.contains(entry("org.springframework.boot.version", attributes.getValue("Spring-Boot-Version")));
			labels.contains(
					entry("org.opencontainers.image.title", attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE)));
			labels.contains(entry("org.opencontainers.image.version",
					attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)));
		});
	}

	private void assertImageHasSbomLayer(ImageReference imageReference, ContainerConfig config, String buildpack)
			throws IOException {
		DigestCapturingCondition digest = new DigestCapturingCondition();
		ImageAssertions.assertThat(config).lifecycleMetadata((metadata) -> metadata.sbomLayerSha().has(digest));
		ImageAssertions.assertThat(imageReference).layer(digest.getDigest(), (layer) -> {
			layer.entries().contains("/layers/sbom/launch/paketo-buildpacks_bellsoft-liberica/jre/sbom.syft.json",
					"/layers/sbom/launch/paketo-buildpacks_" + buildpack + "/sbom.syft.json",
					"/layers/sbom/launch/paketo-buildpacks_" + buildpack + "/sbom.cdx.json");
			layer.jsonEntry("/layers/sbom/launch/paketo-buildpacks_bellsoft-liberica/jre/sbom.syft.json", (json) -> {
				json.extractingJsonPathStringValue("$.Artifacts[0].Name").isEqualTo("BellSoft Liberica JRE");
				json.extractingJsonPathStringValue("$.Artifacts[0].Version").startsWith(javaMajorVersion());
			});
			layer.jsonEntry("/layers/sbom/launch/paketo-buildpacks_" + buildpack + "/sbom.syft.json",
					(json) -> json.extractingJsonPathArrayValue("$.artifacts.[*].name").contains("spring-beans",
							"spring-boot", "spring-boot-autoconfigure", "spring-context", "spring-core",
							"spring-expression", "spring-jcl", "spring-web", "spring-webmvc"));
			layer.jsonEntry("/layers/sbom/launch/paketo-buildpacks_" + buildpack + "/sbom.cdx.json",
					(json) -> json.extractingJsonPathArrayValue("$.components.[*].name").contains("spring-beans",
							"spring-boot", "spring-boot-autoconfigure", "spring-context", "spring-core",
							"spring-expression", "spring-jcl", "spring-web", "spring-webmvc"));
		});
	}

	private void assertImageLayersMatchLayersIndex(ImageReference imageReference, ContainerConfig config)
			throws IOException {
		DigestsCapturingCondition digests = new DigestsCapturingCondition();
		ImageAssertions.assertThat(config)
				.lifecycleMetadata((metadata) -> metadata.appLayerShas().haveExactly(5, digests));
		LayersIndex layersIndex = LayersIndex.fromArchiveFile(projectArchiveFile());
		ImageAssertions.assertThat(imageReference).layer(digests.getDigest(0), (layer) -> layer.entries()
				.allMatch((entry) -> startsWithOneOf(entry, layersIndex.getLayer("dependencies"))));
		ImageAssertions.assertThat(imageReference).layer(digests.getDigest(1), (layer) -> layer.entries()
				.allMatch((entry) -> startsWithOneOf(entry, layersIndex.getLayer("spring-boot-loader"))));
		ImageAssertions.assertThat(imageReference).layer(digests.getDigest(2), (layer) -> layer.entries()
				.allMatch((entry) -> startsWithOneOf(entry, layersIndex.getLayer("snapshot-dependencies"))));
		ImageAssertions.assertThat(imageReference).layer(digests.getDigest(3), (layer) -> layer.entries()
				.allMatch((entry) -> startsWithOneOf(entry, layersIndex.getLayer("application"))));
		ImageAssertions.assertThat(imageReference).layer(digests.getDigest(4),
				(layer) -> layer.entries().allMatch((entry) -> entry.contains("lib/spring-cloud-bindings-")));
	}

	private File projectArchiveFile() {
		return new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0];
	}

	private String javaMajorVersion() {
		String javaVersion = System.getProperty("java.version");
		if (javaVersion.startsWith("1.")) {
			return javaVersion.substring(2, 3);
		}
		else {
			int firstDotIndex = javaVersion.indexOf(".");
			if (firstDotIndex != -1) {
				return javaVersion.substring(0, firstDotIndex);
			}
		}
		return javaVersion;
	}

	private boolean startsWithOneOf(String actual, List<String> expectedPrefixes) {
		for (String prefix : expectedPrefixes) {
			if (actual.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private void removeImage(ImageReference image) throws IOException {
		new DockerApi().image().remove(image, false);
	}

	private static class DigestCapturingCondition extends Condition<Object> {

		private static String digest = null;

		DigestCapturingCondition() {
			super(predicate(), "a value starting with 'sha256:'");
		}

		private static Predicate<Object> predicate() {
			return (sha) -> {
				digest = sha.toString();
				return sha.toString().startsWith("sha256:");
			};
		}

		String getDigest() {
			return digest;
		}

	}

	private static class DigestsCapturingCondition extends Condition<Object> {

		private static List<String> digests;

		DigestsCapturingCondition() {
			super(predicate(), "a value starting with 'sha256:'");
		}

		private static Predicate<Object> predicate() {
			digests = new ArrayList<>();
			return (sha) -> {
				digests.add(sha.toString());
				return sha.toString().startsWith("sha256:");
			};
		}

		String getDigest(int index) {
			return digests.get(index);
		}

	}

}
