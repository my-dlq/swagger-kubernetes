package club.mydlq.swagger.kubernetes.zuul;

import org.springframework.cloud.netflix.zuul.RoutesRefreshedEvent;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Refresh {@code RouteLocator} endpoint.
 * @author mydlq
 * @author WeiX Sun
 */
public class RefreshRoute implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher publisher;

	private RouteLocator routeLocator;

	public RefreshRoute(RouteLocator routeLocator) {
		this.routeLocator = routeLocator;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void refreshRoute() {
		publisher.publishEvent(new RoutesRefreshedEvent(this.routeLocator));
	}

}
