package hk.edu.polyu.comp.vlabcontroller.util;

import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import lombok.Synchronized;

import java.util.*;

public class PortAllocator {

    private final int[] range;
    private final Set<Integer> occupiedPorts;
    private final Map<Integer, String> occupiedPortOwners;

    public PortAllocator(int from, int to) {
        range = new int[]{from, to};
        occupiedPorts = Collections.synchronizedSet(new HashSet<>());
        occupiedPortOwners = Collections.synchronizedMap(new HashMap<>());
    }

    public int allocate(String ownerId) {
        var nextPort = range[0];
        while (occupiedPorts.contains(nextPort)) nextPort++;

        if (range[1] > 0 && nextPort > range[1]) {
            throw new VLabControllerException("Cannot create container: all allocated ports are currently in use."
                + " Please try again later or contact an administrator.");
        }

        occupiedPorts.add(nextPort);
        occupiedPortOwners.put(nextPort, ownerId);
        return nextPort;
    }

    public void release(int port) {
        occupiedPorts.remove(port);
        occupiedPortOwners.remove(port);
    }

    @Synchronized("occupiedPortOwners")
    public void release(String ownerId) {
        occupiedPortOwners.entrySet().stream()
            .filter(e -> e.getValue().equals(ownerId))
            .map(Map.Entry::getKey).distinct().forEach(this::release);
    }
}
