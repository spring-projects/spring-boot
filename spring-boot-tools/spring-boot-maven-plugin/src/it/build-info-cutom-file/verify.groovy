import org.springframework.boot.maven.Verify

import static org.junit.Assert.assertTrue

def file = new File(basedir, "target/build.info")
println file.getAbsolutePath()
Properties properties = Verify.verifyBuildInfo(file,
		'org.springframework.boot.maven.it', 'build-info-custom-file',
		'Generate custom build info', '0.0.1.BUILD-SNAPSHOT')
assertTrue properties.containsKey('build.time')