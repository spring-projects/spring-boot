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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A parser for a Maven plugin's {@code plugin.xml} file.
 *
 * @author Andy Wilkinson
 * @author Mike Smithson
 */
class PluginXmlParser {

	private final XPath xpath;

	/**
	 * Constructs a new PluginXmlParser object. Initializes the XPath object using the
	 * XPathFactory.
	 */
	PluginXmlParser() {
		this.xpath = XPathFactory.newInstance().newXPath();
	}

	/**
	 * Parses the given plugin XML file and returns a Plugin object.
	 * @param pluginXml the plugin XML file to parse
	 * @return the parsed Plugin object
	 * @throws RuntimeException if an error occurs during parsing
	 */
	Plugin parse(File pluginXml) {
		try {
			Node root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pluginXml);
			List<Mojo> mojos = parseMojos(root);
			return new Plugin(textAt("//plugin/groupId", root), textAt("//plugin/artifactId", root),
					textAt("//plugin/version", root), textAt("//plugin/goalPrefix", root), mojos);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Retrieves the text content at the specified XPath path from the given XML node.
	 * @param path the XPath path to the desired text content
	 * @param source the XML node from which to retrieve the text content
	 * @return the text content at the specified XPath path, or null if it is empty
	 * @throws XPathExpressionException if an error occurs while evaluating the XPath
	 * expression
	 */
	private String textAt(String path, Node source) throws XPathExpressionException {
		String text = this.xpath.evaluate(path + "/text()", source);
		return text.isEmpty() ? null : text;
	}

	/**
	 * Parses the mojos from the given plugin node.
	 * @param plugin the plugin node to parse mojos from
	 * @return a list of Mojo objects parsed from the plugin node
	 * @throws XPathExpressionException if there is an error in the XPath expression
	 */
	private List<Mojo> parseMojos(Node plugin) throws XPathExpressionException {
		List<Mojo> mojos = new ArrayList<>();
		for (Node mojoNode : nodesAt("//plugin/mojos/mojo", plugin)) {
			mojos.add(new Mojo(textAt("goal", mojoNode), format(textAt("description", mojoNode)),
					parseParameters(mojoNode)));
		}
		return mojos;
	}

	/**
	 * Returns an iterable collection of nodes at the specified path from the given source
	 * node.
	 * @param path the XPath expression specifying the path to the desired nodes
	 * @param source the node from which to start the search
	 * @return an iterable collection of nodes at the specified path
	 * @throws XPathExpressionException if an error occurs while evaluating the XPath
	 * expression
	 */
	private Iterable<Node> nodesAt(String path, Node source) throws XPathExpressionException {
		return IterableNodeList.of((NodeList) this.xpath.evaluate(path, source, XPathConstants.NODESET));
	}

	/**
	 * Parses the parameters from the given Mojo node.
	 * @param mojoNode the Mojo node to parse
	 * @return a list of Parameter objects representing the parsed parameters
	 * @throws XPathExpressionException if there is an error in the XPath expression
	 */
	private List<Parameter> parseParameters(Node mojoNode) throws XPathExpressionException {
		Map<String, String> defaultValues = new HashMap<>();
		Map<String, String> userProperties = new HashMap<>();
		for (Node parameterConfigurationNode : nodesAt("configuration/*", mojoNode)) {
			String userProperty = parameterConfigurationNode.getTextContent();
			if (userProperty != null && !userProperty.isEmpty()) {
				userProperties.put(parameterConfigurationNode.getNodeName(),
						userProperty.replace("${", "`").replace("}", "`"));
			}
			Node defaultValueAttribute = parameterConfigurationNode.getAttributes().getNamedItem("default-value");
			if (defaultValueAttribute != null && !defaultValueAttribute.getTextContent().isEmpty()) {
				defaultValues.put(parameterConfigurationNode.getNodeName(), defaultValueAttribute.getTextContent());
			}
		}
		List<Parameter> parameters = new ArrayList<>();
		for (Node parameterNode : nodesAt("parameters/parameter", mojoNode)) {
			parameters.add(parseParameter(parameterNode, defaultValues, userProperties));
		}
		return parameters;
	}

	/**
	 * Parses a parameter node and returns a Parameter object.
	 * @param parameterNode the node representing the parameter
	 * @param defaultValues a map of default values for parameters
	 * @param userProperties a map of user properties for parameters
	 * @return the parsed Parameter object
	 * @throws XPathExpressionException if there is an error in the XPath expression
	 */
	private Parameter parseParameter(Node parameterNode, Map<String, String> defaultValues,
			Map<String, String> userProperties) throws XPathExpressionException {
		String description = textAt("description", parameterNode);
		return new Parameter(textAt("name", parameterNode), textAt("type", parameterNode),
				booleanAt("required", parameterNode), booleanAt("editable", parameterNode),
				(description != null) ? format(description) : "", defaultValues.get(textAt("name", parameterNode)),
				userProperties.get(textAt("name", parameterNode)), textAt("since", parameterNode));
	}

	/**
	 * Returns the boolean value at the specified XPath expression in the given XML node.
	 * @param path the XPath expression to evaluate
	 * @param node the XML node to search in
	 * @return the boolean value at the specified XPath expression
	 * @throws XPathExpressionException if an error occurs while evaluating the XPath
	 * expression
	 */
	private boolean booleanAt(String path, Node node) throws XPathExpressionException {
		return Boolean.parseBoolean(textAt(path, node));
	}

	/**
	 * Formats the input string by replacing certain HTML tags and special characters with
	 * their corresponding representations.
	 * @param input the string to be formatted
	 * @return the formatted string
	 */
	private String format(String input) {
		return input.replace("<code>", "`")
			.replace("</code>", "`")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("<br>", " ")
			.replace("\n", " ")
			.replace("&quot;", "\"")
			.replaceAll("\\{@code (.*?)}", "`$1`")
			.replaceAll("\\{@link (.*?)}", "`$1`")
			.replaceAll("\\{@literal (.*?)}", "`$1`")
			.replaceAll("<a href=.\"(.*?)\".>(.*?)</a>", "$1[$2]");
	}

	/**
	 * IterableNodeList class.
	 */
	private static final class IterableNodeList implements Iterable<Node> {

		private final NodeList nodeList;

		/**
		 * Constructs a new IterableNodeList object with the specified NodeList.
		 * @param nodeList the NodeList to be used for iteration
		 */
		private IterableNodeList(NodeList nodeList) {
			this.nodeList = nodeList;
		}

		/**
		 * Returns an Iterable of Nodes from the given NodeList.
		 * @param nodeList the NodeList to convert to an Iterable
		 * @return an Iterable of Nodes
		 */
		private static Iterable<Node> of(NodeList nodeList) {
			return new IterableNodeList(nodeList);
		}

		/**
		 * Returns an iterator over the elements in this IterableNodeList in proper
		 * sequence.
		 * @return an iterator over the elements in this IterableNodeList in proper
		 * sequence
		 */
		@Override
		public Iterator<Node> iterator() {

			return new Iterator<>() {

				private int index = 0;

				@Override
				public boolean hasNext() {
					return this.index < IterableNodeList.this.nodeList.getLength();
				}

				@Override
				public Node next() {
					return IterableNodeList.this.nodeList.item(this.index++);
				}

			};
		}

	}

	/**
	 * Plugin class.
	 */
	static final class Plugin {

		private final String groupId;

		private final String artifactId;

		private final String version;

		private final String goalPrefix;

		private final List<Mojo> mojos;

		/**
		 * Constructs a new Plugin with the specified parameters.
		 * @param groupId the group ID of the plugin
		 * @param artifactId the artifact ID of the plugin
		 * @param version the version of the plugin
		 * @param goalPrefix the goal prefix of the plugin
		 * @param mojos the list of mojos associated with the plugin
		 */
		private Plugin(String groupId, String artifactId, String version, String goalPrefix, List<Mojo> mojos) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
			this.goalPrefix = goalPrefix;
			this.mojos = mojos;
		}

		/**
		 * Returns the group ID of the plugin.
		 * @return the group ID of the plugin
		 */
		String getGroupId() {
			return this.groupId;
		}

		/**
		 * Returns the artifact ID of the plugin.
		 * @return the artifact ID of the plugin
		 */
		String getArtifactId() {
			return this.artifactId;
		}

		/**
		 * Returns the version of the plugin.
		 * @return the version of the plugin
		 */
		String getVersion() {
			return this.version;
		}

		/**
		 * Returns the goal prefix.
		 * @return the goal prefix
		 */
		String getGoalPrefix() {
			return this.goalPrefix;
		}

		/**
		 * Returns the list of Mojo objects.
		 * @return the list of Mojo objects
		 */
		List<Mojo> getMojos() {
			return this.mojos;
		}

	}

