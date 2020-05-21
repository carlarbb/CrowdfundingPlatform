package com.example.demo;

import com.example.demo.classes.Role;
import com.example.demo.configuration.SecurityConfig;
import com.example.demo.context.ApplicationContextProvider;
import com.example.demo.repositories.RoleRepository;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.SpringVersion;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@SpringBootApplication
@Import({SecurityConfig.class})
public class CrowdfundingPlatformApplication {
	public static void main(String[] args) {
		SpringApplication.run(CrowdfundingPlatformApplication.class, args);

		//afisare bean-uri
//		String[] beanNames = ApplicationContextProvider.getContext().getBeanDefinitionNames();
//		Arrays.sort(beanNames);
//		for(String beanName : beanNames) System.out.println(beanName);
//		System.out.println();
		//System.out.println(ApplicationContextProvider.getContext().getBean(.class).getObject().getLoggedKeyspace());
	}
}

