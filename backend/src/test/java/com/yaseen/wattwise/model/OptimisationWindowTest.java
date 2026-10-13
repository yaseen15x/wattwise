package com.yaseen.wattwise.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimisationWindowTest {

    @Test
    void acceptsAnApplianceThatFinishesExactlyAtTheDeadline() {
        OptimisationWindow window = new OptimisationWindow(360, 600);

        assertTrue(window.canFit(240));
    }

    @Test
    void rejectsAnApplianceThatWouldExceedTheWindow() {
        OptimisationWindow window = new OptimisationWindow(360, 600);

        assertFalse(window.canFit(241));
    }

    @Test
    void rejectsZeroDuration() {
        OptimisationWindow window = new OptimisationWindow(360, 600);

        assertFalse(window.canFit(0));
    }

    @Test
    void rejectsADeadlineBeforeTheStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OptimisationWindow(600, 360)
        );
    }
}