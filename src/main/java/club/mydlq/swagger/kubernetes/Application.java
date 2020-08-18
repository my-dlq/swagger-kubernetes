package club.mydlq.swagger.kubernetes;

import club.mydlq.swagger.kubernetes.config.EnableSwaggerKubernetes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot 启动类
 *
 * @author mydlq
 */
@SpringBootApplication
@EnableSwaggerKubernetes
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
