/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A helper class generating a report from the meta-data of a particular service.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.2.0
 */
class ServiceCapabilitiesReportGenerator {

	private static final String NEW_LINE = System.getProperty("line.separator");

	private final InitializrService initializrService;

	/**
	 * Creates an instance using the specified {@link InitializrService}.
	 * @param initializrService the initializr service
	 */
	ServiceCapabilitiesReportGenerator(InitializrService initializrService) {
		this.initializrService = initializrService;
	}

	/**
	 * Generate a report for the specified service. The report contains the available
	 * capabilities as advertised by the root endpoint.
	 * @param url the url of the service
	 * @return the report that describes the service
	 * @throws IOException if the report cannot be generated
	 */
	public String generate(String url) throws IOException {
		Object content = this.initializrService.loadServiceCapabilities(url);
		if (content instanceof InitializrServiceMetadata) {
			return generateHelp(url, (InitializrServiceMetadata) content);
		}
		return content.toString();
	}

	private String generateHelp(String url, InitializrServiceMetadata metadata) {
		String header = "Capabilities of " + url;
		StringBuilder report = new StringBuilder();
		report.append(repeat("=", header.length()) + NEW_LINE);
		report.append(header + NEW_LINE);
		report.append(repeat("=", header.length()) + NEW_LINE);
		report.append(NEW_LINE);
		reportAvailableDependencies(metadata, report);
		report.append(NEW_LINE);
		reportAvailableProjectTypes(metadata, report);
		report.append(NEW_LINE);
		reportDefaults(report, metadata);
		return report.toString();
	}

	private void reportAvailableDependencies(InitializrServiceMetadata metadata,
			StringBuilder report) {
		report.append("Available dependencies:" + NEW_LINE);
		report.append("-----------------------" + NEW_LINE);
		List<Dependency> dependencies = getSortedDependencies(metadata);
		for (Dependency dependency : dependencies) {
			report.append(dependency.getId() + " - " + dependency.getName());
			if (dependency.getDescription() != null) {
				report.append(": " + dependency.getDescription());
			}
			report.append(NEW_LINE);
		}
	}

	private List<Dependency> getSortedDependencies(InitializrServiceMetadata metadata) {
		ArrayList<Dependency> dependencies = new ArrayList<Dependency>(
				metadata.getDependencies());
		Collections.sort(dependencies, new Comparator<Dependency>() {
			@Override
			public int compare(Dependency o1, Dependency o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
		return dependencies;
	}

	private void reportAvailableProjectTypes(InitializrServiceMetadata metadata,
			StringBuilder report) {
		report.append("Available project types:" + NEW_LINE);
		report.append("------------------------" + NEW_LINE);
		SortedSet<Entry<String, ProjectType>> entries = new TreeSet<Entry<String, ProjectType>>(
				new Comparator<Entry<String, ProjectType>>() {

					@Override
					public int compare(Entry<String, ProjectType> o1,
							Entry<String, ProjectType> o2) {
						return o1.getKey().compareTo(o2.getKey());
					}

				});
		entries.addAll(metadata.getProjectTypes().entrySet());
		for (Entry<String, ProjectType> entry : entries) {
			ProjectType type = entry.getValue();
			report.append(entry.getKey() + " -  " + type.getName());
			if (!type.getTags().isEmpty()) {
				reportTags(report, type);
			}
			if (type.isDefaultType()) {
				report.append(" (default)");
			}
			report.append(NEW_LINE);
		}
	}

	private void reportTags(StringBuilder report, ProjectType type) {
		Map<String, String> tags = type.getTags();
		Iterator<Map.Entry<String, String>> iterator = tags.entrySet().iterator();
		report.append(" [");
		while (iterator.hasNext()) {
			Map.Entry<String, String> entry = iterator.next();
			report.append(entry.getKey() + ":" + entry.getValue());
			if (iterator.hasNext()) {
				report.append(", ");
			}
		}
		report.append("]");
	}

	private void reportDefaults(StringBuilder report,
			InitializrServiceMetadata metadata) {
		report.append("Defaults:" + NEW_LINE);
		report.append("---------" + NEW_LINE);
		List<String> defaultsKeys = new ArrayList<String>(
				metadata.getDefaults().keySet());
		Collections.sort(defaultsKeys);
		for (String defaultsKey : defaultsKeys) {
			String defaultsValue = metadata.getDefaults().get(defaultsKey);
			report.append(defaultsKey + ": " + defaultsValue + NEW_LINE);
		}
	}

	private static String repeat(String s, int count) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append(s);
		}
		return sb.toString();
	}

}
