package uk.ac.ed.inf.eventsapp.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for text-user-interface helper behaviour.
 */
public class TextUserInterfaceTests {
  @Test
  void commonExitSequencesAreRecognised() {
    assertTrue(TextUserInterface.isExitSequence(":q"),
        "The Vim-style :q sequence should request application exit.");
    assertTrue(TextUserInterface.isExitSequence(":quit"),
        "The prefixed :quit sequence should request application exit.");
    assertTrue(TextUserInterface.isExitSequence(" :exit "),
        "Exit detection should ignore surrounding whitespace.");
  }

  @Test
  void normalInputIsNotTreatedAsExitSequence() {
    assertFalse(TextUserInterface.isExitSequence("1"),
        "Ordinary menu input should not be treated as an exit sequence.");
    assertFalse(TextUserInterface.isExitSequence("quit"),
        "Unprefixed quit should not be treated as an exit sequence.");
    assertFalse(TextUserInterface.isExitSequence("student@ed.ac.uk"),
        "Normal user input should not be treated as an exit sequence.");
  }
}
