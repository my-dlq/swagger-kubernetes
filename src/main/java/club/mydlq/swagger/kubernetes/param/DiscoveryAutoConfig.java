package club.mydlq.swagger.kubernetes.param;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 服务发现配置参数
 *
 * @author mydlq
 */
@Data
@ConfigurationProperties("swagger.discovery")
public class DiscoveryAutoConfig {

    /**
     * 服务发现间隔
     */
    private long interval = 30;

    /**
     * 服务发现初始化延迟加载时间
     */
    private long initialDelay = 30;

    /**
     * 服务发现地址
     */
    private String url = "";

    /**
     * 指定监控的 Namespace
     */
    private String namespace = "";

    /**
     * 指定监控哪种 Kubernetes Service Type 的服务，默认有 ClusterIP 和 NodePort
     */
    private String portType = "ClusterIP";
}
