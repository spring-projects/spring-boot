import org.springframework.boot.maven.*

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

File main = new File(basedir, "target/jar-0.0.1.BUILD-SNAPSHOT.jar")
File backup = new File(basedir, "target/jar-0.0.1.BUILD-SNAPSHOT.jar.original")
Verify.verifyJar(main, "some.random.Main")
assertTrue 'backup file should exist', backup.exists()

def file = new File(basedir, "build.log")
assertTrue 'main artifact should have been replaced by repackaged archive',
		file.text.contains("Replacing main artifact with repackaged archive")
assertTrue 'main artifact should have been installed',
		file.text.contains ("Installing " + main + " to")
assertFalse 'backup artifact should not have been installed',
		file.text.contains ("Installing " + backup + "to")

