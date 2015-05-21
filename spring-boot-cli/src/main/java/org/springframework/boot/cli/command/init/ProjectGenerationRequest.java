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

package org.springframework.boot.cli.command.init;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

import org.springframework.util.StringUtils;

/**
 * Represent the settings to apply to generating the project.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 1.2.0
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

	private String packaging;

	private String type;

	private String build;

	private String format;

	private boolean detectType;

	private String javaVersion;

	private String language;

	private String bootVersion;

	private List<String> dependencies = new ArrayList<String>();

	/**
	 * The URL of the service to use.
	 * @see #DEFAULT_SERVICE_URL
	 * @return the service URL
	 */
	public String getServiceUrl() {
		return this.serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * The location of the generated project.
	 * @return the location of the generated project
	 */
	public String getOutput() {
		return this.output;
	}

	public void setOutput(String output) {
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
	public boolean isExtract() {
		return this.extract;
	}

	public void setExtract(boolean extract) {
		this.extract = extract;
	}

	/**
	 * The groupId to use or {@code null} if it should not be customized.
	 * @return the groupId or {@code null}
	 */
	public String getGroupId() {
		return this.groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * The artifactId to use or {@code null} if it should not be customized.
	 * @return the artifactId or {@code null}
	 */
	public String getArtifactId() {
		return this.artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * The artifact version to use or {@code null} if it should not be customized.
	 * @return the artifact version or {@code null}
	 */
	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * The name to use or {@code null} if it should not be customized.
	 * @return the name or {@code null}
	 */
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The description to use or {@code null} if it should not be customized.
	 * @return the description or {@code null}
	 */
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * The packaging type or {@code null} if it should not be customized.
	 * @return the packaging type or {@code null}
	 */
	public String getPackaging() {
		return this.packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	/**
	 * The type of project to generate. Should match one of the advertized type that the
	 * service supports. If not set, the default is retrieved from the service metadata.
	 * @return the project type
	 */
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * The build type to use. Ignored if a type is set. Can be used alongside the
	 * {@link #getFormat() format} to identify the type to use.
	 * @return the build type
	 */
	public String getBuild() {
		return this.build;
	}

	public void setBuild(String build) {
		this.build = build;
	}

	/**
	 * The project format to use. Ignored if a type is set. Can be used alongside the
	 * {@link #getBuild() build} to identify the type to use.
	 * @return the project format
	 */
	public String getFormat() {
		return this.format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Whether or not the type should be detected based on the build and format value.
	 * @return {@code true} if type detection will be performed, otherwise {@code false}
	 */
	public boolean isDetectType() {
		return this.detectType;
	}

	public void setDetectType(boolean detectType) {
		this.detectType = detectType;
	}

	/**
	 * The Java version to use or {@code null} if it should not be customized.
	 * @return the Java version or {@code null}
	 */
	public String getJavaVersion() {
		return this.javaVersion;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	/**
	 * The programming language to use or {@code null} if it should not be customized.
	 * @return the programming language or {@code null}
	 */
	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * The Spring Boot version to use or {@code null} if it should not be customized.
	 * @return the Spring Boot version or {@code null}
	 */
	public String getBootVersion() {
		return this.bootVersion;
	}

	public void setBootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
	}

	/**
	 * The identifiers of the dependencies to include in the project.
	 * @return the dependency identifiers
	 */
	public List<String> getDependencies() {
		return this.dependencies;
	}

	/**
	 * Generates the URI to use to generate a project represented by this request
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
				builder.setParameter("dependencies",
						StringUtils.collectionToCommaDelimitedString(this.dependencies));
			}

			if (this.groupId != null) {
				builder.setParameter("groupId", this.groupId);
			}
			if (this.artifactId != null) {
				builder.setParameter("artifactId", this.artifactId);
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
			if (this.packaging != null) {
				builder.setParameter("packaging", this.packaging);
			}
			if (this.type != null) {
				builder.setParameter("type", projectType.getId());
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
		catch (URISyntaxException e) {
			throw new ReportableException("Invalid service URL (" + e.getMessage() + ")");
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
			Map<String, ProjectType> types = new HashMap<String, ProjectType>(
					metadata.getProjectTypes());
			if (this.build != null) {
				filter(types, "build", this.build);
			}
			if (this.format != null) {
				filter(types, "format", this.format);
			}
			if (types.size() == 1) {
				return types.values().iterator().next();
			}
			else if (types.size() == 0) {
				throw new ReportableException("No type found with build '" + this.build
						+ "' and format '" + this.format
						+ "' check the service capabilities (--list)");
			}
			else {
				throw new ReportableException("Multiple types found with build '"
						+ this.build + "' and format '" + this.format
						+ "' use --type with a more specific value " + types.keySet());
			}
		}
		else {
			ProjectType defaultType = metadata.getDefaultType();
			if (defaultType == null) {
				throw new ReportableException(
						("No project type is set and no default is defined. "
								+ "Check the service capabilities (--list)"));
			}
			return defaultType;
		}
	}

	private static void filter(Map<String, ProjectType> projects, String tag,
			String tagValue) {
		for (Iterator<Map.Entry<String, ProjectType>> it = projects.entrySet().iterator(); it
				.hasNext(); ) {
			Map.Entry<String, ProjectType> entry = it.next();
			String value = entry.getValue().getTags().get(tag);
			if (!tagValue.equals(value)) {
				it.remove();
			}
		}
	}

}
