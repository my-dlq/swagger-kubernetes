package club.mydlq.swagger.kubernetes.swagger;

import club.mydlq.swagger.kubernetes.param.SwaggerAutoConfig;
import club.mydlq.swagger.kubernetes.entity.ServiceInfo;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * SwaggerResources 配置
 *
 * @author mydlq
 */
public class SwaggerResources implements SwaggerResourcesProvider {

    /**
     * 服务信息列表
     */
    List<ServiceInfo> serviceInfoList = new ArrayList<>();

    private final SwaggerAutoConfig swaggerAutoConfig;

    public SwaggerResources(SwaggerAutoConfig swaggerAutoConfig) {
        this.swaggerAutoConfig = swaggerAutoConfig;
    }

    /**
     * 更新服务列表
     *
     * @param serviceInfoList 服务信息列表
     */
    public void updateServiceInfos(List<ServiceInfo> serviceInfoList) {
        this.serviceInfoList = serviceInfoList;
    }

    /**
     * 增加 SwaggerResource 对象到 swagger 列表
     *
     * @return SwaggerResource 列表
     */
    @Override
    public List<SwaggerResource> get() {
        List<SwaggerResource> resources = new ArrayList<>();
        for (ServiceInfo serviceInfo : serviceInfoList) {
            resources.add(swaggerResource(serviceInfo.getName(),
                    "/" + serviceInfo.getName() + swaggerAutoConfig.getDocApiPath(),
                    swaggerAutoConfig.getSwaggerVersion()));
        }
        return resources;
    }

    /**
     * 创建 SwaggerResource 对象
     *
     * @param name 设置 Swagger 名称
     * @param location 设置 Swagger 位置
     * @param swaggerVersion 指定 swagger 版本
     * @return SwaggerResource 对象
     */
    private SwaggerResource swaggerResource(String name, String location, String swaggerVersion) {
        SwaggerResource swaggerResource = new SwaggerResource();
        swaggerResource.setName(name);
        swaggerResource.setLocation(location);
        swaggerResource.setSwaggerVersion(swaggerVersion);
        return swaggerResource;
    }

}
