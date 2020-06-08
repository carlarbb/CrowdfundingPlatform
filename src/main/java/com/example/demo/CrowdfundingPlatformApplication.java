package com.example.demo;

import com.example.demo.configuration.SecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

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

