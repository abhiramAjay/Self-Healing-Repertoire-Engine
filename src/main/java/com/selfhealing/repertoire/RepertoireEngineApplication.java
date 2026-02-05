package com.selfhealing.repertoire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@org.springframework.retry.annotation.EnableRetry
public class RepertoireEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepertoireEngineApplication.class, args);
	}

}
