def processModule(File moduleDir, File generatedResourcesDir) {
	def moduleName = moduleDir.name
	def factoriesFile = new File(moduleDir, 'META-INF/spring.factories')
	new File(generatedResourcesDir, "auto-configuration-classes-${moduleName}.adoc")
			.withPrintWriter {
					generateAutoConfigurationClassTable(moduleName, factoriesFile, it)
	}
}

def generateAutoConfigurationClassTable(String module, File factories, PrintWriter writer) {
	writer.println '[cols="4,1"]'
	writer.println '|==='
	writer.println '| Configuration Class | Links'

	getAutoConfigurationClasses(factories).each {
		writer.println ''
		writer.println "| {spring-boot-code}/spring-boot-project/${module}/src/main/java/${it.path}.java[`${it.name}`]"
		writer.println "| {spring-boot-api}/${it.path}.html[javadoc]"
	}

	writer.println '|==='
}

def getAutoConfigurationClasses(File factories) {
	factories.withInputStream {
		def properties = new Properties()
		properties.load(it)
		properties.get('org.springframework.boot.autoconfigure.EnableAutoConfiguration')
				.split(',')
				.collect {
					def path = it.replace('.', '/')
					def name = it.substring(it.lastIndexOf('.') + 1)
					[ 'path': path, 'name': name]
				}
				.sort {a, b -> a.name.compareTo(b.name)}
	}
}

def autoConfigDir = new File(project.build.directory, 'auto-config')
def generatedResourcesDir = new File(project.build.directory, 'generated-resources')
autoConfigDir.eachDir { processModule(it, generatedResourcesDir) }
