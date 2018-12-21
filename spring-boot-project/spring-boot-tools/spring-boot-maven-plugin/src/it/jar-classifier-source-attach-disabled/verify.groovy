import org.springframework.boot.maven.*

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

File main = new File(basedir, "target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar");
File backup = new File( basedir, "target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar.original")

new Verify.JarArchiveVerification(main, Verify.SAMPLE_APP).verify();
assertTrue 'backup artifact should exist', backup.exists()

def file = new File(basedir, "build.log")
assertFalse 'repackaged artifact should not have been attached',
		file.text.contains("Attaching repackaged archive " + main + " with classifier test")
assertTrue 'test artifact should have been updated',
		file.text.contains("Updating artifact with classifier test " + main + " to " + backup)
assertTrue 'backup artifact should have been installed',
		file.text.contains ("Installing " + backup + " to")
assertFalse 'repackaged artifact should not have been installed',
		file.text.contains ("Installing " + main + " to")

