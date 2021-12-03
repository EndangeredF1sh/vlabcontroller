package hk.edu.polyu.comp.vlabcontroller.backend.strategy;

/**
 * Defines a strategy for deciding what to do with a user's proxies when
 * the user logs out.
 */
public interface IProxyLogoutStrategy {

    void onLogout(String userId, boolean expired);

}
