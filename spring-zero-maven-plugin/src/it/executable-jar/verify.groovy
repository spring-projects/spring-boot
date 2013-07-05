import java.io.*;
import org.springframework.bootstrap.maven.*;

Verify.verifyJar(
	new File( basedir, "target/executable-jar-0.0.1.BUILD-SNAPSHOT.jar" )
);

