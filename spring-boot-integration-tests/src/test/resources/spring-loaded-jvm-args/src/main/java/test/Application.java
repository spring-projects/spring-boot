package test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

public class Application {

    public static void main(String[] args) throws Exception {
    	PrintWriter writer = new PrintWriter(new FileWriter(new File("build/output.txt")));
    	for (String argument: ManagementFactory.getRuntimeMXBean().getInputArguments()) {
    		writer.println(argument);
    	}
    	writer.close();
    }
}
