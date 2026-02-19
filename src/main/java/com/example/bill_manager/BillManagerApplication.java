package com.example.bill_manager;

import com.example.bill_manager.config.GroqApiProperties;
import com.example.bill_manager.config.UploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GroqApiProperties.class, UploadProperties.class})
public class BillManagerApplication {

  public static void main(final String[] args) {
    SpringApplication.run(BillManagerApplication.class, args);
  }

}
