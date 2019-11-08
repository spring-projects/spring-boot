import groovy.io.FileType

import java.util.Properties

import org.springframework.core.io.InputStreamResource
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.ClassMetadata
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils

class Project {

	final List<File> classFiles

	final Properties springFactories

	Project(File rootDirectory) {
		this.springFactories = loadSpringFactories(rootDirectory)
		this.classFiles = []
		rootDirectory.eachFileRecurse (FileType.FILES) { file ->
			if (file.name.endsWith('.class')) {
				classFiles << file
			}
		}
	}

	private static Properties loadSpringFactories(File rootDirectory) {
		Properties springFactories = new Properties()
		new File(rootDirectory, 'META-INF/spring.factories').withInputStream { inputStream ->
			springFactories.load(inputStream)
		}
		return springFactories
	}
}

class TestSlice {

	final String name

	final SortedSet<String> importedAutoConfiguration

	TestSlice(String annotationName, Collection<String> importedAutoConfiguration) {
		this.name = ClassUtils.getShortName(annotationName)
		this.importedAutoConfiguration = new TreeSet<String>(importedAutoConfiguration)
	}
}

List<TestSlice> createTestSlices(Project project) {
	MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory()
	project.classFiles
		.findAll { classFile ->
			classFile.name.endsWith('Test.class')
		}.collect { classFile ->
			createMetadataReader(metadataReaderFactory, classFile)
		}.findAll { metadataReader ->
			metadataReader.classMetadata.annotation
		}.collect { metadataReader ->
			createTestSlice(project.springFactories, metadataReader.classMetadata, metadataReader.annotationMetadata)
		}.sort {
			a, b -> a.name.compareTo b.name
		}
}

MetadataReader createMetadataReader(MetadataReaderFactory factory, File classFile) {
	classFile.withInputStream { inputStream ->
		factory.getMetadataReader(new InputStreamResource(inputStream))
	}
}

TestSlice createTestSlice(Properties springFactories, ClassMetadata classMetadata, AnnotationMetadata annotationMetadata) {
	new TestSlice(classMetadata.className, getImportedAutoConfiguration(springFactories, annotationMetadata))
}

Set<String> getImportedAutoConfiguration(Properties springFactories, AnnotationMetadata annotationMetadata) {
	Set<String> importers = findMetaImporters(annotationMetadata)
	if (annotationMetadata.isAnnotated('org.springframework.boot.autoconfigure.ImportAutoConfiguration')) {
		importers.add(annotationMetadata.className)
	}
	importers
		.collect { autoConfigurationImporter ->
			StringUtils.commaDelimitedListToSet(springFactories.get(autoConfigurationImporter))
		}.flatten()
}

Set<String> findMetaImporters(AnnotationMetadata annotationMetadata) {
	annotationMetadata.annotationTypes
		.findAll { annotationType ->
			isAutoConfigurationImporter(annotationType, annotationMetadata)
		}
}

boolean isAutoConfigurationImporter(String annotationType, AnnotationMetadata metadata) {
	metadata.getMetaAnnotationTypes(annotationType).contains('org.springframework.boot.autoconfigure.ImportAutoConfiguration')
}

void writeTestSlicesTable(List<TestSlice> testSlices) {
	new File(project.build.directory, "generated-resources/test-slice-auto-configuration.adoc").withPrintWriter { writer ->
		writer.println '[cols="d,a"]'
		writer.println '|==='
		writer.println '| Test slice | Imported auto-configuration'
		testSlices.each { testSlice ->
			writer.println ''
			writer.println "| `@${testSlice.name}`"
			writer.print '| '
			testSlice.importedAutoConfiguration.each {
				writer.println "`${it}`"
			}
		}
		writer.println '|==='
	}
}

List<TestSlice> testSlices = createTestSlices(new Project(new File(project.build.directory, 'test-auto-config')))
writeTestSlicesTable(testSlices)
