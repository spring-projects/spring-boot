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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.plexus.util.StringUtils;

/**
 * A helper class generating a report from the metadata of a particular service.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class ListMetadataCommand {

	private static final String NEW_LINE = System.getProperty("line.separator");

	private final InitializrServiceHttpInvoker initializrServiceInvoker;

	/**
	 * Creates an instance using the specified {@link CloseableHttpClient}.
	 */
	ListMetadataCommand(CloseableHttpClient httpClient) {
		this.initializrServiceInvoker = new InitializrServiceHttpInvoker(httpClient);
	}

	/**
	 * Generate a report for the specified service. The report contains the available
	 * capabilities as advertized by the root endpoint.
	 */
	String generateReport(String serviceUrl) throws IOException {
		InitializrServiceMetadata metadata = initializrServiceInvoker.loadMetadata(serviceUrl);
		String header = "Capabilities of " + serviceUrl;
		int size = header.length();

		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.repeat("=", size)).append(NEW_LINE)
				.append(header).append(NEW_LINE)
				.append(StringUtils.repeat("=", size)).append(NEW_LINE)
				.append(NEW_LINE)
				.append("Available dependencies:").append(NEW_LINE)
				.append("-----------------------").append(NEW_LINE);

		List<Dependency> dependencies = new ArrayList<Dependency>(metadata.getDependencies());
		Collections.sort(dependencies, new Comparator<Dependency>() {
			@Override
			public int compare(Dependency o1, Dependency o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
		for (Dependency dependency : dependencies) {
			sb.append(dependency.getId()).append(" - ").append(dependency.getName());
			if (dependency.getDescription() != null) {
				sb.append(": ").append(dependency.getDescription());
			}
			sb.append(NEW_LINE);
		}

		sb.append(NEW_LINE)
				.append("Available project types:").append(NEW_LINE)
				.append("------------------------").append(NEW_LINE);
		List<String> typeIds = new ArrayList<String>(metadata.getProjectTypes().keySet());
		Collections.sort(typeIds);
		for (String typeId : typeIds) {
			ProjectType type = metadata.getProjectTypes().get(typeId);
			sb.append(typeId).append(" -  ").append(type.getName());
			if (!type.getTags().isEmpty()) {
				sb.append(" [");
				Iterator<Map.Entry<String, String>> it = type.getTags().entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> entry = it.next();
					sb.append(entry.getKey()).append(":").append(entry.getValue());
					if (it.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("]");
			}
			if (type.isDefaultType()) {
				sb.append(" (default)");
			}
			sb.append(NEW_LINE);
		}

		sb.append(NEW_LINE)
				.append("Defaults:").append(NEW_LINE)
				.append("---------").append(NEW_LINE);

		List<String> defaultsKeys = new ArrayList<String>(metadata.getDefaults().keySet());
		Collections.sort(defaultsKeys);
		for (String defaultsKey : defaultsKeys) {
			sb.append(defaultsKey).append(": ").append(metadata.getDefaults().get(defaultsKey)).append(NEW_LINE);
		}
		return sb.toString();
	}

}
