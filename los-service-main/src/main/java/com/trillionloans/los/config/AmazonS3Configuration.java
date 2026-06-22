package com.trillionloans.los.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonS3Configuration {

  @Value("${aws.region}")
  private String region;

  @Bean
  public AmazonS3 s3Client() {
    return AmazonS3ClientBuilder.standard().withRegion(region).build();
  }
}
