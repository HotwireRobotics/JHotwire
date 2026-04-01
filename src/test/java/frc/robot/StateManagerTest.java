package frc.robot;

import static org.junit.jupiter.api.Assertions.*;

import frc.robot.hotwire.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the generic StateManager class that handles subsystem state transitions.
 */
class StateManagerTest {

  /** Test enum for StateManager tests. */
  private enum TestState {
    IDLE,
    RUNNING,
    STOPPED,
    ERROR
  }

  private StateManager<TestState> stateManager;

  @BeforeEach
  void setUp() {
    stateManager = new StateManager<>("TestSubsystem", TestState.IDLE);
  }

  @Nested
  @DisplayName("Initialization Tests")
  class InitializationTests {

    @Test
    @DisplayName("Should initialize with default state")
    void shouldInitializeWithDefaultState() {
      assertEquals(TestState.IDLE, stateManager.get());
    }

    @Test
    @DisplayName("Should initialize with custom default state")
    void shouldInitializeWithCustomDefaultState() {
      StateManager<TestState> customManager = new StateManager<>("Custom", TestState.RUNNING);
      assertEquals(TestState.RUNNING, customManager.get());
    }

    @Test
    @DisplayName("Should handle all enum values as default state")
    void shouldHandleAllEnumValuesAsDefaultState() {
      for (TestState state : TestState.values()) {
        StateManager<TestState> manager = new StateManager<>("Test", state);
        assertEquals(state, manager.get());
      }
    }
  }

  @Nested
  @DisplayName("State Transition Tests")
  class StateTransitionTests {

    @Test
    @DisplayName("Should transition from default to new state")
    void shouldTransitionFromDefaultToNewState() {
      stateManager.set(TestState.RUNNING);
      assertEquals(TestState.RUNNING, stateManager.get());
    }

    @Test
    @DisplayName("Should transition through multiple states")
    void shouldTransitionThroughMultipleStates() {
      stateManager.set(TestState.RUNNING);
      assertEquals(TestState.RUNNING, stateManager.get());

      stateManager.set(TestState.STOPPED);
      assertEquals(TestState.STOPPED, stateManager.get());

      stateManager.set(TestState.ERROR);
      assertEquals(TestState.ERROR, stateManager.get());
    }

    @Test
    @DisplayName("Should handle setting same state")
    void shouldHandleSettingSameState() {
      stateManager.set(TestState.IDLE);
      assertEquals(TestState.IDLE, stateManager.get());

      // Set to same state multiple times
      stateManager.set(TestState.IDLE);
      stateManager.set(TestState.IDLE);
      assertEquals(TestState.IDLE, stateManager.get());
    }

    @Test
    @DisplayName("Should return to initial state after transitions")
    void shouldReturnToInitialStateAfterTransitions() {
      stateManager.set(TestState.RUNNING);
      stateManager.set(TestState.STOPPED);
      stateManager.set(TestState.IDLE);
      assertEquals(TestState.IDLE, stateManager.get());
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle rapid state changes")
    void shouldHandleRapidStateChanges() {
      for (int i = 0; i < 1000; i++) {
        TestState state = TestState.values()[i % TestState.values().length];
        stateManager.set(state);
        assertEquals(state, stateManager.get());
      }
    }

    @Test
    @DisplayName("Get should not modify state")
    void getShouldNotModifyState() {
      TestState initial = stateManager.get();
      for (int i = 0; i < 100; i++) {
        stateManager.get();
      }
      assertEquals(initial, stateManager.get());
    }
  }
}
