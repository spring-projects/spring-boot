def jarfile = './target/executable-props-lib-0.0.1.BUILD-SNAPSHOT-app.jar'

new File("${basedir}/application.properties").delete()

String exec(String command) {
	def proc = command.execute([], basedir)
	proc.waitFor()
	proc.err.text
}

String out = exec("java -jar ${jarfile}")
assert out.contains('Hello Embedded World!'),
	'Using -jar my.jar should load dependencies from separate jar and use the application.properties from the jar\n' + out

out = exec("java -cp ${jarfile} org.springframework.boot.loader.PropertiesLauncher")
assert out.contains('Hello Embedded World!'),
	'Using -cp my.jar with PropertiesLauncher should load dependencies from separate jar and use the application.properties from the jar\n' + out
