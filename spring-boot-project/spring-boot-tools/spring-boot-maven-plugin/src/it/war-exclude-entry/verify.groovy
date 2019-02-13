
import org.springframework.boot.maven.Verify;

new Verify.WarArchiveVerification(new File(basedir, "target/war-exclude-entry-0.0.1.BUILD-SNAPSHOT.war")) {

	@Override
	protected void verifyZipEntries(Verify.ArchiveVerifier verifier) throws Exception {
		super.verifyZipEntries(verifier)
		verifier.assertHasNoEntryNameStartingWith("lib/log4j-api-2.4.1.jar")
	}
}.verify()
