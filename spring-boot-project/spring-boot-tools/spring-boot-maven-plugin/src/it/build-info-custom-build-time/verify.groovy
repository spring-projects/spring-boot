import org.springframework.boot.maven.Verify

import static org.junit.Assert.assertEquals

def file = new File(basedir, "target/classes/META-INF/build-info.properties")
Properties properties = Verify.verifyBuildInfo(file,
		'org.springframework.boot.maven.it', 'build-info-custom-build-time',
		'Generate build info with custom build time', '0.0.1.BUILD-SNAPSHOT')
assertEquals(properties.get('build.time'), '2019-07-08T08:00:00Z')
