import java.io.*;
import org.springframework.maven.packaging.*;

Verify.verifyJar(
	new File( basedir, "target/executable-jar-0.0.1.BUILD-SNAPSHOT.jar" )
);

