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

package org.springframework.boot.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.layer.ApplicationContentFilter;
import org.springframework.boot.loader.tools.layer.ContentFilter;
import org.springframework.boot.loader.tools.layer.ContentSelector;
import org.springframework.boot.loader.tools.layer.CustomLayers;
import org.springframework.boot.loader.tools.layer.IncludeExcludeContentSelector;
import org.springframework.boot.loader.tools.layer.LibraryContentFilter;

/**
 * Produces a {@link CustomLayers} based on the given {@link Document}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class CustomLayersProvider {

	/**
	 * Retrieves the layers, application selectors, and library selectors from the given
	 * document.
	 * @param document the document to retrieve the layers from
	 * @return a CustomLayers object containing the layers, application selectors, and
	 * library selectors
	 * @throws IllegalArgumentException if the document is invalid
	 */
	CustomLayers getLayers(Document document) {
		validate(document);
		Element root = document.getDocumentElement();
		List<ContentSelector<String>> applicationSelectors = getApplicationSelectors(root);
		List<ContentSelector<Library>> librarySelectors = getLibrarySelectors(root);
		List<Layer> layers = getLayers(root);
		return new CustomLayers(layers, applicationSelectors, librarySelectors);
	}

	/**
	 * Validates the given XML document against a loaded schema.
	 * @param document the XML document to be validated
	 * @throws IllegalStateException if the document is invalid
	 */
	private void validate(Document document) {
		Schema schema = loadSchema();
		try {
			Validator validator = schema.newValidator();
			validator.validate(new DOMSource(document));
		}
		catch (SAXException | IOException ex) {
			throw new IllegalStateException("Invalid layers.xml configuration", ex);
		}
	}

	/**
	 * Loads the schema for the custom layers.
	 * @return The loaded schema.
	 * @throws IllegalStateException If unable to load the layers XSD.
	 */
	private Schema loadSchema() {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			return factory.newSchema(getClass().getResource("layers.xsd"));
		}
		catch (SAXException ex) {
			throw new IllegalStateException("Unable to load layers XSD");
		}
	}

	/**
	 * Retrieves a list of application selectors based on the given root element.
	 * @param root the root element to search for application selectors
	 * @return a list of application selectors
	 */
	private List<ContentSelector<String>> getApplicationSelectors(Element root) {
		return getSelectors(root, "application", (element) -> getSelector(element, ApplicationContentFilter::new));
	}

	/**
	 * Retrieves a list of content selectors for libraries based on the given root
	 * element.
	 * @param root the root element to search for library selectors
	 * @return a list of content selectors for libraries
	 */
	private List<ContentSelector<Library>> getLibrarySelectors(Element root) {
		return getSelectors(root, "dependencies", (element) -> getLibrarySelector(element, LibraryContentFilter::new));
	}

	/**
	 * Retrieves the layers from the given root element.
	 * @param root The root element to retrieve the layers from.
	 * @return The list of layers retrieved from the root element.
	 */
	private List<Layer> getLayers(Element root) {
		Element layerOrder = getChildElement(root, "layerOrder");
		if (layerOrder == null) {
			return Collections.emptyList();
		}
		return getChildNodeTextContent(layerOrder, "layer").stream().map(Layer::new).toList();
	}

	/**
	 * Retrieves a list of content selectors based on the given root element, element
	 * name, and selector factory.
	 * @param root The root element to search for the specified element name.
	 * @param elementName The name of the element to search for within the root element.
	 * @param selectorFactory The factory function used to create content selectors based
	 * on child elements.
	 * @return A list of content selectors created from the child elements found within
	 * the specified root element and element name.
	 */
	private <T> List<ContentSelector<T>> getSelectors(Element root, String elementName,
			Function<Element, ContentSelector<T>> selectorFactory) {
		Element element = getChildElement(root, elementName);
		if (element == null) {
			return Collections.emptyList();
		}
		List<ContentSelector<T>> selectors = new ArrayList<>();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element childElement) {
				ContentSelector<T> selector = selectorFactory.apply(childElement);
				selectors.add(selector);
			}
		}
		return selectors;
	}

	/**
	 * Returns a ContentSelector object based on the provided element and filterFactory.
	 * @param element the element containing the layer information
	 * @param filterFactory the function used to create ContentFilter objects
	 * @return a ContentSelector object
	 * @param <T> the type of content to be selected
	 */
	private <T> ContentSelector<T> getSelector(Element element, Function<String, ContentFilter<T>> filterFactory) {
		Layer layer = new Layer(element.getAttribute("layer"));
		List<String> includes = getChildNodeTextContent(element, "include");
		List<String> excludes = getChildNodeTextContent(element, "exclude");
		return new IncludeExcludeContentSelector<>(layer, includes, excludes, filterFactory);
	}

	/**
	 * Returns a ContentSelector for selecting libraries based on the provided element and
	 * filter factory.
	 * @param element The element containing the configuration for the ContentSelector.
	 * @param filterFactory The factory function for creating ContentFilters based on
	 * string inputs.
	 * @return The ContentSelector for selecting libraries.
	 */
	private ContentSelector<Library> getLibrarySelector(Element element,
			Function<String, ContentFilter<Library>> filterFactory) {
		Layer layer = new Layer(element.getAttribute("layer"));
		List<String> includes = getChildNodeTextContent(element, "include");
		List<String> excludes = getChildNodeTextContent(element, "exclude");
		Element includeModuleDependencies = getChildElement(element, "includeModuleDependencies");
		Element excludeModuleDependencies = getChildElement(element, "excludeModuleDependencies");
		List<ContentFilter<Library>> includeFilters = includes.stream()
			.map(filterFactory)
			.collect(Collectors.toCollection(ArrayList::new));
		if (includeModuleDependencies != null) {
			includeFilters.add(Library::isLocal);
		}
		List<ContentFilter<Library>> excludeFilters = excludes.stream()
			.map(filterFactory)
			.collect(Collectors.toCollection(ArrayList::new));
		if (excludeModuleDependencies != null) {
			excludeFilters.add(Library::isLocal);
		}
		return new IncludeExcludeContentSelector<>(layer, includeFilters, excludeFilters);
	}

	/**
	 * Retrieves the text content of child nodes with the specified tag name from the
	 * given element.
	 * @param element the element from which to retrieve child nodes
	 * @param tagName the tag name of the child nodes to retrieve
	 * @return a list of text content of the child nodes with the specified tag name
	 */
	private List<String> getChildNodeTextContent(Element element, String tagName) {
		List<String> patterns = new ArrayList<>();
		NodeList nodes = element.getElementsByTagName(tagName);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element) {
				patterns.add(node.getTextContent());
			}
		}
		return patterns;
	}

	/**
	 * Returns the child element with the specified tag name from the given parent
	 * element.
	 * @param element the parent element from which to retrieve the child element
	 * @param tagName the tag name of the child element to retrieve
	 * @return the child element with the specified tag name, or null if not found
	 * @throws IllegalStateException if multiple child elements with the specified tag
	 * name are found
	 */
	private Element getChildElement(Element element, String tagName) {
		NodeList nodes = element.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return null;
		}
		if (nodes.getLength() > 1) {
			throw new IllegalStateException("Multiple '" + tagName + "' nodes found");
		}
		return (Element) nodes.item(0);
	}

}
