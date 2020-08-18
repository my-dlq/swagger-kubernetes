package club.mydlq.swagger.kubernetes.zuul;

import club.mydlq.swagger.kubernetes.entity.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zuul 路由管理
 *
 * @author mydlq
 */
@Slf4j
public class ZuulRouteLocator extends SimpleRouteLocator implements RefreshableRouteLocator {

    private List<ServiceInfo> serviceInfos = new ArrayList<>();

    public ZuulRouteLocator(String servletPath, ZuulProperties properties) {
        super(servletPath, properties);
    }

    /**
     * 更新服务列表
     * Update service list
     *
     * @param serviceInfos
     */
    public void setServiceInfos(List<ServiceInfo> serviceInfos) {
        this.serviceInfos = serviceInfos;
    }

    /**
     * 加载 Kubernetes 服务列表
     * Load the Kubernetes service list
     *
     * @return
     */
    private Map<String, ZuulProperties.ZuulRoute> locateRoutesFromKubernetes() {
        Map<String, ZuulRoute> routes = new LinkedHashMap<>();
        try {
            for (ServiceInfo serviceInfo : serviceInfos) {
                ZuulRoute zuulRoute = new ZuulRoute();
                zuulRoute.setId(serviceInfo.getName());
                zuulRoute.setUrl(serviceInfo.getHost() + ":" + serviceInfo.getPort());
                zuulRoute.setPath("/" + serviceInfo.getName() + "/**");
                zuulRoute.setServiceId("/" + serviceInfo.getName() + "/**");
                routes.put("/" + serviceInfo.getName() + "/**", zuulRoute);
            }
        } catch (Exception e) {
            log.error("update zuul service list error!",e);
        }
        return routes;
    }

    /**
     * 重写 Zuul 路由策略
     * Rewrite Zuul routing policy
     *
     * @return
     */
    @Override
    protected LinkedHashMap<String, ZuulRoute> locateRoutes() {
        LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<>();
        /** ----------------------Loads custom routing information---------------------- **/
        routesMap.putAll(locateRoutesFromKubernetes());
        LinkedHashMap<String, ZuulRoute> values = new LinkedHashMap<>();
        for (Map.Entry<String, ZuulRoute> entry : routesMap.entrySet()) {
            String path = entry.getKey();
            values.put(path, entry.getValue());
        }
        return values;
    }

    @Override
    public void refresh() {
        doRefresh();
    }

}
