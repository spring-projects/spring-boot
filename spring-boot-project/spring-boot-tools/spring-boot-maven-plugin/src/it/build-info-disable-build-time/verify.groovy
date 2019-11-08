import org.springframework.boot.maven.Verify

import static org.junit.Assert.assertFalse

def file = new File(basedir, "target/classes/META-INF/build-info.properties")
Properties properties = Verify.verifyBuildInfo(file,
		'org.springframework.boot.maven.it', 'build-info-disable-build-time',
		'Generate build info with disabled build time', '0.0.1.BUILD-SNAPSHOT')
assertFalse 'build time must not be present', properties.containsKey('build.time')
