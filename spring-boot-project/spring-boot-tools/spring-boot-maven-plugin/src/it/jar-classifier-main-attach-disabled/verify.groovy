import org.springframework.boot.maven.*

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

File repackaged = new File(basedir, "target/jar-classifier-main-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar")
File main = new File(basedir, "target/jar-classifier-main-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar")
File backup = new File(basedir, "target/jar-classifier-main-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar.original")

new Verify.JarArchiveVerification(repackaged, Verify.SAMPLE_APP).verify();
assertTrue 'main artifact should exist', main.exists()
assertFalse 'backup artifact should not exist', backup.exists()

def file = new File(basedir, "build.log")
assertFalse 'repackaged artifact should not have been attached',
		file.text.contains("Attaching repackaged archive " + repackaged + " with classifier test")
assertTrue 'repackaged artifact should have been created',
		file.text.contains("Creating repackaged archive " + repackaged + " with classifier test")
assertTrue 'main artifact should have been installed',
		file.text.contains ("Installing " + main + " to")
assertFalse 'repackaged artifact should not have been installed',
		file.text.contains ("Installing " + repackaged + " to")
assertFalse 'backup artifact should not have been installed',
		file.text.contains ("Installing " + backup + "to")
