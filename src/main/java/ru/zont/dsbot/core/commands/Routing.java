package ru.zont.dsbot.core.commands;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Routing {
    private final LinkedList<Routing> routes = new LinkedList<>();
    private final String name;
    private final Routing parent;

    public Routing() {
        this("root");
    }

    public Routing(String name) {
        this(name, null);
    }

    public Routing(String name, Routing parent) {
        this.name = name;
        this.parent = parent;
    }

    public Routing addRoute(String name) {
        Routing routing = new Routing(name, this);
        routes.add(routing);
        return routing;
    }

    public Routing end() {
        if (parent == null) return this;
        return parent;
    }

    public boolean isEndpoint() {
        return routes.isEmpty();
    }

    public int getDepth() {
        if (isEndpoint()) return 0;
        int maxDepth = 0;
        for (Routing route : routes) {
            int depth = route.getDepth() + 1;
            if (depth > maxDepth)
                maxDepth = depth;
        }
        return maxDepth;
    }

    public String getName() {
        return name;
    }

    public String routeVector() {
        int depth = getDepth();
        if (depth == 0) return "";
        return routes.stream().map(Routing::getName).collect(Collectors.joining("|"))
               + (depth > 1 ? " ..." : "");
    }

    public Deque<String> routeMap() {
        return routeMap("");
    }

    private Deque<String> routeMap(String prefix) {
        LinkedList<String> list = new LinkedList<>();
        list.add(String.join(" ", prefix, routeVector()).trim());
        for (Routing route : routes)
            if (route.getDepth() > 0)
                list.addAll(route.routeMap(String.join(" ", prefix, route.name).trim()));
        return list;
    }
}
