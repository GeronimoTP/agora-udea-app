package com.udea.agora_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class AgoraBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgoraBackendApplication.class, args);
	}

}
