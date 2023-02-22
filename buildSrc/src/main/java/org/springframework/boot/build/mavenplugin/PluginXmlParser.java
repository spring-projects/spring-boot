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

	PluginXmlParser() {
		this.xpath = XPathFactory.newInstance().newXPath();
	}

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

	private String textAt(String path, Node source) throws XPathExpressionException {
		String text = this.xpath.evaluate(path + "/text()", source);
		return text.isEmpty() ? null : text;
	}

	private List<Mojo> parseMojos(Node plugin) throws XPathExpressionException {
		List<Mojo> mojos = new ArrayList<>();
		for (Node mojoNode : nodesAt("//plugin/mojos/mojo", plugin)) {
			mojos.add(new Mojo(textAt("goal", mojoNode), format(textAt("description", mojoNode)),
					parseParameters(mojoNode)));
		}
		return mojos;
	}

	private Iterable<Node> nodesAt(String path, Node source) throws XPathExpressionException {
		return IterableNodeList.of((NodeList) this.xpath.evaluate(path, source, XPathConstants.NODESET));
	}

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

	private Parameter parseParameter(Node parameterNode, Map<String, String> defaultValues,
			Map<String, String> userProperties) throws XPathExpressionException {
		String description = textAt("description", parameterNode);
		return new Parameter(textAt("name", parameterNode), textAt("type", parameterNode),
				booleanAt("required", parameterNode), booleanAt("editable", parameterNode),
				(description != null) ? format(description) : "", defaultValues.get(textAt("name", parameterNode)),
				userProperties.get(textAt("name", parameterNode)), textAt("since", parameterNode));
	}

	private boolean booleanAt(String path, Node node) throws XPathExpressionException {
		return Boolean.parseBoolean(textAt(path, node));
	}

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

	private static final class IterableNodeList implements Iterable<Node> {

		private final NodeList nodeList;

		private IterableNodeList(NodeList nodeList) {
			this.nodeList = nodeList;
		}

		private static Iterable<Node> of(NodeList nodeList) {
			return new IterableNodeList(nodeList);
		}

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

	static final class Plugin {

		private final String groupId;

		private final String artifactId;

		private final String version;

		private final String goalPrefix;

		private final List<Mojo> mojos;

		private Plugin(String groupId, String artifactId, String version, String goalPrefix, List<Mojo> mojos) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
			this.goalPrefix = goalPrefix;
			this.mojos = mojos;
		}

		String getGroupId() {
			return this.groupId;
		}

		String getArtifactId() {
			return this.artifactId;
		}

		String getVersion() {
			return this.version;
		}

		String getGoalPrefix() {
			return this.goalPrefix;
		}

		List<Mojo> getMojos() {
			return this.mojos;
		}

	}

	static final class Mojo {

		private final String goal;

		private final String description;

		private final List<Parameter> parameters;

		private Mojo(String goal, String description, List<Parameter> parameters) {
			this.goal = goal;
			this.description = description;
			this.parameters = parameters;
		}

		String getGoal() {
			return this.goal;
		}

		String getDescription() {
			return this.description;
		}

		List<Parameter> getParameters() {
			return this.parameters;
		}

	}

	static final class Parameter {

		private final String name;

		private final String type;

		private final boolean required;

		private final boolean editable;

		private final String description;

		private final String defaultValue;

		private final String userProperty;

		private final String since;

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

		String getName() {
			return this.name;
		}

		String getType() {
			return this.type;
		}

		boolean isRequired() {
			return this.required;
		}

		boolean isEditable() {
			return this.editable;
		}

		String getDescription() {
			return this.description;
		}

		String getDefaultValue() {
			return this.defaultValue;
		}

		String getUserProperty() {
			return this.userProperty;
		}

		String getSince() {
			return this.since;
		}

	}

}
