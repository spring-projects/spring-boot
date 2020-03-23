/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.layer.CustomLayers;
import org.springframework.boot.loader.tools.layer.application.FilteredResourceStrategy;
import org.springframework.boot.loader.tools.layer.application.LocationFilter;
import org.springframework.boot.loader.tools.layer.application.ResourceFilter;
import org.springframework.boot.loader.tools.layer.application.ResourceStrategy;
import org.springframework.boot.loader.tools.layer.library.CoordinateFilter;
import org.springframework.boot.loader.tools.layer.library.FilteredLibraryStrategy;
import org.springframework.boot.loader.tools.layer.library.LibraryFilter;
import org.springframework.boot.loader.tools.layer.library.LibraryStrategy;

/**
 * Produces a {@link CustomLayers} based on the given {@link Document}.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class CustomLayersProvider {

	public CustomLayers getLayers(Document document) {
		Element root = document.getDocumentElement();
		NodeList nl = root.getChildNodes();
		List<Layer> layers = new ArrayList<>();
		List<LibraryStrategy> libraryStrategies = new ArrayList<>();
		List<ResourceStrategy> resourceStrategies = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				processNode(layers, libraryStrategies, resourceStrategies, (Element) node);
			}
		}
		return new CustomLayers(layers, resourceStrategies, libraryStrategies);
	}

	private void processNode(List<Layer> layers, List<LibraryStrategy> libraryStrategies,
			List<ResourceStrategy> resourceStrategies, Element node) {
		String nodeName = node.getNodeName();
		if ("layers".equals(nodeName)) {
			layers.addAll(getLayers(node));
		}
		NodeList contents = node.getChildNodes();
		if ("libraries".equals(nodeName)) {
			libraryStrategies.addAll(getStrategies(contents,
					(StrategyFactory<LibraryFilter, LibraryStrategy>) FilteredLibraryStrategy::new,
					CoordinateFilter::new, "coordinates"::equals));
		}
		if ("application".equals(nodeName)) {
			resourceStrategies.addAll(getStrategies(contents,
					(StrategyFactory<ResourceFilter, ResourceStrategy>) FilteredResourceStrategy::new,
					LocationFilter::new, "locations"::equals));
		}
	}

	private List<Layer> getLayers(Element element) {
		List<Layer> layers = new ArrayList<>();
		NodeList nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element) {
				Element ele = (Element) node;
				String nodeName = ele.getNodeName();
				if ("layer".equals(nodeName)) {
					layers.add(new Layer(ele.getTextContent()));
				}
			}
		}
		return layers;
	}

	private <T, E> List<T> getStrategies(NodeList nodes, StrategyFactory<E, T> strategyFactory,
			FilterFactory<E> filterFactory, Predicate<String> filterPredicate) {
		List<T> contents = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node item = nodes.item(i);
			if (item instanceof Element) {
				Element element = (Element) item;
				if ("layer-content".equals(element.getTagName())) {
					NodeList filterList = item.getChildNodes();
					if (filterList.getLength() == 0) {
						throw new IllegalArgumentException("Filters for layer-content must not be empty.");
					}
					List<E> filters = new ArrayList<>();
					for (int j = 0; j < filterList.getLength(); j++) {
						Node filterNode = filterList.item(j);
						if (filterNode instanceof Element) {
							List<String> includeList = getPatterns((Element) filterNode, "include");
							List<String> excludeList = getPatterns((Element) filterNode, "exclude");
							if (filterPredicate.test(filterNode.getNodeName())) {
								E filter = filterFactory.getFilter(includeList, excludeList);
								filters.add(filter);
							}
						}
					}
					String layer = element.getAttribute("layer");
					contents.add(strategyFactory.getStrategy(layer, filters));
				}
			}
		}
		return contents;
	}

	private List<String> getPatterns(Element element, String key) {
		NodeList patterns = element.getElementsByTagName(key);
		List<String> values = new ArrayList<>();
		for (int j = 0; j < patterns.getLength(); j++) {
			Node item = patterns.item(j);
			if (item instanceof Element) {
				values.add(item.getTextContent());
			}
		}
		return values;
	}

	interface StrategyFactory<E, T> {

		T getStrategy(String layer, List<E> filters);

	}

	interface FilterFactory<E> {

		E getFilter(List<String> includes, List<String> excludes);

	}

}
