import groovy.util.XmlSlurper

private String format(String input) {
	input.replace("<code>", "`")
		.replace("</code>", "`")
		.replace("&lt;", "<")
		.replace("&gt;", ">")
		.replace("<br>", " ")
		.replace("\n", " ")
		.replace("&quot;", '"')
		.replaceAll('\\{@code (.*?)\\}', '`$1`')
		.replaceAll('\\{@link (.*?)\\}', '`$1`')
		.replaceAll('\\{@literal (.*?)\\}', '`$1`')
		.replaceAll('<a href=."(.*?)".>(.*?)</a>', '\$1[\$2]')
}

private writeParametersTable(PrintWriter writer, def goal, def parameters, def configuration) {
	writer.println '[cols="3,2,3"]'
	writer.println '|==='
	writer.println '| Name | Type | Default'
	writer.println()
	parameters.each { parameter ->
		def name = parameter.name.text()
		writer.println("| <<goals-$goal-parameters-details-$name,$name>>")
		def type = parameter.type.text()
		if (type.lastIndexOf('.') >= 0) {
			type = type.substring(type.lastIndexOf('.') + 1)
		}
		writer.println("| `$type`")
		def defaultValue = "${configuration[name].@'default-value'}"
		if (defaultValue) {
			writer.println("| `$defaultValue`")
		}
		else {
			writer.println("|")
		}
		writer.println()
	}
	writer.println '|==='
}

private writeParameterDetails(PrintWriter writer, def parameters, def configuration, def sectionId) {
	parameters.each { parameter ->
		def name = parameter.name.text()
		writer.println "[[$sectionId-$name]]"
		writer.println "==== `$name`"
		writer.println(format(parameter.description.text()))
		writer.println()
		writer.println '[cols="10h,90"]'
		writer.println '|==='
		writer.println()
		writer.println '| Name'
		writer.println "| `$name`"
		writer.println '| Type'
		def type = parameter.type.text()
		if (type.lastIndexOf('.') >= 0) {
			type = type.substring(type.lastIndexOf('.') + 1)
		}
		writer.println("| `$type`")
		def defaultValue = "${configuration[name].@'default-value'}"
		if (defaultValue) {
			writer.println '| Default value'
			writer.println("| `$defaultValue`")
		}
		def userProperty = "${configuration[name].text().replace('${', '`').replace('}', '`')}"
		writer.println '| User property'
		userProperty ? writer.println("| ${userProperty}") : writer.println("|")
		writer.println '| Since'
		def since = parameter.since.text()
		since ? writer.println("| `${since}`") : writer.println("|")
		writer.println '| Required'
		writer.println "| ${parameter.required.text()}"
		writer.println()
		writer.println '|==='
	}
}

def plugin = new XmlSlurper().parse("${project.build.outputDirectory}/META-INF/maven/plugin.xml" as File)
String goalPrefix = plugin.goalPrefix.text()
File goalsDir = new File(project.build.directory, "generated-resources/goals/")
goalsDir.mkdirs()

new File(goalsDir, "overview.adoc").withPrintWriter { writer ->
	writer.println '[cols="1,3"]'
	writer.println '|==='
	writer.println '| Goal | Description'
	writer.println()
	plugin.mojos.mojo.each { mojo ->
		def goal = mojo.goal.text()
		writer.println "| <<goals-$goal,${goalPrefix}:${mojo.goal.text()}>>"
		writer.println "| ${format(mojo.description.text())}"
		writer.println()
	}
	writer.println '|==='
}

plugin.mojos.mojo.each { mojo ->
	String goal = mojo.goal.text()
	new File(goalsDir, "${goal}.adoc").withPrintWriter { writer ->
		def sectionId = "goals-$goal"
		writer.println()
		writer.println("[[$sectionId]]")
		writer.println("== `$goalPrefix:$goal`")
		writer.println("`${plugin.groupId.text()}:${plugin.artifactId.text()}:${plugin.version.text()}:${mojo.goal.text()}`")
		writer.println()
		writer.println(format(mojo.description.text()))
		writer.println()
		def parameters = mojo.parameters.parameter.findAll { it.editable.text() == 'true' }
		def requiredParameters =  parameters.findAll { it.required.text() == 'true' }
		if (requiredParameters.size()) {
			writer.println("[[$sectionId-parameters-required]]")
			writer.println("=== Required parameters")
			writeParametersTable(writer, goal, requiredParameters, mojo.configuration)
			writer.println()
		}
		def optionalParameters = parameters.findAll { it.required.text() == 'false' }
		if (optionalParameters.size()) {
			writer.println("[[$sectionId-parameters-optional]]")
			writer.println("=== Optional parameters")
			writeParametersTable(writer, goal, optionalParameters, mojo.configuration)
			writer.println()
		}
		def detailsSectionId = "$sectionId-parameters-details"
		writer.println("[[$detailsSectionId]]")
		writer.println("=== Parameter details")
		writeParameterDetails(writer, parameters, mojo.configuration, detailsSectionId)
		writer.println()
	}
}
