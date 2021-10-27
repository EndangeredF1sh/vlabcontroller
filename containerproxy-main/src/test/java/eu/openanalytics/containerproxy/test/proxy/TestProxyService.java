package eu.openanalytics.containerproxy.test.proxy;

import java.net.URI;

import javax.inject.Inject;

import eu.openanalytics.containerproxy.service.UserService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import eu.openanalytics.containerproxy.ContainerProxyApplication;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.test.proxy.TestProxyService.TestConfiguration;
import eu.openanalytics.containerproxy.util.ProxyMappingManager;

@Ignore
@SpringBootTest(classes= {TestConfiguration.class, ContainerProxyApplication.class})
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class TestProxyService {

	@Inject
	private Environment environment;
	
	@Inject
	private ProxyService proxyService;
	
	@Test
	public void launchProxy() throws Exception {
		String specId = environment.getProperty("proxy.specs[0].id");

		ProxySpec baseSpec = proxyService.findProxySpec(s -> s.getId().equals(specId), true);
		ProxySpec spec = proxyService.resolveProxySpec(baseSpec, null, null);

		Proxy proxy = proxyService.startProxy(spec, true);
		proxyService.stopProxy(proxy, false, true);
	}
	
	public static class TestConfiguration {
		@Bean
		@Primary
		public ProxyMappingManager mappingManager() {
			return new NoopMappingManager();
		}

		@Bean
		@Primary
		public UserService mockedUserService() {
			return new MockedUserService();
		}
	}
	
	public static class NoopMappingManager extends ProxyMappingManager {
		@Override
		public synchronized void addMapping(String proxyId, String path, URI target) {
			// No-op
			System.out.println("NOOP");
		}
		
		@Override
		public synchronized void removeMapping(String path) {
			// No-ops
		}
	}
}
