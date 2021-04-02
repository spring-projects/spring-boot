package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.core.util.Throwables;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SpringBootXmlConfiguration extends AbstractConfiguration implements Reconfigurable {
	private LoggingInitializationContext initializationContext;
	private Environment environment;
	/**
	 * Constructor.
	 *
	 * @param loggerContext
	 * @param configurationSource
	 */
	private static final String XINCLUDE_FIXUP_LANGUAGE =
			"http://apache.org/xml/features/xinclude/fixup-language";
	private static final String XINCLUDE_FIXUP_BASE_URIS =
			"http://apache.org/xml/features/xinclude/fixup-base-uris";
	private static final String[] VERBOSE_CLASSES = new String[] {ResolverUtil.class.getName()};
	private static final String LOG4J_XSD = "Log4j-config.xsd";

	private static final String SPRING_PROFILE_TAG_NAME = "SpringProfile";

	private final List<Status> status = new ArrayList<>();
	private Element rootElement;
	private boolean strict;
	private String schemaResource;

	/**
	 * Add a initializationContext construct parameter
	 */
	public SpringBootXmlConfiguration(final LoggingInitializationContext initializationContext, final LoggerContext loggerContext, final ConfigurationSource configSource) {
		super(loggerContext, configSource);
		this.initializationContext = initializationContext;
		this.environment = initializationContext.getEnvironment();
		final File configFile = configSource.getFile();
		byte[] buffer = null;

		try {
			final InputStream configStream = configSource.getInputStream();
			try {
				buffer = toByteArray(configStream);
			} finally {
				Closer.closeSilently(configStream);
			}
			final InputSource source = new InputSource(new ByteArrayInputStream(buffer));
			source.setSystemId(configSource.getLocation());
			final DocumentBuilder documentBuilder = newDocumentBuilder(true);
			Document document;
			try {
				document = documentBuilder.parse(source);
			} catch (final Exception e) {
				// LOG4J2-1127
				final Throwable throwable = Throwables.getRootCause(e);
				if (throwable instanceof UnsupportedOperationException) {
					LOGGER.warn(
							"The DocumentBuilder {} does not support an operation: {}."
									+ "Trying again without XInclude...",
							documentBuilder, e);
					document = newDocumentBuilder(false).parse(source);
				} else {
					throw e;
				}
			}
			rootElement = document.getDocumentElement();
			final Map<String, String> attrs = processAttributes(rootNode, rootElement);
			final StatusConfiguration statusConfig = new StatusConfiguration().withVerboseClasses(VERBOSE_CLASSES)
					.withStatus(getDefaultStatus());
			int monitorIntervalSeconds = 0;
			for (final Map.Entry<String, String> entry : attrs.entrySet()) {
				final String key = entry.getKey();
				final String value = getStrSubstitutor().replace(entry.getValue());
				if ("status".equalsIgnoreCase(key)) {
					statusConfig.withStatus(value);
				} else if ("dest".equalsIgnoreCase(key)) {
					statusConfig.withDestination(value);
				} else if ("shutdownHook".equalsIgnoreCase(key)) {
					isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
				} else if ("shutdownTimeout".equalsIgnoreCase(key)) {
					shutdownTimeoutMillis = Long.parseLong(value);
				} else if ("verbose".equalsIgnoreCase(key)) {
					statusConfig.withVerbosity(value);
				} else if ("packages".equalsIgnoreCase(key)) {
					pluginPackages.addAll(Arrays.asList(value.split(Patterns.COMMA_SEPARATOR)));
				} else if ("name".equalsIgnoreCase(key)) {
					setName(value);
				} else if ("strict".equalsIgnoreCase(key)) {
					strict = Boolean.parseBoolean(value);
				} else if ("schema".equalsIgnoreCase(key)) {
					schemaResource = value;
				} else if ("monitorInterval".equalsIgnoreCase(key)) {
					monitorIntervalSeconds = Integer.parseInt(value);
				} else if ("advertiser".equalsIgnoreCase(key)) {
					createAdvertiser(value, configSource, buffer, "text/xml");
				}
			}
			initializeWatchers(this, configSource, monitorIntervalSeconds);
			statusConfig.initialize();
		} catch (final SAXException | IOException | ParserConfigurationException e) {
			LOGGER.error("Error parsing " + configSource.getLocation(), e);
		}
		if (strict && schemaResource != null && buffer != null) {
			try (InputStream is = Loader.getResourceAsStream(schemaResource, SpringBootXmlConfiguration.class.getClassLoader())) {
				if (is != null) {
					final javax.xml.transform.Source src = new StreamSource(is, LOG4J_XSD);
					final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
					Schema schema = null;
					try {
						schema = factory.newSchema(src);
					} catch (final SAXException ex) {
						LOGGER.error("Error parsing Log4j schema", ex);
					}
					if (schema != null) {
						final Validator validator = schema.newValidator();
						try {
							validator.validate(new StreamSource(new ByteArrayInputStream(buffer)));
						} catch (final IOException ioe) {
							LOGGER.error("Error reading configuration for validation", ioe);
						} catch (final SAXException ex) {
							LOGGER.error("Error validating configuration", ex);
						}
					}
				}
			} catch (final Exception ex) {
				LOGGER.error("Unable to access schema {}", this.schemaResource, ex);
			}
		}

		if (getName() == null) {
			setName(configSource.getLocation());
		}
	}

	/**
	 * Creates a new DocumentBuilder suitable for parsing a configuration file.
	 *
	 * @param xIncludeAware enabled XInclude
	 * @return a new DocumentBuilder
	 * @throws ParserConfigurationException
	 */
	static DocumentBuilder newDocumentBuilder(final boolean xIncludeAware) throws ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		disableDtdProcessing(factory);

		if (xIncludeAware) {
			enableXInclude(factory);
		}
		return factory.newDocumentBuilder();
	}

	private static void disableDtdProcessing(final DocumentBuilderFactory factory) {
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
		setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
		setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	}

	private static void setFeature(final DocumentBuilderFactory factory, final String featureName, final boolean value) {
		try {
			factory.setFeature(featureName, value);
		} catch (Exception | LinkageError e) {
			getStatusLogger().error("Caught {} setting feature {} to {} on DocumentBuilderFactory {}: {}",
					e.getClass().getCanonicalName(), featureName, value, factory, e, e);
		}
	}

	/**
	 * Enables XInclude for the given DocumentBuilderFactory
	 *
	 * @param factory a DocumentBuilderFactory
	 */
	private static void enableXInclude(final DocumentBuilderFactory factory) {
		try {
			// Alternative: We set if a system property on the command line is set, for example:
			// -DLog4j.XInclude=true
			factory.setXIncludeAware(true);
		} catch (final UnsupportedOperationException e) {
			LOGGER.warn("The DocumentBuilderFactory [{}] does not support XInclude: {}", factory, e);
		} catch (@SuppressWarnings("ErrorNotRethrown") final AbstractMethodError | NoSuchMethodError err) {
			LOGGER.warn("The DocumentBuilderFactory [{}] is out of date and does not support XInclude: {}", factory,
					err);
		}
		try {
			// Alternative: We could specify all features and values with system properties like:
			// -DLog4j.DocumentBuilderFactory.Feature="http://apache.org/xml/features/xinclude/fixup-base-uris true"
			factory.setFeature(XINCLUDE_FIXUP_BASE_URIS, true);
		} catch (final ParserConfigurationException e) {
			LOGGER.warn("The DocumentBuilderFactory [{}] does not support the feature [{}]: {}", factory,
					XINCLUDE_FIXUP_BASE_URIS, e);
		} catch (@SuppressWarnings("ErrorNotRethrown") final AbstractMethodError err) {
			LOGGER.warn("The DocumentBuilderFactory [{}] is out of date and does not support setFeature: {}", factory,
					err);
		}
		try {
			factory.setFeature(XINCLUDE_FIXUP_LANGUAGE, true);
		} catch (final ParserConfigurationException e) {
			LOGGER.warn("The DocumentBuilderFactory [{}] does not support the feature [{}]: {}", factory,
					XINCLUDE_FIXUP_LANGUAGE, e);
		} catch (@SuppressWarnings("ErrorNotRethrown") final AbstractMethodError err) {
			LOGGER.warn("The DocumentBuilderFactory [{}] is out of date and does not support setFeature: {}", factory,
					err);
		}
	}

	@Override
	public void setup() {
		if (rootElement == null) {
			LOGGER.error("No logging configuration");
			return;
		}
		constructHierarchy(rootNode, rootElement,false);
		if (status.size() > 0) {
			for (final Status s : status) {
				LOGGER.error("Error processing element {} ({}): {}", s.name, s.element, s.errorType);
			}
			return;
		}
		rootElement = null;
	}

	@Override
	public Configuration reconfigure() {
		try {
			final ConfigurationSource source = getConfigurationSource().resetInputStream();
			if (source == null) {
				return null;
			}
			final SpringBootXmlConfiguration config = new SpringBootXmlConfiguration(initializationContext,getLoggerContext(), source);
			return config.rootElement == null ? null : config;
		} catch (final IOException ex) {
			LOGGER.error("Cannot locate file {}", getConfigurationSource(), ex);
		}
		return null;
	}

	private void constructHierarchy(final Node node, final Element element, boolean profileNode) {
		if(!profileNode){
			processAttributes(node,element);
		}
		final StringBuilder buffer = new StringBuilder();
		final NodeList list = element.getChildNodes();
		final List<Node> children = node.getChildren();
		for (int i = 0; i < list.getLength(); i++) {
			final org.w3c.dom.Node w3cNode = list.item(i);
			if (w3cNode instanceof Element) {
				final Element child = (Element) w3cNode;

				final String name = getType(child);

				// Enhance log4j2.xml configuration
				if(SPRING_PROFILE_TAG_NAME.equalsIgnoreCase(name)){
					if(acceptsProfiles(child.getAttribute("name"))){
						constructHierarchy(node, child, true);
					}
					// Break <SpringProfile> node
					continue;
				}
				final PluginType<?> type = pluginManager.getPluginType(name);
				final Node childNode = new Node(node, name, type);
				constructHierarchy(childNode, child, false);
				if (type == null) {
					final String value = childNode.getValue();
					if (!childNode.hasChildren() && value != null) {
						node.getAttributes().put(name, value);
					} else {
						status.add(new Status(name, element, ErrorType.CLASS_NOT_FOUND));
					}
				} else {
					children.add(childNode);
				}
			} else if (w3cNode instanceof Text) {
				final Text data = (Text) w3cNode;
				buffer.append(data.getData());
			}
		}

		final String text = buffer.toString().trim();
		if (text.length() > 0 || (!node.hasChildren() && !node.isRoot())) {
			node.setValue(text);
		}
	}

	private boolean acceptsProfiles(String profile) {
		if (this.environment == null) {
			return false;
		}
		String[] profileNames = StringUtils
				.trimArrayElements(StringUtils.commaDelimitedListToStringArray(profile));
		if (profileNames.length == 0) {
			return false;
		}
		return this.environment.acceptsProfiles(Profiles.of(profileNames));
	}

	private String getType(final Element element) {
		if (strict) {
			final NamedNodeMap attrs = element.getAttributes();
			for (int i = 0; i < attrs.getLength(); ++i) {
				final org.w3c.dom.Node w3cNode = attrs.item(i);
				if (w3cNode instanceof Attr) {
					final Attr attr = (Attr) w3cNode;
					if (attr.getName().equalsIgnoreCase("type")) {
						final String type = attr.getValue();
						attrs.removeNamedItem(attr.getName());
						return type;
					}
				}
			}
		}
		return element.getTagName();
	}

	private Map<String, String> processAttributes(final Node node, final Element element) {
		final NamedNodeMap attrs = element.getAttributes();
		final Map<String, String> attributes = node.getAttributes();

		for (int i = 0; i < attrs.getLength(); ++i) {
			final org.w3c.dom.Node w3cNode = attrs.item(i);
			if (w3cNode instanceof Attr) {
				final Attr attr = (Attr) w3cNode;
				if (attr.getName().equals("xml:base")) {
					continue;
				}
				attributes.put(attr.getName(), attr.getValue());
			}
		}
		return attributes;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[location=" + getConfigurationSource() + "]";
	}

	/**
	 * The error that occurred.
	 */
	private enum ErrorType {
		CLASS_NOT_FOUND
	}

	/**
	 * Status for recording errors.
	 */
	private static class Status {
		private final Element element;
		private final String name;
		private final ErrorType errorType;

		public Status(final String name, final Element element, final ErrorType errorType) {
			this.name = name;
			this.element = element;
			this.errorType = errorType;
		}

		@Override
		public String toString() {
			return "Status [name=" + name + ", element=" + element + ", errorType=" + errorType + "]";
		}

	}

}