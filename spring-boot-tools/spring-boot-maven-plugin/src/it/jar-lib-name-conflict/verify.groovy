import java.io.*;
import org.springframework.boot.maven.*;

File f = new File(basedir, "test-project/target/test-project-0.0.1.BUILD-SNAPSHOT.jar")
new Verify.JarArchiveVerification(f, Verify.SAMPLE_APP) {
	@Override
	protected void verifyZipEntries(Verify.ArchiveVerifier verifier) throws Exception {
		super.verifyZipEntries(verifier)
		verifier.assertHasEntryNameStartingWith("lib/org.springframework.boot.maven.it-acme-lib-0.0.1.BUILD-SNAPSHOT.jar")
		verifier.assertHasEntryNameStartingWith("lib/org.springframework.boot.maven.it.another-acme-lib-0.0.1.BUILD-SNAPSHOT.jar")
	}
}.verify();


