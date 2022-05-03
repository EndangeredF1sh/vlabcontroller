package hk.edu.polyu.comp.vlabcontroller.controllers;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.service.HeartbeatService;
import hk.edu.polyu.comp.vlabcontroller.util.DurationUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DurationUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class AdminController extends BaseController {
    private final HeartbeatService heartbeatService;

    @RequestMapping("/admin")
    private String admin(ModelMap map, HttpServletRequest request) {
        prepareMap(map, request);

        var websocketHeartbeats = heartbeatService.getWebsocketHeartbeats();
        var heartbeatRate = proxyProperties.getHeartbeatRate();

        map.put("proxies", proxyService.getProxies(null, false));
        map.put("proxyUptimes", getUptimes(proxyService.getProxies(null, false)));
        map.put("lastActive", heartbeatService.getProxyHeartbeats().entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, x -> DurationUtil.max(x.getValue(),
                Optional.ofNullable(websocketHeartbeats.get(x.getKey()))
                    .map(hbs -> hbs.getLastRecordTimestamp().minus(heartbeatRate.multipliedBy(hbs.getTerminateCounter())))
                    .orElse(Duration.ofMillis(1L).negated()))
            )));

        return "admin";
    }

    static Map<String, String> getUptimes(List<Proxy> proxies) {
        return proxies.stream()
            .collect(Collectors.toMap(Proxy::getId, proxy -> {
                // if the proxy hasn't started up yet, the uptime should be zero
                var uptime = proxy.getStartupTimestamp();
                if (DurationUtils.isPositive(uptime)) {
                    uptime = Duration.ofMillis(System.currentTimeMillis()).minus(uptime);
                }
                return String.format("%d:%02d:%02d", uptime.toHours(), uptime.toMinutesPart(), uptime.toSecondsPart());
            }));
    }
}
