def jarfile = './target/executable-props-0.0.1.BUILD-SNAPSHOT-full.jar'

new File("${basedir}/application.properties").delete()

String exec(String command) {
	def proc = command.execute([], basedir)
	proc.waitFor()
	proc.err.text
}

String out = exec("java -jar ${jarfile}")
assert out.contains('Hello Embedded World!'),
	'Using -jar my.jar should use the application.properties from the jar\n' + out

out = exec("java -cp ${jarfile} org.springframework.boot.loader.PropertiesLauncher")
assert out.contains('Hello Embedded World!'),
	'Using -cp my.jar with PropertiesLauncher should use the application.properties from the jar\n' + out

new File("${basedir}/application.properties").withWriter { it -> it << "message: Foo" }
out = exec("java -jar ${jarfile}")
assert out.contains('Hello Embedded World!'),
	'Should use the application.properties from the jar in preference to local filesystem\n' + out

out = exec("java -Dloader.path=.,lib -jar ${jarfile}")
assert out.contains('Hello Embedded Foo!'),
	'With loader.path=.,lib should use the application.properties from the local filesystem\n' + out

new File("${basedir}/target/application.properties").withWriter { it -> it << "message: Spam" }
out = exec("java -Dloader.path=target,.,lib -jar ${jarfile}")
assert out.contains('Hello Embedded Spam!'),
	'With loader.path=target,.,lib should use the application.properties from the target directory\n' + out