package org.springframework.boot.cli.util;

import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Look up property-based settings for Spring Boot CLI.
 *
 * @author Greg Turnquist
 */
public class SpringBootCliPropertyMapper {

	// Base name of any CLI-based property
	public static final String SPRING_BOOT_CLI = "spring.boot.cli.";

	// Internal collection of properties
	private Properties properties;

	/**
	 * Look up annotations associated with a module
	 *
	 * @param module - name of module to find
	 * @return array of annotations, any one which will activate a given module
	 */
	public String[] getAnnotations(String module) {
		return parseProperty(module, "annotations");
	}

	/**
	 * Look up field or method types associated with a module
	 *
	 * @param module - name of module to find
	 * @return array of types, any one which will activate a given module
	 */
	public String[] getFields(String module) {
		String[] fields = parseProperty(module, "fields");
		for (String field : fields) {
			Log.info("Parsed field: " + field);
		}
		return fields;
	}

	/**
	 * Look up set of dependencies to add to the classpath when a module is activated
	 *
	 * @param module - name of module to find
	 * @return array of dependencies to add to the classpath when a module is activated
	 */
	public String[] getDependencies(String module) {
		String[] dependencies = parseProperty(module, "dependencies");
		for (String dependency : dependencies) {
			Log.info("Parsed dependency: " + dependency);
		}
		return dependencies;
	}

	/**
	 * Look up paths to "star import" into a CLI app. Essentially,
	 * "import [star import].*"
	 * ...is the net effect
	 *
	 * @param module - name of module to find
	 * @return array of paths to "import [path].*"
	 */
	public String[] getStarImports(String module) {
		String[] starImports = parseProperty(module, "starImports");
		for (String starImport : starImports) {
			Log.info("Parsed starImport: " + starImport);
		}
		return starImports;
	}

	/**
	 * Scan for all "mapping*.properties" files on the classpath, and load them into a single {@link java.util.Properties}.
	 *
	 * @return {@link java.util.Properties} containing the entire collection of Spring Boot CLI properties.
	 */
	private Properties getProperties() {
		if (this.properties == null) {
			this.properties = new Properties();
			List<String> urls = ResourceUtils.getUrls("classpath*:mappings**.properties", this.getClass().getClassLoader());
			for (String url : urls) {
				try {
					this.properties.load(new URL(url).openStream());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return this.properties;
	}

	/**
	 * Parse a given property into a {@link String[]} array.
	 *
	 * @param module - name of module to find
	 * @param segment - submodule, like "annotations", etc.
	 * @return {@link String[]} of the property
	 */
	private String[] parseProperty(String module, String segment) {
		final String property = getProperties().getProperty(SPRING_BOOT_CLI + module + "." + segment);
		if (property != null) {
			return property.split(",");
		}
		return new String[0];
	}

}
