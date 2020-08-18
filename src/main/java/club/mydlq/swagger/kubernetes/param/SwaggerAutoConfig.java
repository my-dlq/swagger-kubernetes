package club.mydlq.swagger.kubernetes.param;

import lombok.Data;
import java.util.Set;
import java.util.HashSet;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 全局 Swagger 配置参数
 *
 * @author mydlq
 */
@Data
@ConfigurationProperties("swagger.global")
public class SwaggerAutoConfig {

    /**
     * 全局 Swagger 文档 API 路径
     */
    private String docApiPath;

    /**
     * 全局 Swagger 文档版本
     */
    private String swaggerVersion = "2.0";

    /**
     * 忽略服务列表
     */
    private Set<String> ignoreServices = new HashSet<>();

}
