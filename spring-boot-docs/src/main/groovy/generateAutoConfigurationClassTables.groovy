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
		writer.println "| {github-code}/$module/src/main/java/$it.path.{sc-ext}[`$it.name`]"
		writer.println "| {dc-root}/$it.path.{dc-ext}[javadoc]"
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
