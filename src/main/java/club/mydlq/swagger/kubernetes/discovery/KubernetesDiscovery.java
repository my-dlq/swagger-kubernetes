package club.mydlq.swagger.kubernetes.discovery;

import club.mydlq.swagger.kubernetes.param.DiscoveryAutoConfig;
import club.mydlq.swagger.kubernetes.param.SwaggerAutoConfig;
import club.mydlq.swagger.kubernetes.entity.ServiceInfo;
import club.mydlq.swagger.kubernetes.swagger.SwaggerResources;
import club.mydlq.swagger.kubernetes.utils.FileUtils;
import club.mydlq.swagger.kubernetes.utils.HttpUtils;
import club.mydlq.swagger.kubernetes.utils.ValidationUtils;
import club.mydlq.swagger.kubernetes.zuul.RefreshRoute;
import club.mydlq.swagger.kubernetes.zuul.ZuulRouteLocator;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Kubernetes 服务发现
 *
 * @author mydlq
 */
@Slf4j
public class KubernetesDiscovery implements SchedulingConfigurer {

    /**
     * NodePort 端口常量
     */
    public static final String PORT_TYPE_NODEPORT = "NodePort";
    /**
     * 路径分隔符常量
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * 服务发现配置
     */
    private final DiscoveryAutoConfig discoveryAutoConfig;
    /**
     * Swagger Resources 处理器
     */
    private final SwaggerResources swaggerResourcesProcessor;
    /**
     * Zuul 路由配置
     */
    private final ZuulRouteLocator zuulRouteLocator;
    /**
     * Zuul 路由刷新配置
     */
    private final RefreshRoute refreshRouteService;
    /**
     * Swagger 配置
     */
    private final SwaggerAutoConfig swaggerAutoConfig;
    /**
     * Zuul 参数对象
     */
    private final ZuulProperties zuulProperties;

    public KubernetesDiscovery(SwaggerResources swaggerResourcesProcessor,
                               ZuulRouteLocator zuulRouteLocator,
                               RefreshRoute refreshRouteService,
                               SwaggerAutoConfig swaggerAutoConfig,
                               ZuulProperties zuulProperties,
                               DiscoveryAutoConfig discoveryAutoConfig) {
        this.swaggerResourcesProcessor = swaggerResourcesProcessor;
        this.zuulRouteLocator = zuulRouteLocator;
        this.refreshRouteService = refreshRouteService;
        this.swaggerAutoConfig = swaggerAutoConfig;
        this.zuulProperties = zuulProperties;
        this.discoveryAutoConfig = discoveryAutoConfig;
    }

    /**
     * 刷新服务列表
     * Refresh service list
     */
    public void freshServiceList() {
        serviceFresh();
    }

    /**
     * 定时任务
     * timed task
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                new IntervalTask(
                        this::serviceFresh,
                        discoveryAutoConfig.getInterval() * 1000,
                        discoveryAutoConfig.getInitialDelay() * 1000));
    }

    /**
     * 刷新服务列表
     * fresh kubernetes service list
     */
    private void serviceFresh() {
        // 验证 Namespace 是否设置，如果未设置则默认读取集群所在的 Namespace
        readNamespace();
        // 获取动态服务列表
        List<ServiceInfo> serviceInfos = getServiceInfo(discoveryAutoConfig.getNamespace(),
                discoveryAutoConfig.getPortType(),
                discoveryAutoConfig.getUrl(),
                swaggerAutoConfig.getDocApiPath());
        // 获取静态服务列表
        List<ServiceInfo> staticServiceList = getStaticServiceList();
        // 将静态服务列表加入总服务列表
        serviceInfos.addAll(staticServiceList);
        // 忽略用户指定的服务
        excludeService(serviceInfos, swaggerAutoConfig.getIgnoreServices());
        // 刷新 Swagger 服务列表
        swaggerResourcesProcessor.updateServiceInfos(serviceInfos);
        // 刷新 Zuul 服务列表
        zuulRouteLocator.setServiceInfos(serviceInfos);
        refreshRouteService.refreshRoute();
    }

    /**
     * 获取静态服务列表
     */
    private List<ServiceInfo> getStaticServiceList() {
        List<ServiceInfo> serviceInfoList = new ArrayList<>();
        for (ZuulRoute route : zuulProperties.getRoutes().values()) {
            ServiceInfo serviceInfo = analysisStaticService(route);
            if (serviceInfo != null) {
                serviceInfoList.add(serviceInfo);
            }
        }
        return serviceInfoList;
    }

