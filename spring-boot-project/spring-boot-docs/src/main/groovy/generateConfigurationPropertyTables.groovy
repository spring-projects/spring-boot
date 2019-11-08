import org.springframework.boot.configurationdocs.ConfigurationMetadataDocumentWriter
import org.springframework.boot.configurationdocs.DocumentOptions
import org.springframework.core.io.UrlResource

import java.nio.file.Path
import java.nio.file.Paths

def getConfigMetadataInputStreams() {
	def mainMetadata = getClass().getClassLoader().getResources("META-INF/spring-configuration-metadata.json")
	def additionalMetadata = getClass().getClassLoader().getResources("META-INF/additional-spring-configuration-metadata.json")
	def streams = []
	streams += mainMetadata.collect { new UrlResource(it).getInputStream() }
	streams += additionalMetadata.collect { new UrlResource(it).getInputStream() }
	return streams
}

def generateConfigMetadataDocumentation() {

	def streams = getConfigMetadataInputStreams()
	try {
		Path outputPath = Paths.get(project.build.directory, 'generated-resources', 'config-docs')
		def builder = DocumentOptions.builder();

		builder
				.addSection("core")
					.withKeyPrefixes("debug", "trace", "logging", "spring.aop", "spring.application",
						"spring.autoconfigure", "spring.banner", "spring.beaninfo", "spring.codec", "spring.config",
						"spring.info", "spring.jmx", "spring.main", "spring.messages", "spring.pid",
						"spring.profiles", "spring.quartz", "spring.reactor", "spring.task",
						"spring.mandatory-file-encoding", "info", "spring.output.ansi.enabled")
				.addSection("mail")
					.withKeyPrefixes("spring.mail", "spring.sendgrid")
				.addSection("cache")
					.withKeyPrefixes("spring.cache")
				.addSection("server")
					.withKeyPrefixes("server")
				.addSection("web")
					.withKeyPrefixes("spring.hateoas",
						"spring.http", "spring.servlet", "spring.jersey",
						"spring.mvc", "spring.resources", "spring.webflux")
				.addSection("json")
					.withKeyPrefixes("spring.jackson", "spring.gson")
				.addSection("rsocket")
					.withKeyPrefixes("spring.rsocket")
				.addSection("templating")
					.withKeyPrefixes("spring.freemarker", "spring.groovy", "spring.mustache", "spring.thymeleaf")
				.addOverride("spring.groovy.template.configuration", "See GroovyMarkupConfigurer")
				.addSection("security")
					.withKeyPrefixes("spring.security", "spring.ldap", "spring.session")
				.addSection("data-migration")
					.withKeyPrefixes("spring.flyway", "spring.liquibase")
				.addSection("data")
					.withKeyPrefixes("spring.couchbase", "spring.elasticsearch", "spring.h2",
						"spring.influx", "spring.mongodb", "spring.redis",
						"spring.dao", "spring.data", "spring.datasource", "spring.jooq",
						"spring.jdbc", "spring.jpa")
				.addOverride("spring.datasource.dbcp2", "Commons DBCP2 specific settings")
				.addOverride("spring.datasource.tomcat", "Tomcat datasource specific settings")
				.addOverride("spring.datasource.hikari", "Hikari specific settings")
				.addSection("transaction")
				.withKeyPrefixes("spring.jta", "spring.transaction")
				.addSection("integration")
					.withKeyPrefixes("spring.activemq", "spring.artemis", "spring.batch",
						"spring.integration", "spring.jms", "spring.kafka", "spring.rabbitmq", "spring.hazelcast",
						"spring.webservices")
				.addSection("actuator")
					.withKeyPrefixes("management")
				.addSection("devtools")
					.withKeyPrefixes("spring.devtools")
				.addSection("testing")
					.withKeyPrefixes("spring.test");

		ConfigurationMetadataDocumentWriter writer = new ConfigurationMetadataDocumentWriter();
		writer.writeDocument(outputPath, builder.build(), streams.toArray(new InputStream[0]));
	}
	finally {
		streams.each { it.close() }
	}
}

generateConfigMetadataDocumentation()