import java.io.*;
import org.springframework.boot.maven.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

File repackaged = new File(basedir, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT-test.jar")
new Verify.JarArchiveVerification(repackaged, Verify.SAMPLE_APP).verify();

File main = new File( basedir, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar")
assertTrue 'main artifact should exist', main.exists()

File backup = new File( basedir, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar.original")
assertFalse 'backup artifact should not exist', backup.exists()

def file = new File(basedir, "build.log")
assertTrue 'repackaged artifact should have been attached', file.text.contains("Attaching archive " + repackaged)
assertTrue 'main artifact should have been installed', file.text.contains ("Installing "+main)
assertTrue 'repackaged artifact should have been installed', file.text.contains ("Installing "+repackaged)