    /**
     * 分析静态服务
     *
     * @param route 路由对象
     * @return 服务信息
     */
    private ServiceInfo analysisStaticService(ZuulRoute route) {
        boolean isVerify = true;
        ServiceInfo serviceInfo = new ServiceInfo();
        // 非空验证
        if (route.getUrl() == null || route.getPath() == null) {
            isVerify = false;
        }
        // 如果不是以 http 或者 https 开始,则默认加上 "http://"
        if (isVerify && (!(route.getUrl().startsWith("http") || route.getUrl().startsWith("https")))) {
            route.setUrl("http://" + route.getUrl());
        }
        // 如果为域名,则默认加上 "80" 端口
        if (!ValidationUtils.validatePort(route.getUrl())) {
            route.setUrl(route.getUrl() + ":80");
        }
        // URL验证
        if (!ValidationUtils.validateUrl(route.getUrl())) {
            isVerify = false;
        }
        // 截取 URL,设置Host & Port
        String host = StringUtils.substringBeforeLast(route.getUrl(), ":");
        String port = StringUtils.substringAfterLast(route.getUrl(), ":");
        // 检测截取端口是否为数字
        if (!StringUtils.isNumeric(port)) {
            isVerify = false;
        }
        if (!isVerify) {
            return null;
        }
        serviceInfo.setHost(host);
        serviceInfo.setPort(Integer.parseInt(port));
        // Path验证
        if (!route.getPath().startsWith(PATH_SEPARATOR)) {
            route.setPath(PATH_SEPARATOR + route.getPath());
        }
        // 拆分Path,设置Name
        String[] paths = route.getPath().split(PATH_SEPARATOR);
        serviceInfo.setName(paths[1]);
        // 设置SwaggerUrl
        serviceInfo.setPath(discoveryAutoConfig.getUrl());
        return serviceInfo;
    }

    /**
     * 排除设置的不需要发现的服务
     *
     * @param serviceInfoList    服务信息列表
     * @param excludeServiceList 排除的服务名称列表
     */
    private void excludeService(List<ServiceInfo> serviceInfoList, Set<String> excludeServiceList) {
        if (excludeServiceList == null) {
            return;
        }
        List<ServiceInfo> excludeServiceInfoList = new ArrayList<>();
        for (String serviceName : excludeServiceList) {
            for (ServiceInfo serviceInfo : serviceInfoList) {
                if (StringUtils.equalsIgnoreCase(serviceInfo.getName(), serviceName)) {
                    excludeServiceInfoList.add(serviceInfo);
                }
            }
        }
        serviceInfoList.removeAll(excludeServiceInfoList);
    }

    /**
     * 获取 ServiceInfo 列表
     *
     * @param namespace  命名空间
     * @param host       主机地址
     * @param portType   端口类型，支持ClusterIP Or NodePort
     * @param swaggerUrl swagger url 地址
     * @return ServiceInfo 列表
     */
    private static List<ServiceInfo> getServiceInfo(String namespace, String portType, String host, String swaggerUrl) {
        if (StringUtils.isEmpty(namespace)) {
            throw new NullPointerException("namespace is null!");
        }
        List<ServiceInfo> serviceInfos = new ArrayList<>();
        // 从 Kubernetes 集群获取 Service 列表
        List<V1Service> serviceList = getKubernetesServiceList(namespace, portType);
        // 检测 swagger url 是否以 "/" 开始
        if (!swaggerUrl.startsWith(PATH_SEPARATOR)) {
            swaggerUrl = PATH_SEPARATOR + swaggerUrl;
        }
        // 检测接口是否符合要求
        for (V1Service service : serviceList) {
            // 如果 Service 名称为空，就跳过
            if(service.getMetadata() == null){
                continue;
            }
            // 设置 host
            String serviceHost = "http://" + Objects.requireNonNull(service.getMetadata()).getName() + "." + service.getMetadata().getNamespace();
            if (portType.equalsIgnoreCase(PORT_TYPE_NODEPORT)) {
                serviceHost = host;
            }
            // 获取端口列表
            Integer[] ports = getPort(service, portType);
            // 根据 Port & swaggerUrl 检查地址是否是 Swagger Api 来确定是否加入服务列表
            for (Integer port : ports) {
                log.debug(serviceHost + ":" + port + swaggerUrl);
                ServiceInfo serviceInfo = new ServiceInfo();
                serviceInfo.setName(service.getMetadata().getName());
                serviceInfo.setHost(serviceHost);
                serviceInfo.setPort(port);
                serviceInfo.setPath(swaggerUrl);
                serviceInfos.add(serviceInfo);
            }
            // 过滤 Service,只保留拥有swagger api的服务
            HttpUtils.checkUrl(serviceInfos);
        }
        return serviceInfos;
    }

