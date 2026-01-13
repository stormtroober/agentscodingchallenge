package org.example;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppTest {
    @Test
    public void appExists() {
        try {
            Class.forName("org.example.App");
        } catch (ClassNotFoundException e) {
            fail("App class should exist");
        }
    }
}
