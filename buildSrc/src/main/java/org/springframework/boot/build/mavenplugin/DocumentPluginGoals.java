/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.build.mavenplugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.mavenplugin.PluginXmlParser.Mojo;
import org.springframework.boot.build.mavenplugin.PluginXmlParser.Parameter;
import org.springframework.boot.build.mavenplugin.PluginXmlParser.Plugin;

/**
 * A {@link Task} to document the plugin's goals.
 *
 * @author Andy Wilkinson
 */
public class DocumentPluginGoals extends DefaultTask {

	private final PluginXmlParser parser = new PluginXmlParser();

	private File pluginXml;

	private File outputDir;

	private Map<String, String> goalSections;

	/**
	 * Returns the output directory.
	 * @return the output directory
	 */
	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	/**
	 * Sets the output directory for the DocumentPluginGoals class.
	 * @param outputDir the output directory to be set
	 */
	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * Retrieves the goal sections of the DocumentPluginGoals class.
	 * @return A map containing the goal sections, where the key is the section name and
	 * the value is the section content.
	 */
	@Input
	public Map<String, String> getGoalSections() {
		return this.goalSections;
	}

	/**
	 * Sets the goal sections for the DocumentPluginGoals class.
	 * @param goalSections a map containing the goal sections to be set
	 */
	public void setGoalSections(Map<String, String> goalSections) {
		this.goalSections = goalSections;
	}

	/**
	 * Returns the pluginXml file.
	 * @return the pluginXml file
	 */
	@InputFile
	public File getPluginXml() {
		return this.pluginXml;
	}

	/**
	 * Sets the pluginXml file for the DocumentPluginGoals class.
	 * @param pluginXml the pluginXml file to be set
	 */
	public void setPluginXml(File pluginXml) {
		this.pluginXml = pluginXml;
	}

	/**
	 * Generates Javadoc style documentation for the method "documentPluginGoals" in the
	 * "DocumentPluginGoals" class. This method is responsible for parsing the plugin XML
	 * file, writing an overview of the plugin, and documenting each mojo in the plugin.
	 * @throws IOException if an I/O error occurs while parsing the plugin XML file or
	 * writing the documentation.
	 */
	@TaskAction
	public void documentPluginGoals() throws IOException {
		Plugin plugin = this.parser.parse(this.pluginXml);
		writeOverview(plugin);
		for (Mojo mojo : plugin.getMojos()) {
			documentMojo(plugin, mojo);
		}
	}

