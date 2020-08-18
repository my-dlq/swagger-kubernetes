package club.mydlq.swagger.kubernetes.param;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kubernetes 配置参数
 *
 * @author mydlq
 */
@Data
@ConfigurationProperties("swagger.kubernetes.connect")
public class KubernetesAutoConfig {

    /**
     * Kubernetes ApiServer url 地址
     */
    private String url = "";

    /**
     * Kubernetes token 串
     */
    private String token = "";

    /**
     * Kubernetes token 文件路径
     */
    private String tokenPath = "";

    /**
     * 是否验证 SSL 证书
     */
    boolean validateSsl = false;

    /**
     * Kubernetes ca 证书文件路径
     */
    private String caPath = "";

    /**
     * 连接 Kubernetes 配置文件的路径
     */
    private String fromConfigPath = "";

}