	/**
	 * Mojo class.
	 */
	static final class Mojo {

		private final String goal;

		private final String description;

		private final List<Parameter> parameters;

		/**
		 * Constructs a new Mojo with the specified goal, description, and parameters.
		 * @param goal the goal of the Mojo
		 * @param description the description of the Mojo
		 * @param parameters the list of parameters for the Mojo
		 */
		private Mojo(String goal, String description, List<Parameter> parameters) {
			this.goal = goal;
			this.description = description;
			this.parameters = parameters;
		}

		/**
		 * Returns the goal of the Mojo.
		 * @return the goal of the Mojo
		 */
		String getGoal() {
			return this.goal;
		}

		/**
		 * Returns the description of the Mojo.
		 * @return the description of the Mojo
		 */
		String getDescription() {
			return this.description;
		}

		/**
		 * Retrieves the list of parameters.
		 * @return the list of parameters
		 */
		List<Parameter> getParameters() {
			return this.parameters;
		}

	}

	/**
	 * Parameter class.
	 */
	static final class Parameter {

		private final String name;

		private final String type;

		private final boolean required;

		private final boolean editable;

		private final String description;

		private final String defaultValue;

		private final String userProperty;

		private final String since;

		/**
		 * Constructs a new Parameter with the specified attributes.
		 * @param name the name of the parameter
		 * @param type the type of the parameter
		 * @param required indicates if the parameter is required
		 * @param editable indicates if the parameter is editable
		 * @param description the description of the parameter
		 * @param defaultValue the default value of the parameter
		 * @param userProperty the user property of the parameter
		 * @param since the version since the parameter is available
		 */
		private Parameter(String name, String type, boolean required, boolean editable, String description,
				String defaultValue, String userProperty, String since) {
			this.name = name;
			this.type = type;
			this.required = required;
			this.editable = editable;
			this.description = description;
			this.defaultValue = defaultValue;
			this.userProperty = userProperty;
			this.since = since;
		}

		/**
		 * Returns the name of the Parameter.
		 * @return the name of the Parameter
		 */
		String getName() {
			return this.name;
		}

		/**
		 * Returns the type of the parameter.
		 * @return the type of the parameter
		 */
		String getType() {
			return this.type;
		}

		/**
		 * Returns a boolean value indicating if the parameter is required.
		 * @return true if the parameter is required, false otherwise
		 */
		boolean isRequired() {
			return this.required;
		}

		/**
		 * Returns a boolean value indicating whether the parameter is editable.
		 * @return true if the parameter is editable, false otherwise
		 */
		boolean isEditable() {
			return this.editable;
		}

		/**
		 * Returns the description of the Parameter.
		 * @return the description of the Parameter
		 */
		String getDescription() {
			return this.description;
		}

		/**
		 * Returns the default value of the parameter.
		 * @return the default value of the parameter
		 */
		String getDefaultValue() {
			return this.defaultValue;
		}

		/**
		 * Returns the value of the userProperty.
		 * @return the value of the userProperty
		 */
		String getUserProperty() {
			return this.userProperty;
		}

		/**
		 * Returns the value of the "since" parameter.
		 * @return the value of the "since" parameter
		 */
		String getSince() {
			return this.since;
		}

	}

}
