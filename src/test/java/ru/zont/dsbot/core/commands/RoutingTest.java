package ru.zont.dsbot.core.commands;

import org.junit.jupiter.api.Test;

import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

class RoutingTest {

    @Test
    void routeVector() {
        Routing routing = new Routing()
                .addRoute("add").end()
                .addRoute("del").end()
                .addRoute("get").end();
        String vector = routing.routeVector();
        assertEquals("add|del|get", vector);
    }

    @Test
    void routeVector2d() {
        Routing routing = new Routing()
            .addRoute("kek")
                .addRoute("add").end()
                .addRoute("del").end()
                .addRoute("get").end().end()
            .addRoute("lol")
                .addRoute("add").end()
                .addRoute("del").end()
                .addRoute("get").end().end()
            .addRoute("zxc")
                .addRoute("add").end()
                .addRoute("del").end()
                .addRoute("get").end().end();
        String vector = routing.routeVector();
        assertTrue(vector.startsWith("kek|lol|zxc"), vector);
        assertTrue(vector.endsWith("..."), vector);
    }

    @Test
    void routeMap() {
        Routing routing = new Routing()
            .addRoute("kek")
                .addRoute("add").end()
                .addRoute("del").end()
                .addRoute("get").end().end()
            .addRoute("lol")
                .addRoute("add").end()
                .addRoute("del").end()
                .addRoute("get").end().end()
            .addRoute("zxc")
                .addRoute("add").end()
                .addRoute("del").end()
                .addRoute("get").end().end();
        Deque<String> map = routing.routeMap();
        assertEquals(4, map.size());
        for (String s : map) {
            //noinspection StringEquality
            if (map.getFirst() == s)
                assertTrue(map.getFirst().endsWith("..."));
            else assertTrue(s.endsWith("add|del|get"));
        }
    }
}