package uk.ac.ed.inf.eventsapp.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Student preferences from the UML diagram.
 */
public class StudentPreferences {
  private boolean preferMusicEvents;
  private boolean preferTheaterEvents;
  private boolean preferDanceEvents;
  private boolean preferMovieEvents;
  private boolean preferSportsEvents;

  /** Creates an empty preference set. */
  public StudentPreferences() {}

  /**
   * Creates a preference set with explicit values for each supported event type.
   *
   * @param preferMusicEvents whether music events are preferred
   * @param preferTheaterEvents whether theatre events are preferred
   * @param preferDanceEvents whether dance events are preferred
   * @param preferMovieEvents whether movie events are preferred
   * @param preferSportsEvents whether sports events are preferred
   */
  public StudentPreferences(boolean preferMusicEvents, boolean preferTheaterEvents,
      boolean preferDanceEvents, boolean preferMovieEvents, boolean preferSportsEvents) {
    this.preferMusicEvents = preferMusicEvents;
    this.preferTheaterEvents = preferTheaterEvents;
    this.preferDanceEvents = preferDanceEvents;
    this.preferMovieEvents = preferMovieEvents;
    this.preferSportsEvents = preferSportsEvents;
  }

  /**
   * Replaces the stored preferences using a comma-separated list of event types.
   *
   * @param studentRawStringPreferences the raw user input containing zero to three event types
   * @return {@code true} if the preferences were updated successfully, otherwise {@code false}
   */
  public boolean updatePreferences(String studentRawStringPreferences) {
    if (studentRawStringPreferences == null) {
      return false;
    }

    clearPreferences();
    String trimmedPreferences = studentRawStringPreferences.trim();
    if (trimmedPreferences.isEmpty()) {
      return true;
    }

    String[] rawPreferences = trimmedPreferences.split(",");
    if (rawPreferences.length > 3) {
      return false;
    }

    Set<String> seenPreferences = new HashSet<>();
    for (String rawPreference : rawPreferences) {
      String normalisedPreference = rawPreference.trim().toUpperCase().replace(' ', '_');
      if (normalisedPreference.isEmpty() || !seenPreferences.add(normalisedPreference)) {
        clearPreferences();
        return false;
      }

      if ("THEATER".equals(normalisedPreference)) {
        normalisedPreference = "THEATRE";
      }

      switch (normalisedPreference) {
        case "MUSIC" -> preferMusicEvents = true;
        case "THEATRE" -> preferTheaterEvents = true;
        case "DANCE" -> preferDanceEvents = true;
        case "MOVIE" -> preferMovieEvents = true;
        case "SPORTS" -> preferSportsEvents = true;
        default -> {
          clearPreferences();
          return false;
        }
      }
    }

    return true;
  }

  private void clearPreferences() {
    preferMusicEvents = false;
    preferTheaterEvents = false;
    preferDanceEvents = false;
    preferMovieEvents = false;
    preferSportsEvents = false;
  }

  /** @return whether music events are preferred */
  public boolean isPreferMusicEvents() {
    return preferMusicEvents;
  }

  /** @return whether theatre events are preferred */
  public boolean isPreferTheaterEvents() {
    return preferTheaterEvents;
  }

  /** @return whether dance events are preferred */
  public boolean isPreferDanceEvents() {
    return preferDanceEvents;
  }

  /** @return whether movie events are preferred */
  public boolean isPreferMovieEvents() {
    return preferMovieEvents;
  }

  /** @return whether sports events are preferred */
  public boolean isPreferSportsEvents() {
    return preferSportsEvents;
  }
}
