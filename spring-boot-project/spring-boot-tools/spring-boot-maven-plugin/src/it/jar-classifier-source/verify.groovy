import org.springframework.boot.maven.*

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

File repackaged = new File(basedir, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar")
File backup = new File(basedir, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original")

new Verify.JarArchiveVerification(repackaged, Verify.SAMPLE_APP).verify();
assertTrue 'backup artifact should exist', backup.exists()

def file = new File(basedir, "build.log")
assertTrue 'repackaged artifact should have been replaced',
		file.text.contains("Replacing artifact with classifier test with repackaged archive")
assertFalse 'backup artifact should not have been installed', file.text.contains ("Installing "+backup)
assertTrue 'repackaged artifact should have been installed', file.text.contains ("Installing "+repackaged)
