// Generate a POM from the effective-pom that can be used to build a complete dependency tree

import groovy.util.*
import groovy.xml.*

def effectivePom = new XmlSlurper().parse(
	new File(project.build.directory, 'effective-pom/spring-boot-versions.xml'))

effectivePom.dependencyManagement.dependencies.dependency.findAll{ it.groupId != "org.springframework.boot" }.each {
	effectivePom.dependencies.appendNode( it )
}

// effectivePom.appendNode(effectivePom.dependencyManagement.dependencies)
effectivePom.parent.replaceNode {}
effectivePom.dependencyManagement.replaceNode {}
effectivePom.build.replaceNode {}
effectivePom.properties.replaceNode {}
effectivePom.repositories.replaceNode {}
effectivePom.pluginRepositories.replaceNode {}
effectivePom.reporting.replaceNode {}

out = new StreamingMarkupBuilder()
String xmlResult = out.bind {
	mkp.declareNamespace("": "http://maven.apache.org/POM/4.0.0")
	mkp.yield effectivePom
}


def outputDir = new File(project.build.directory, 'dependency-tree');
outputDir.mkdirs();
XmlUtil.serialize(xmlResult, new FileWriter(new File(outputDir, 'pom.xml')))



