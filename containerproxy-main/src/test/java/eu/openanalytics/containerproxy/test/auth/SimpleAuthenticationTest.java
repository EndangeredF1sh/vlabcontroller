package eu.openanalytics.containerproxy.test.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@ActiveProfiles("test-simple-auth")
public class SimpleAuthenticationTest {

	@Inject
	private MockMvc mvc;

	@Inject
	private Environment environment;
	
	@Test
	public void authenticateUser() throws Exception {
		String userName = environment.getProperty("proxy.users[0].name");
		String password = environment.getProperty("proxy.users[0].password");
		mvc
			.perform(get("/api/proxy").with(httpBasic(userName, password)).accept(MediaType.APPLICATION_JSON_VALUE))
			.andExpect(status().isOk());
	}
}
