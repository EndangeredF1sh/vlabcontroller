package eu.openanalytics.containerproxy.test.proxy;

import eu.openanalytics.containerproxy.service.UserService;

public class MockedUserService extends UserService {
    public String getCurrentUserId() {
        return "jack";
    }
}
