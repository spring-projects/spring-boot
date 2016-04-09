import groovy.util.XmlSlurper

def getStarters(File dir) {
	def starters = []
	new File(project.build.directory, 'external-resources/starter-poms').eachDir { starterDir ->
		def pom = new XmlSlurper().parse(new File(starterDir, 'pom.xml'))
		def dependencies = getDependencies(pom)
		if (isStarter(dependencies)) {
			def name = pom.artifactId.text()
			starters << [
					'name': name,
					'description': postProcessDescription(pom.description.text()),
					'dependencies': dependencies,
					'pomUrl': "{github-code}/spring-boot-starters/$name/pom.xml"
			]
		}
	}
	return starters
}

boolean isApplicationStarter(def starter) {
	!isTechnicalStarter(starter) && !isProductionStarter(starter)
}

boolean isTechnicalStarter(def starter) {
	starter.name != 'spring-boot-starter-test' && !isProductionStarter(starter) &&
			starter.dependencies.find {
					it.startsWith('org.springframework.boot:spring-boot-starter') } == null
}

boolean isProductionStarter(def starter) {
	starter.name in ['spring-boot-starter-actuator', 'spring-boot-starter-remote-shell']
}

boolean isStarter(def dependencies) {
	!dependencies.empty
}

def postProcessDescription(String description) {
	addStarterCrossLinks(removeExtraWhitespace(description))
}

def removeExtraWhitespace(String input) {
	input.replaceAll('\\s+', ' ')
}

def addStarterCrossLinks(String input) {
	input.replaceAll('(spring-boot-starter[A-Za-z-]*)', '<<$1,`$1`>>')
}

def getDependencies(def pom) {
	dependencies = []
	pom.dependencies.dependency.each { dependency ->
		dependencies << "${dependency.groupId.text()}:${dependency.artifactId.text()}"
	}
	dependencies
}

def writeTable(String name, def starters) {
	new File(project.build.directory, "generated-resources/${name}.adoc").withPrintWriter { writer ->
		writer.println '|==='
		writer.println '| Name | Description | Pom'
		starters.each { starter ->
			writer.println ''
			writer.println "| [[${starter.name}]]`${starter.name}`"
			writer.println "| ${starter.description}"
			writer.println "| ${starter.pomUrl}[Pom]"
		}
		writer.println '|==='
	}
}

def starters = getStarters(new File(project.build.directory, 'external-resources/starter-poms'))
writeTable('application-starters', starters.findAll { isApplicationStarter(it) })
writeTable('production-starters', starters.findAll { isProductionStarter(it) })
writeTable('technical-starters', starters.findAll { isTechnicalStarter(it) })