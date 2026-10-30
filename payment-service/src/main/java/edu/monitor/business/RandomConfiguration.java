package edu.monitor.business;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.*;

@Configuration
public class RandomConfiguration {
  @Bean
  FailureRandom failureRandom() {
    return () -> ThreadLocalRandom.current().nextDouble();
  }
}
