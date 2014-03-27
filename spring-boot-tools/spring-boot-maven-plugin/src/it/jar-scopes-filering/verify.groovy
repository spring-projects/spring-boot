import java.io.*;
import org.springframework.boot.maven.*;

Verify.verifyJar(
	new File( basedir, "target/jar-scopes-filtering-0.0.1.BUILD-SNAPSHOT.jar" ), "some.random.Main"
);

Verify.verifyJarContain(
        new File( basedir, "target/jar-scopes-filtering-0.0.1.BUILD-SNAPSHOT.jar" ), "some.random.Main", "lib/spring-context-3.2.3.RELEASE.jar"
);

Verify.verifyJarNotContain(
        new File( basedir, "target/jar-scopes-filtering-0.0.1.BUILD-SNAPSHOT.jar" ), "some.random.Main", "lib/jcl-over-slf4j-1.7.6.jar"
);

Verify.verifyJarContain(
        new File( basedir, "target/jar-scopes-filtering-0.0.1.BUILD-SNAPSHOT.jar" ), "some.random.Main", "lib/spring-beans-3.2.3.RELEASE.jar"
);


