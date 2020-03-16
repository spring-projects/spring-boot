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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.layer.CustomLayers;
import org.springframework.boot.loader.tools.layer.application.FilteredResourceStrategy;
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
				Element ele = (Element) node;
				String nodeName = ele.getNodeName();
				if ("layers".equals(nodeName)) {
					layers.addAll(getLayers(ele));
				}
				if ("libraries".equals(nodeName)) {
					libraryStrategies.addAll(getLibraryStrategies(ele.getChildNodes()));
				}
				if ("classes".equals(nodeName)) {
					resourceStrategies.addAll(getResourceStrategies(ele.getChildNodes()));
				}
			}
		}
		return new CustomLayers(layers, resourceStrategies, libraryStrategies);
	}

	private List<LibraryStrategy> getLibraryStrategies(NodeList nodes) {
		List<LibraryStrategy> strategy = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node item = nodes.item(i);
			if (item instanceof Element) {
				Element element = (Element) item;
				String layer = element.getAttribute("layer");
				if ("layer-content".equals(element.getTagName())) {
					List<LibraryFilter> filters = new ArrayList<>();
					NodeList filterList = item.getChildNodes();
					if (filterList.getLength() == 0) {
						throw new IllegalArgumentException("Filters for layer-content must not be empty.");
					}
					for (int k = 0; k < filterList.getLength(); k++) {
						Node filter = filterList.item(k);
						if (filter instanceof Element) {
							List<String> includeList = getPatterns((Element) filter, "include");
							List<String> excludeList = getPatterns((Element) filter, "exclude");
							addLibraryFilter(filters, filter, includeList, excludeList);
						}
					}
					strategy.add(new FilteredLibraryStrategy(layer, filters));
				}
			}
		}
		return strategy;
	}

	private void addLibraryFilter(List<LibraryFilter> filters, Node filter, List<String> includeList,
			List<String> excludeList) {
		if ("coordinates".equals(filter.getNodeName())) {
			filters.add(new CoordinateFilter(includeList, excludeList));
		}
	}

	private List<ResourceStrategy> getResourceStrategies(NodeList strategies) {
		List<ResourceStrategy> strategy = new ArrayList<>();
		for (int i = 0; i < strategies.getLength(); i++) {
			Node item = strategies.item(i);
			List<ResourceFilter> filters = new ArrayList<>();
			if (item instanceof Element) {
				Element element = (Element) item;
				String layer = element.getAttribute("layer");
				if ("layer-content".equals(element.getTagName())) {
					NodeList filterList = item.getChildNodes();
					if (filterList.getLength() == 0) {
						throw new IllegalArgumentException("Filters for layer-content must not be empty.");
					}
					for (int k = 0; k < filterList.getLength(); k++) {
						Node filter = filterList.item(k);
						if (filter instanceof Element) {
							List<String> includeList = getPatterns((Element) filter, "include");
							List<String> excludeList = getPatterns((Element) filter, "exclude");
							addFilter(filters, filter, includeList, excludeList);
						}
					}
					strategy.add(new FilteredResourceStrategy(layer, filters));
				}
			}
		}
		return strategy;
	}

	private void addFilter(List<ResourceFilter> filters, Node filter, List<String> includeList,
			List<String> excludeList) {
		if ("locations".equals(filter.getNodeName())) {
			filters.add(
					new org.springframework.boot.loader.tools.layer.application.LocationFilter(includeList, excludeList));
		}
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

	private List<Layer> getLayers(Element element) {
		List<Layer> layers = new ArrayList<>();
		NodeList nl = element.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
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

}
