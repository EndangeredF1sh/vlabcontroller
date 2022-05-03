package hk.edu.polyu.comp.vlabcontroller.service;

import hk.edu.polyu.comp.vlabcontroller.entity.LabInstance;
import hk.edu.polyu.comp.vlabcontroller.entity.SessionData;
import hk.edu.polyu.comp.vlabcontroller.entity.User;
import hk.edu.polyu.comp.vlabcontroller.event.ProxyStartEvent;
import hk.edu.polyu.comp.vlabcontroller.event.ProxyStopEvent;
import hk.edu.polyu.comp.vlabcontroller.event.UserLoginEvent;
import hk.edu.polyu.comp.vlabcontroller.event.UserLogoutEvent;
import hk.edu.polyu.comp.vlabcontroller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RefreshScope
@Component
@RequiredArgsConstructor
public class UserActionEventsListener {
    private final UserRepository repository;

    @EventListener
    public void onProxyStart(ProxyStartEvent event) {
        var time = new DateTime(event.getTimestamp());
        var user = this.repository.findUserByIdOrCreate(event.getUserId());
        var labs = user.getLabs();
        labs.stream().filter(x -> x.getId().equals(event.getProxyId())).findAny()
            .ifPresentOrElse(
                lab -> lab.setStartedAt(time),
                () -> labs.addFirst(LabInstance.builder().id(event.getProxyId()).startedAt(time).build())
            );
        this.repository.save(user);
    }

    @EventListener
    public void onProxyStop(ProxyStopEvent event) {
        var time = new DateTime(event.getTimestamp());
        User user = this.repository.findUserByIdOrCreate(event.getUserId());
        var labs = user.getLabs();
        labs.stream().filter(x -> x.getId().equals(event.getProxyId())).findAny()
            .ifPresentOrElse(
                lab -> lab.setStartedAt(time),
                () -> labs.addFirst(LabInstance.builder().id(event.getProxyId()).completedAt(time).build())
            );
        this.repository.save(user);
    }

    @EventListener
    public void onUserLogin(UserLoginEvent event) {
        var time = new DateTime(event.getTimestamp());
        var user = this.repository.findUserByIdOrCreate(event.getUserId());
        var sessions = user.getSession();
        Optional.ofNullable(sessions.get(event.getSessionId()))
            .ifPresentOrElse(
                session -> session.setLoggedInAt(time),
                () -> sessions.put(event.getSessionId(), SessionData.builder().loggedInAt(time).build())
            );
        this.repository.save(user);
    }

    @EventListener
    public void onUserLogout(UserLogoutEvent event) {
        var time = new DateTime(event.getTimestamp());
        var user = this.repository.findUserByIdOrCreate(event.getUserId());
        var sessions = user.getSession();
        Optional.ofNullable(sessions.get(event.getSessionId()))
            .ifPresentOrElse(
                session -> session.setLoggedOutAt(time),
                () -> sessions.put(event.getSessionId(), SessionData.builder().loggedOutAt(time).build())
            );
        this.repository.save(user);
    }
}