package club.mydlq.swagger.kubernetes.config;

import club.mydlq.swagger.kubernetes.zuul.RefreshRoute;
import club.mydlq.swagger.kubernetes.zuul.ZuulRouteLocator;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author mydlq
 * @author WeiX Sun
 */
@EnableZuulProxy
public class ZuulConfig {

	@Bean
	public ZuulRouteLocator routeLocator(ServerProperties server,
			ZuulProperties zuulProperties) {
		return new ZuulRouteLocator(server.getServlet().getContextPath(), zuulProperties);
	}

	@Bean
	public RefreshRoute refreshRoute(ZuulRouteLocator routeLocator) {
		return new RefreshRoute(routeLocator);
	}

}
