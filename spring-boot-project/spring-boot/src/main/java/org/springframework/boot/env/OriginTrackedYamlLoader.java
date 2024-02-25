/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.core.io.Resource;

/**
 * Class to load {@code .yml} files into a map of {@code String} to
 * {@link OriginTrackedValue}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OriginTrackedYamlLoader extends YamlProcessor {

	private final Resource resource;

	/**
     * Constructs a new OriginTrackedYamlLoader with the specified resource.
     * 
     * @param resource the resource to be loaded
     */
    OriginTrackedYamlLoader(Resource resource) {
		this.resource = resource;
		setResources(resource);
	}

	/**
     * Creates a Yaml object with customized loader options.
     * 
     * @return the created Yaml object
     */
    @Override
	protected Yaml createYaml() {
		LoaderOptions loaderOptions = new LoaderOptions();
		loaderOptions.setAllowDuplicateKeys(false);
		loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
		loaderOptions.setAllowRecursiveKeys(true);
		loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
		return createYaml(loaderOptions);
	}

	/**
     * Creates a new instance of Yaml with the specified LoaderOptions.
     * 
     * @param loaderOptions the LoaderOptions to be used for creating the Yaml instance
     * @return a new instance of Yaml
     */
    private Yaml createYaml(LoaderOptions loaderOptions) {
		BaseConstructor constructor = new OriginTrackingConstructor(loaderOptions);
		DumperOptions dumperOptions = new DumperOptions();
		Representer representer = new Representer(dumperOptions);
		NoTimestampResolver resolver = new NoTimestampResolver();
		return new Yaml(constructor, representer, dumperOptions, loaderOptions, resolver);
	}

	/**
     * Loads the YAML file and returns a list of flattened maps.
     * 
     * @return a list of maps containing the flattened YAML data
     */
    List<Map<String, Object>> load() {
		List<Map<String, Object>> result = new ArrayList<>();
		process((properties, map) -> result.add(getFlattenedMap(map)));
		return result;
	}

	/**
	 * {@link Constructor} that tracks property origins.
	 */
	private class OriginTrackingConstructor extends SafeConstructor {

		/**
         * Constructs a new OriginTrackingConstructor object with the specified loading configuration.
         * 
         * @param loadingConfig the loading configuration for the OriginTrackingConstructor
         */
        OriginTrackingConstructor(LoaderOptions loadingConfig) {
			super(loadingConfig);
		}

		/**
         * Retrieves the data from the superclass and checks if it is an instance of CharSequence.
         * If it is, it checks if the CharSequence is empty and returns null if it is.
         * Otherwise, it returns the data.
         *
         * @return the retrieved data, or null if it is an empty CharSequence
         * @throws NoSuchElementException if the data cannot be retrieved
         */
        @Override
		public Object getData() throws NoSuchElementException {
			Object data = super.getData();
			if (data instanceof CharSequence charSequence && charSequence.isEmpty()) {
				return null;
			}
			return data;
		}

		/**
         * Constructs an object based on the given node.
         * 
         * @param node the node to construct the object from
         * @return the constructed object
         */
        @Override
		protected Object constructObject(Node node) {
			if (node instanceof CollectionNode && ((CollectionNode<?>) node).getValue().isEmpty()) {
				return constructTrackedObject(node, super.constructObject(node));
			}
			if (node instanceof ScalarNode) {
				if (!(node instanceof KeyScalarNode)) {
					return constructTrackedObject(node, super.constructObject(node));
				}
			}
			if (node instanceof MappingNode mappingNode) {
				replaceMappingNodeKeys(mappingNode);
			}
			return super.constructObject(node);
		}

		/**
         * Replaces the keys of the given MappingNode with the corresponding values.
         * 
         * @param node the MappingNode whose keys need to be replaced
         */
        private void replaceMappingNodeKeys(MappingNode node) {
			List<NodeTuple> newValue = new ArrayList<>();
			node.getValue().stream().map(KeyScalarNode::get).forEach(newValue::add);
			node.setValue(newValue);
		}

		/**
         * Constructs a tracked object with the given node and value.
         * 
         * @param node the node associated with the tracked object
         * @param value the value of the tracked object
         * @return the constructed tracked object
         */
        private Object constructTrackedObject(Node node, Object value) {
			Origin origin = getOrigin(node);
			return OriginTrackedValue.of(getValue(value), origin);
		}

		/**
         * Returns the value if it is not null, otherwise returns an empty string.
         *
         * @param value the value to be checked
         * @return the value if not null, otherwise an empty string
         */
        private Object getValue(Object value) {
			return (value != null) ? value : "";
		}

		/**
         * Returns the origin of the given node.
         * 
         * @param node the node for which to get the origin
         * @return the origin of the node
         */
        private Origin getOrigin(Node node) {
			Mark mark = node.getStartMark();
			Location location = new Location(mark.getLine(), mark.getColumn());
			return new TextResourceOrigin(OriginTrackedYamlLoader.this.resource, location);
		}

	}

	/**
	 * {@link ScalarNode} that replaces the key node in a {@link NodeTuple}.
	 */
	private static class KeyScalarNode extends ScalarNode {

		/**
         * Constructs a new KeyScalarNode object by copying the properties of the provided ScalarNode.
         * 
         * @param node the ScalarNode to be copied
         */
        KeyScalarNode(ScalarNode node) {
			super(node.getTag(), node.getValue(), node.getStartMark(), node.getEndMark(), node.getScalarStyle());
		}

		/**
         * Returns a new NodeTuple with the key node converted to a KeyScalarNode.
         * 
         * @param nodeTuple the original NodeTuple
         * @return a new NodeTuple with the key node converted to a KeyScalarNode
         */
        static NodeTuple get(NodeTuple nodeTuple) {
			Node keyNode = nodeTuple.getKeyNode();
			Node valueNode = nodeTuple.getValueNode();
			return new NodeTuple(KeyScalarNode.get(keyNode), valueNode);
		}

		/**
         * Returns a new Node object based on the given Node.
         * If the given Node is an instance of ScalarNode, it creates a new KeyScalarNode object based on it.
         * Otherwise, it returns the given Node as is.
         *
         * @param node the Node object to be processed
         * @return a new Node object based on the given Node
         */
        private static Node get(Node node) {
			if (node instanceof ScalarNode scalarNode) {
				return new KeyScalarNode(scalarNode);
			}
			return node;
		}

	}

	/**
	 * {@link Resolver} that limits {@link Tag#TIMESTAMP} tags.
	 */
	private static final class NoTimestampResolver extends Resolver {

		/**
         * Adds an implicit resolver for a specific tag with a given regular expression pattern, first match, and limit.
         * 
         * @param tag     the tag to add the implicit resolver for
         * @param regexp  the regular expression pattern to match
         * @param first   the first match to consider
         * @param limit   the maximum number of matches to consider
         */
        @Override
		public void addImplicitResolver(Tag tag, Pattern regexp, String first, int limit) {
			if (tag == Tag.TIMESTAMP) {
				return;
			}
			super.addImplicitResolver(tag, regexp, first, limit);
		}

	}

}
