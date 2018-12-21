import org.springframework.boot.maven.*

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

File main = new File(basedir, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar")
File backup = new File(basedir, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar.original")
Verify.verifyJar(main, "some.random.Main")
assertTrue 'backup file should exist', backup.exists()

def file = new File(basedir, "build.log")
assertTrue 'main artifact should have been updated',
		file.text.contains("Updating main artifact " + main + " to " + backup)
assertTrue 'main artifact should have been installed',
		file.text.contains ("Installing " + backup + " to")
assertFalse 'repackaged artifact should not have been installed',
		file.text.contains ("Installing " + main + "to")

