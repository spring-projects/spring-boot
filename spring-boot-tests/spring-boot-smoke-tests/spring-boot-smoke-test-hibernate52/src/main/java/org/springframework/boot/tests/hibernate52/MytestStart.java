package org.springframework.boot.tests.hibernate52;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class MytestStart {
	public static void main(String[] args) {
		System.out.println("Spring Boot 源码剖析之源码环境搭建验证");
		SpringApplication.run(MytestStart.class, args);;
		System.out.println(1111);
		System.out.println("Spring Boot 源码剖析之源码环境搭建验证成功");
		Arrays.asList(1,2,3,45).stream().
				filter(i->i%2==0||i%3==0).
				map(i->i*i).
				forEach(System.out::println);
	}
}







