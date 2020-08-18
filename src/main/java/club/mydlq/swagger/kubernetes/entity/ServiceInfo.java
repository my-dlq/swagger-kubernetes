package club.mydlq.swagger.kubernetes.entity;

import lombok.Data;
import lombok.ToString;

/**
 * Kubernetes Service 信息
 *
 * @author mydlq
 */
@Data
@ToString
public class ServiceInfo {
    /**
     * 服务名称
     */
    private String name;
    /**
     * 主机地址
     */
    private String host;
    /**
     * 端口号
     */
    private Integer port;
    /**
     * 接口路径
     */
    private String path;
}

