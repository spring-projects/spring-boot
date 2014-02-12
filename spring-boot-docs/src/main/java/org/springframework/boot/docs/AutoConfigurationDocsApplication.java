package org.springframework.boot.docs;

import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * Simple generator for a Markdown table of autoconfig classes and links to
 * documentation. Prints report out on stdout.
 * 
 * @author Dave Syer
 * 
 */
public class AutoConfigurationDocsApplication {

	public static void main(String args[]) throws Throwable {
		AutoConfigurationDocumentationClient autoConfigurationDocumentationClient = new AutoConfigurationDocumentationClient();
		autoConfigurationDocumentationClient.discoverConfigurationClasses();
	}

}

class AutoConfigurationDocumentationClient {

	private final static String SPRING_FACTORIES_PATH_EXPRESSION = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
			+ "META-INF/spring.factories";

	private final static String AUTO_CONFIGURATION_KEY = EnableAutoConfiguration.class
			.getName();

	private Log logger = LogFactory.getLog(getClass());

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private String processTopLevelConfigurationClass(String location, String name) {
		logger.debug("Processing: " + name);
		String simpleName = StringUtils.getFilenameExtension(name);
		String project = extractProject(location);
		String projectPath = "https://github.com/spring-projects/spring-boot/tree/master/"
				+ project;
		String classPath = projectPath + "/src/main/java/" + name.replace(".", "/")
				+ ".java";
		String javadoc = "http://docs.spring.io/spring-boot/docs/1.0.0.RC1/api/"
				+ name.replace(".", "/") + ".html";
		return String.format("| [%s](%s) | [javadoc](%s) | [%s](%s) |", simpleName,
				classPath, javadoc, project, projectPath);
	}

	private String extractProject(String location) {
		if (location.startsWith("file:")) {
			String result = location.substring(0, location.indexOf("/target"));
			result = result.replaceAll(".*/", "");
			return result;
		}
		String result = location.substring(0, location.indexOf(".jar!"));
		result = result.replaceAll(".*/", "");
		result = result.replaceAll("-[0-9].*", "");
		return result;
	}

	public void discoverConfigurationClasses() throws Exception {
		StringBuilder builder = new StringBuilder(
				"| Configuration Class | Links | Project |\n|---|---|---|\n");
		Resource[] resources = this.resourcePatternResolver
				.getResources(SPRING_FACTORIES_PATH_EXPRESSION);
		for (Resource r : resources) {
			try {
				InputStream inputStream = r.getInputStream();
				Properties properties = new Properties();
				properties.load(inputStream);
				if (properties.containsKey(AUTO_CONFIGURATION_KEY)) {
					String rawValue = (String) properties.get(AUTO_CONFIGURATION_KEY);
					Set<String> clazzNames = StringUtils
							.commaDelimitedListToSet(rawValue);
					for (String name : clazzNames) {
						builder.append(
								processTopLevelConfigurationClass(r.getURI().toString(),
										name)).append("\n");
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		System.out.println(builder);
	}

}