package club.mydlq.swagger.kubernetes.discovery;

import club.mydlq.swagger.kubernetes.param.KubernetesAutoConfig;
import club.mydlq.swagger.kubernetes.utils.FileUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.io.*;

/**
 * Kubernetes 服务发现
 *
 * @author mydlq / 小豆丁
 * Blog:   http://www.mydlq.club
 * Github: https://github.com/my-dlq/
 */
@Slf4j
public class KubernetesConnect {

    /**
     * Token 路径，用于检测是否是部署在 Pod 中
     */
    public static final String SERVICE_ACCOUNT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    /**
     * Kubernetes 配置参数
     */
    private final KubernetesAutoConfig kubernetesAutoConfig;

    public KubernetesConnect(KubernetesAutoConfig kubernetesAutoConfig) {
        this.kubernetesAutoConfig = kubernetesAutoConfig;
    }

    /**
     * 连接 Kubernetes 集群
     * Connection kubernetes
     */
    public void connection() {
        String token = kubernetesAutoConfig.getToken();
        String tokenPath = kubernetesAutoConfig.getTokenPath();
        String url = kubernetesAutoConfig.getUrl();
        String caPath = kubernetesAutoConfig.getCaPath();
        String formConfigPath = kubernetesAutoConfig.getFromConfigPath();
        boolean isFromCluster = new File(SERVICE_ACCOUNT_TOKEN_PATH).exists();
        boolean validateSsL = kubernetesAutoConfig.isValidateSsl();
        // form token
        if (StringUtils.isNotEmpty(tokenPath) && StringUtils.isNotEmpty(url)) {
            log.info("from token file connection kubernetes");
            token = FileUtils.readFile(tokenPath);
            connectFromToken(url, token, validateSsL, caPath);
        } else if (StringUtils.isNotEmpty(token) && StringUtils.isNotEmpty(url)) {
            log.info("from token connection kubernetes");
            connectFromToken(url, token, validateSsL, caPath);
        }
        // form cluster
        else if (isFromCluster) {
            log.info("from cluster env connection kubernetes");
            connectFromCluster(validateSsL);
        }
        // form config
        else if (StringUtils.isNotEmpty(formConfigPath)) {
            log.info("from config file connection kubernetes");
            connectFromConfig(formConfigPath);
        } else {
            log.info("from $HOME/.kube/config connection kubernetes");
            connectFromSystemConfig();
        }
    }

    /**
     * 默认方式,从系统配置 $HOME/.kube/config 读取配置文件连接 Kubernetes 集群
     * By default, connect to the Kubernetes cluster by reading a configuration file from the system configuration $HOME/.kube/config
     */
    private void connectFromSystemConfig() {
        ApiClient apiClient = null;
        try {
            apiClient = Config.defaultClient();
        } catch (IOException e) {
            log.error("read config file error",e);
        }
        Configuration.setDefaultApiClient(apiClient);
    }

    /**
     * 从指定文件读取配置文件连接 Kubernetes 集群
     *
     * @param configPath 连接 Kube-ApiServer 的配置文件路径
     */
    private void connectFromConfig(String configPath) {
        ApiClient apiClient = null;
        try {
            apiClient = Config.fromConfig(configPath);
        } catch (IOException e) {
            log.error("read config file error",e);
        }
        Configuration.setDefaultApiClient(apiClient);
    }

    /**
     * 通过 Token 连接 Kubernetes 集群
     *
     * @param url         kube-ApiServer 地址
     * @param token       连接 Kubernetes API 的 Token
     * @param validateSsl 是否验证 SSL
     * @param caPath      CA 证书路径
     */
    private void connectFromToken(String url, String token, boolean validateSsl, String caPath) {
        ApiClient apiClient = Config.fromToken(url, token, validateSsl);
        // validateSSL
        if (validateSsl) {
            try {
                apiClient.setSslCaCert(new FileInputStream(caPath));
            } catch (FileNotFoundException e) {
                log.error("check certificate file exists");
            }
        }
        Configuration.setDefaultApiClient(apiClient);
    }

    /**
     * 如果在 kubernetes 集群内，则利用 kubernetes 环境
     */
    private void connectFromCluster(boolean validateSsL) {
        ApiClient apiClient = null;
        try {
            apiClient = Config.fromCluster().setVerifyingSsl(validateSsL);
        } catch (IOException e) {
            log.error("read container token error", e);
        }
        Configuration.setDefaultApiClient(apiClient);
    }

}
