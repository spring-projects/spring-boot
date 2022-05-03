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

package org.springframework.boot.cli.command.init;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

import org.springframework.util.StringUtils;

/**
 * Represent the settings to apply to generating the project.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class ProjectGenerationRequest {

	public static final String DEFAULT_SERVICE_URL = "https://start.spring.io";

	private String serviceUrl = DEFAULT_SERVICE_URL;

	private String output;

	private boolean extract;

	private String groupId;

	private String artifactId;

	private String version;

	private String name;

	private String description;

	private String packageName;

	private String type;

	private String packaging;

	private String build;

	private String format;

	private boolean detectType;

	private String javaVersion;

	private String language;

	private String bootVersion;

	private final List<String> dependencies = new ArrayList<>();

	/**
	 * The URL of the service to use.
	 * @return the service URL
	 * @see #DEFAULT_SERVICE_URL
	 */
	String getServiceUrl() {
		return this.serviceUrl;
	}

	void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * The location of the generated project.
	 * @return the location of the generated project
	 */
	String getOutput() {
		return this.output;
	}

	void setOutput(String output) {
		if (output != null && output.endsWith("/")) {
			this.output = output.substring(0, output.length() - 1);
			this.extract = true;
		}
		else {
			this.output = output;
		}
	}

	/**
	 * Whether or not the project archive should be extracted in the output location. If
	 * the {@link #getOutput() output} ends with "/", the project is extracted
	 * automatically.
	 * @return {@code true} if the archive should be extracted, otherwise {@code false}
	 */
	boolean isExtract() {
		return this.extract;
	}

	void setExtract(boolean extract) {
		this.extract = extract;
	}

	/**
	 * The groupId to use or {@code null} if it should not be customized.
	 * @return the groupId or {@code null}
	 */
	String getGroupId() {
		return this.groupId;
	}

	void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * The artifactId to use or {@code null} if it should not be customized.
	 * @return the artifactId or {@code null}
	 */
	String getArtifactId() {
		return this.artifactId;
	}

	void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * The artifact version to use or {@code null} if it should not be customized.
	 * @return the artifact version or {@code null}
	 */
	String getVersion() {
		return this.version;
	}

	void setVersion(String version) {
		this.version = version;
	}

	/**
	 * The name to use or {@code null} if it should not be customized.
	 * @return the name or {@code null}
	 */
	String getName() {
		return this.name;
	}

	void setName(String name) {
		this.name = name;
	}

	/**
	 * The description to use or {@code null} if it should not be customized.
	 * @return the description or {@code null}
	 */
	String getDescription() {
		return this.description;
	}

	void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return the package name or {@code null} if it should not be customized.
	 * @return the package name or {@code null}
	 */
	String getPackageName() {
		return this.packageName;
	}

	void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * The type of project to generate. Should match one of the advertised type that the
	 * service supports. If not set, the default is retrieved from the service metadata.
	 * @return the project type
	 */
	String getType() {
		return this.type;
	}

	void setType(String type) {
		this.type = type;
	}

	/**
	 * The packaging type or {@code null} if it should not be customized.
	 * @return the packaging type or {@code null}
	 */
	String getPackaging() {
		return this.packaging;
	}

	void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	/**
	 * The build type to use. Ignored if a type is set. Can be used alongside the
	 * {@link #getFormat() format} to identify the type to use.
	 * @return the build type
	 */
	String getBuild() {
		return this.build;
	}

	void setBuild(String build) {
		this.build = build;
	}

	/**
	 * The project format to use. Ignored if a type is set. Can be used alongside the
	 * {@link #getBuild() build} to identify the type to use.
	 * @return the project format
	 */
	String getFormat() {
		return this.format;
	}

	void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Whether or not the type should be detected based on the build and format value.
	 * @return {@code true} if type detection will be performed, otherwise {@code false}
	 */
	boolean isDetectType() {
		return this.detectType;
	}

	void setDetectType(boolean detectType) {
		this.detectType = detectType;
	}

	/**
	 * The Java version to use or {@code null} if it should not be customized.
	 * @return the Java version or {@code null}
	 */
	String getJavaVersion() {
		return this.javaVersion;
	}

	void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	/**
	 * The programming language to use or {@code null} if it should not be customized.
	 * @return the programming language or {@code null}
	 */
	String getLanguage() {
		return this.language;
	}

	void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * The Spring Boot version to use or {@code null} if it should not be customized.
	 * @return the Spring Boot version or {@code null}
	 */
	String getBootVersion() {
		return this.bootVersion;
	}

	void setBootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
	}

	/**
	 * The identifiers of the dependencies to include in the project.
	 * @return the dependency identifiers
	 */
	List<String> getDependencies() {
		return this.dependencies;
	}

	/**
	 * Generates the URI to use to generate a project represented by this request.
	 * @param metadata the metadata that describes the service
	 * @return the project generation URI
	 */
	URI generateUrl(InitializrServiceMetadata metadata) {
		try {
			URIBuilder builder = new URIBuilder(this.serviceUrl);
			StringBuilder sb = new StringBuilder();
			if (builder.getPath() != null) {
				sb.append(builder.getPath());
			}

			ProjectType projectType = determineProjectType(metadata);
			this.type = projectType.getId();
			sb.append(projectType.getAction());
			builder.setPath(sb.toString());

			if (!this.dependencies.isEmpty()) {
				builder.setParameter("dependencies", StringUtils.collectionToCommaDelimitedString(this.dependencies));
			}

			if (this.groupId != null) {
				builder.setParameter("groupId", this.groupId);
			}
			String resolvedArtifactId = resolveArtifactId();
			if (resolvedArtifactId != null) {
				builder.setParameter("artifactId", resolvedArtifactId);
			}
			if (this.version != null) {
				builder.setParameter("version", this.version);
			}
			if (this.name != null) {
				builder.setParameter("name", this.name);
			}
			if (this.description != null) {
				builder.setParameter("description", this.description);
			}
			if (this.packageName != null) {
				builder.setParameter("packageName", this.packageName);
			}
			if (this.type != null) {
				builder.setParameter("type", projectType.getId());
			}
			if (this.packaging != null) {
				builder.setParameter("packaging", this.packaging);
			}
			if (this.javaVersion != null) {
				builder.setParameter("javaVersion", this.javaVersion);
			}
			if (this.language != null) {
				builder.setParameter("language", this.language);
			}
			if (this.bootVersion != null) {
				builder.setParameter("bootVersion", this.bootVersion);
			}

			return builder.build();
		}
		catch (URISyntaxException ex) {
			throw new ReportableException("Invalid service URL (" + ex.getMessage() + ")");
		}
	}

	protected ProjectType determineProjectType(InitializrServiceMetadata metadata) {
		if (this.type != null) {
			ProjectType result = metadata.getProjectTypes().get(this.type);
			if (result == null) {
				throw new ReportableException(
						("No project type with id '" + this.type + "' - check the service capabilities (--list)"));
			}
			return result;
		}
		else if (isDetectType()) {
			Map<String, ProjectType> types = new HashMap<>(metadata.getProjectTypes());
			if (this.build != null) {
				filter(types, "build", this.build);
			}
			if (this.format != null) {
				filter(types, "format", this.format);
			}
			if (types.size() == 1) {
				return types.values().iterator().next();
			}
			else if (types.isEmpty()) {
				throw new ReportableException("No type found with build '" + this.build + "' and format '" + this.format
						+ "' check the service capabilities (--list)");
			}
			else {
				throw new ReportableException("Multiple types found with build '" + this.build + "' and format '"
						+ this.format + "' use --type with a more specific value " + types.keySet());
			}
		}
		else {
			ProjectType defaultType = metadata.getDefaultType();
			if (defaultType == null) {
				throw new ReportableException(("No project type is set and no default is defined. "
						+ "Check the service capabilities (--list)"));
			}
			return defaultType;
		}
	}

	/**
	 * Resolve the artifactId to use or {@code null} if it should not be customized.
	 * @return the artifactId
	 */
	protected String resolveArtifactId() {
		if (this.artifactId != null) {
			return this.artifactId;
		}
		if (this.output != null) {
			int i = this.output.lastIndexOf('.');
			return (i != -1) ? this.output.substring(0, i) : this.output;
		}
		return null;
	}

	private static void filter(Map<String, ProjectType> projects, String tag, String tagValue) {
		projects.entrySet().removeIf((entry) -> !tagValue.equals(entry.getValue().getTags().get(tag)));
	}

}
