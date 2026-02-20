package top.orosirian.orodisk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// npm run build -> dist文件夹传到/opt/OroDisk/
// mvn install -> target/disk-backend-1.0.0.jar传到/opt/OroDisk/ -> sudo systemctl restart orodisk
@SpringBootApplication
@MapperScan("top.orosirian.orodisk.mappers")
@EnableScheduling
public class DiskApplication {

    static void main(String[] args) {
        SpringApplication.run(DiskApplication.class, args);
    }

}
