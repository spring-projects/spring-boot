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

package org.springframework.boot.cli.command.init;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

/**
 * Represent the settings to apply to generating the project.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class ProjectGenerationRequest {

	public static final String DEFAULT_SERVICE_URL = "https://start.spring.io";

	private String serviceUrl = DEFAULT_SERVICE_URL;

	private String output;

	private String bootVersion;

	private List<String> dependencies = new ArrayList<String>();

	private String javaVersion;

	private String packaging;

	private String build;

	private String format;

	private boolean detectType;

	private String type;

	/**
	 * The url of the service to use.
	 * @see #DEFAULT_SERVICE_URL
	 */
	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * The location of the  generated project.
	 */
	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	/**
	 * The Spring Boot version to use or {@code null} if it should not be customized.
	 */
	public String getBootVersion() {
		return bootVersion;
	}

	public void setBootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
	}

	/**
	 * The identifiers of the dependencies to include in the project.
	 */
	public List<String> getDependencies() {
		return dependencies;
	}

	/**
	 * The Java version to use or {@code null} if it should not be customized.
	 */
	public String getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	/**
	 * The packaging type or {@code null} if it should not be customized.
	 */
	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	/**
	 * The build type to use. Ignored if a type is set. Can be used alongside
	 * the {@link #getFormat() format} to identify the type to use.
	 */
	public String getBuild() {
		return build;
	}

	public void setBuild(String build) {
		this.build = build;
	}

	/**
	 * The project format to use. Ignored if a type is set. Can be used alongside
	 * the {@link #getBuild() build} to identify the type to use.
	 */
	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Specify if the type should be detected based on the build
	 * and format value.
	 */
	public boolean isDetectType() {
		return detectType;
	}

	public void setDetectType(boolean detectType) {
		this.detectType = detectType;
	}

	/**
	 * The type of project to generate. Should match one of the advertized type
	 * that the service supports. If not set, the default is retrieved from
	 * the service metadata.
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Generates the URL to use to generate a project represented
	 * by this request
	 */
	URI generateUrl(InitializrServiceMetadata metadata) {
		try {
			URIBuilder builder = new URIBuilder(serviceUrl);
			StringBuilder sb = new StringBuilder();
			if (builder.getPath() != null) {
				sb.append(builder.getPath());
			}

			ProjectType projectType = determineProjectType(metadata);
			this.type = projectType.getId();
			sb.append(projectType.getAction());
			builder.setPath(sb.toString());

			if (this.bootVersion != null) {
				builder.setParameter("bootVersion", this.bootVersion);
			}
			for (String dependency : dependencies) {
				builder.addParameter("style", dependency);
			}
			if (this.javaVersion != null) {
				builder.setParameter("javaVersion", this.javaVersion);
			}
			if (this.packaging != null) {
				builder.setParameter("packaging", this.packaging);
			}
			if (this.type != null) {
				builder.setParameter("type", projectType.getId());
			}

			return builder.build();
		}
		catch (URISyntaxException e) {
			throw new ProjectGenerationException("Invalid service URL (" + e.getMessage() + ")");
		}
	}

	protected ProjectType determineProjectType(InitializrServiceMetadata metadata) {
		if (this.type != null) {
			ProjectType result = metadata.getProjectTypes().get(this.type);
			if (result == null) {
				throw new ProjectGenerationException(("No project type with id '" + this.type +
						"' - check the service capabilities (--list)"));
			}
		}
		if (isDetectType()) {
			Map<String, ProjectType> types = new HashMap<String, ProjectType>(metadata.getProjectTypes());
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
				throw new ProjectGenerationException("No type found with build '" + this.build + "' and format '"
						+ this.format + "' check the service capabilities (--list)");
			}
			else {
				throw new ProjectGenerationException("Multiple types found with build '" + this.build
						+ "' and format '" + this.format + "' use --type with a more specific value " + types.keySet());
			}
		}
		ProjectType defaultType = metadata.getDefaultType();
		if (defaultType == null) {
			throw new ProjectGenerationException(("No project type is set and no default is defined. " +
					"Check the service capabilities (--list)"));
		}
		return defaultType;
	}

	private static void filter(Map<String, ProjectType> projects, String tag, String tagValue) {
		for (Iterator<Map.Entry<String, ProjectType>> it = projects.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, ProjectType> entry = it.next();
			String value = entry.getValue().getTags().get(tag);
			if (!tagValue.equals(value)) {
				it.remove();
			}
		}
	}

}