	/**
	 * Writes the overview of the plugin.
	 * @param plugin the plugin for which the overview is to be written
	 * @throws IOException if an I/O error occurs while writing the overview
	 */
	private void writeOverview(Plugin plugin) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(new File(this.outputDir, "overview.adoc")))) {
			writer.println("[cols=\"1,3\"]");
			writer.println("|===");
			writer.println("| Goal | Description");
			writer.println();
			for (Mojo mojo : plugin.getMojos()) {
				writer.printf("| <<%s,%s:%s>>%n", goalSectionId(mojo), plugin.getGoalPrefix(), mojo.getGoal());
				writer.printf("| %s%n", mojo.getDescription());
				writer.println();
			}
			writer.println("|===");
		}
	}

	/**
	 * Generates a documentation file for a Mojo of a Plugin.
	 * @param plugin The Plugin to which the Mojo belongs.
	 * @param mojo The Mojo for which the documentation is generated.
	 * @throws IOException If an I/O error occurs while writing the documentation file.
	 */
	private void documentMojo(Plugin plugin, Mojo mojo) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(new File(this.outputDir, mojo.getGoal() + ".adoc")))) {
			String sectionId = goalSectionId(mojo);
			writer.println();
			writer.println();
			writer.printf("[[%s]]%n", sectionId);
			writer.printf("= `%s:%s`%n", plugin.getGoalPrefix(), mojo.getGoal());
			writer.printf("`%s:%s:%s`%n", plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
			writer.println();
			writer.println(mojo.getDescription());
			List<Parameter> parameters = mojo.getParameters().stream().filter(Parameter::isEditable).toList();
			List<Parameter> requiredParameters = parameters.stream().filter(Parameter::isRequired).toList();
			String detailsSectionId = sectionId + ".parameter-details";
			if (!requiredParameters.isEmpty()) {
				writer.println();
				writer.println();
				writer.printf("[[%s.required-parameters]]%n", sectionId);
				writer.println("== Required parameters");
				writeParametersTable(writer, detailsSectionId, requiredParameters);
			}
			List<Parameter> optionalParameters = parameters.stream()
				.filter((parameter) -> !parameter.isRequired())
				.toList();
			if (!optionalParameters.isEmpty()) {
				writer.println();
				writer.println();
				writer.printf("[[%s.optional-parameters]]%n", sectionId);
				writer.println("== Optional parameters");
				writeParametersTable(writer, detailsSectionId, optionalParameters);
			}
			writer.println();
			writer.println();
			writer.printf("[[%s]]%n", detailsSectionId);
			writer.println("== Parameter details");
			writeParameterDetails(writer, parameters, detailsSectionId);
		}
	}

	/**
	 * Returns the section ID for the given Mojo goal.
	 * @param mojo The Mojo object representing the goal.
	 * @return The section ID for the goal.
	 * @throws IllegalStateException If the goal has not been assigned to a section.
	 */
	private String goalSectionId(Mojo mojo) {
		String goalSection = this.goalSections.get(mojo.getGoal());
		if (goalSection == null) {
			throw new IllegalStateException("Goal '" + mojo.getGoal() + "' has not be assigned to a section");
		}
		String sectionId = goalSection + "." + mojo.getGoal() + "-goal";
		return sectionId;
	}

	/**
	 * Writes a parameters table to the given PrintWriter.
	 * @param writer the PrintWriter to write the table to
	 * @param detailsSectionId the ID of the details section
	 * @param parameters the list of parameters to include in the table
	 */
	private void writeParametersTable(PrintWriter writer, String detailsSectionId, List<Parameter> parameters) {
		writer.println("[cols=\"3,2,3\"]");
		writer.println("|===");
		writer.println("| Name | Type | Default");
		writer.println();
		for (Parameter parameter : parameters) {
			String name = parameter.getName();
			writer.printf("| <<%s.%s,%s>>%n", detailsSectionId, parameterId(name), name);
			writer.printf("| `%s`%n", typeNameToJavadocLink(shortTypeName(parameter.getType()), parameter.getType()));
			String defaultValue = parameter.getDefaultValue();
			if (defaultValue != null) {
				writer.printf("| `%s`%n", defaultValue);
			}
			else {
				writer.println("|");
			}
			writer.println();
		}
		writer.println("|===");
	}

	/**
	 * Writes the parameter details to the given PrintWriter.
	 * @param writer the PrintWriter to write the details to
	 * @param parameters the list of parameters to write
	 * @param sectionId the section ID to use for the parameter details
	 */
	private void writeParameterDetails(PrintWriter writer, List<Parameter> parameters, String sectionId) {
		for (Parameter parameter : parameters) {
			String name = parameter.getName();
			writer.println();
			writer.println();
			writer.printf("[[%s.%s]]%n", sectionId, parameterId(name));
			writer.printf("=== `%s`%n", name);
			writer.println(parameter.getDescription());
			writer.println();
			writer.println("[cols=\"10h,90\"]");
			writer.println("|===");
			writer.println();
			writeDetail(writer, "Name", name);
			writeDetail(writer, "Type", typeNameToJavadocLink(parameter.getType()));
			writeOptionalDetail(writer, "Default value", parameter.getDefaultValue());
			writeOptionalDetail(writer, "User property", parameter.getUserProperty());
			writeOptionalDetail(writer, "Since", parameter.getSince());
			writer.println("|===");
		}
	}

	/**
	 * Generates a parameter ID based on the given name.
	 * @param name the name of the parameter
	 * @return the generated parameter ID
	 */
	private String parameterId(String name) {
		StringBuilder id = new StringBuilder(name.length() + 4);
		for (char c : name.toCharArray()) {
			if (Character.isLowerCase(c)) {
				id.append(c);
			}
			else {
				id.append("-");
				id.append(Character.toLowerCase(c));
			}
		}
		return id.toString();
	}

	/**
	 * Writes the detail information to the given PrintWriter.
	 * @param writer the PrintWriter to write the detail information to
	 * @param name the name of the detail
	 * @param value the value of the detail
	 */
	private void writeDetail(PrintWriter writer, String name, String value) {
		writer.printf("| %s%n", name);
		writer.printf("| `%s`%n", value);
		writer.println();
	}

	/**
	 * Writes an optional detail to the given PrintWriter.
	 * @param writer the PrintWriter to write to
	 * @param name the name of the detail
	 * @param value the value of the detail (can be null)
	 */
	private void writeOptionalDetail(PrintWriter writer, String name, String value) {
		writer.printf("| %s%n", name);
		if (value != null) {
			writer.printf("| `%s`%n", value);
		}
		else {
			writer.println("|");
		}
		writer.println();
	}

	/**
	 * Returns the short type name of a given name.
	 * @param name the name to get the short type name from
	 * @return the short type name
	 */
	private String shortTypeName(String name) {
		if (name.lastIndexOf('.') >= 0) {
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		if (name.lastIndexOf('$') >= 0) {
			name = name.substring(name.lastIndexOf('$') + 1);
		}
		return name;
	}

	/**
	 * Generates a Javadoc link for the given type name.
	 * @param name the name of the type
	 * @return the Javadoc link for the type
	 */
	private String typeNameToJavadocLink(String name) {
		return typeNameToJavadocLink(name, name);
	}

	/**
	 * Generates a Javadoc link for the given type name.
	 * @param shortName the short name of the type
	 * @param name the fully qualified name of the type
	 * @return the Javadoc link for the type name, or the short name if no link is
	 * available
	 */
	private String typeNameToJavadocLink(String shortName, String name) {
		if (name.startsWith("org.springframework.boot.maven")) {
			return "{spring-boot-docs}/maven-plugin/api/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]";
		}
		if (name.startsWith("org.springframework.boot")) {
			return "{spring-boot-docs}/api/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]";
		}
		return shortName;
	}

	/**
	 * Converts a type name to a Javadoc path.
	 * @param name the type name to convert
	 * @return the Javadoc path
	 */
	private String typeNameToJavadocPath(String name) {
		return name.replace(".", "/").replace("$", ".");
	}

}