    /**
     * 获得端口列表
     *
     * @param service Service 对象
     * @param type    端口类型
     * @return 端口对象列表
     */
    private static Integer[] getPort(V1Service service, String type) {
        // 设置待返回的端口列表
        List<Integer> ports = new ArrayList<>();
        // 判断 Servie 和 Service spec 与 Ports 不为空，则将端口加入到列表中
        if (service != null && service.getSpec() != null && service.getSpec().getPorts() != null) {
            // 获取端口列表
            List<V1ServicePort> servicePortList = service.getSpec().getPorts();
            // 循环列表，进行条件（ClusterIP 和 NodePort）判断，符合条件的就添加到列表集合
            for (V1ServicePort port : servicePortList) {
                if (StringUtils.equalsIgnoreCase(type, PORT_TYPE_NODEPORT)) {
                    ports.add(port.getNodePort());
                } else {
                    ports.add(port.getPort());
                }
            }
        }
        return ports.toArray(new Integer[0]);
    }

    /**
     * 从 Kubernetes 中获取 Service 列表
     *
     * @param namespace 命名空间
     * @param portType  端口类型
     * @return Service 列表
     */
    private static List<V1Service> getKubernetesServiceList(String namespace, String portType) {
        // 设置 Api 客户端
        CoreV1Api api = new CoreV1Api();
        // 设置返回的 Kubernetes Service 集合列表
        List<V1Service> kubernetesServiceList = new ArrayList<>();
        try {
            // 通过 Kubernetes API 获取 Service 与 EndPoint 列表
            V1ServiceList serviceList = api.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null);
            V1EndpointsList endPointList = api.listNamespacedEndpoints(namespace, null, null, null, null, null, null, null, null, null);
            // 判断列表是否获成功，如果值为空则直接返回 Service 空集合
            if (serviceList == null || endPointList == null) {
                return kubernetesServiceList;
            }
            // 检测 Service 是否包含 Endpoints（不包含则将该 Service 过滤），如果端口类型为 NodePort,则检测端口类型
            // 循环 Service 列表，获取每个 Service 信息，加入到待返回的 Service 集合
            for (V1Service service : serviceList.getItems()) {
                // 判断是否存在 Metadata，如果存在则继续，否则跳过本次循环
                if (service.getMetadata() == null || service.getSpec() == null) {
                    continue;
                }
                // 获取服务名称
                String serviceName = service.getMetadata().getName();
                // 判断 Kubernetes Service 是否关联了 EndPoints 资源
                boolean isContainEndpoints = isContainEndpoints(endPointList, serviceName);
                // 判断 Kubernetes Service 类型是否为和指定的类型一致
                boolean isMatchType = StringUtils.equalsIgnoreCase(service.getSpec().getType(), portType);
                // 如果不包含 Endpoints 资源或
                if (isContainEndpoints && isMatchType) {
                    kubernetesServiceList.add(service);
                }
            }
        } catch (ApiException e) {
            log.error(e.getMessage());
        }
        return kubernetesServiceList;
    }

    /**
     * 检测 Service 中是否包含 Endpoints
     *
     * @param endpointList Endpoints 列表
     * @param serviceName  Service 名称
     * @return Service 中是否包含 Endpoints
     */
    private static boolean isContainEndpoints(V1EndpointsList endpointList, String serviceName) {
        if (endpointList.getItems() != null) {
            for (V1Endpoints endpoints : endpointList.getItems()) {
                if (endpoints.getMetadata() == null ||
                        endpoints.getSubsets() == null ||
                        endpoints.getSubsets().get(0) == null ||
                        endpoints.getSubsets().get(0).getAddresses() == null) {
                    continue;
                }
                // 从 Endpoints 列表中查找到对应的服务
                boolean isMatch = StringUtils.equalsIgnoreCase(endpoints.getMetadata().getName(), serviceName);
                // 检测该 Endpoints 是否存在关联的 Pod 地址
                List<V1EndpointAddress> endpointAddresses = endpoints.getSubsets().get(0).getAddresses();
                boolean hasAddress = endpointAddresses != null && !endpointAddresses.isEmpty();
                // 如果满足上面设置的两个条件，则返回 ture
                if (isMatch && hasAddress) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 读取容器所在的 Namespace
     * Read the Namespace where the container is located
     */
    private void readNamespace() {
        if (StringUtils.isEmpty(discoveryAutoConfig.getNamespace())) {
            String namespace = FileUtils.readFile(Config.SERVICEACCOUNT_ROOT + "/namespace");
            discoveryAutoConfig.setNamespace(namespace);
            log.info("read namespace " + namespace);
        }
    }

}