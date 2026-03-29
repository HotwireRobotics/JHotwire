package frc.robot;

import org.littletonrobotics.junction.Logger;

public class StateManager<T extends Enum<T>> {

  private T state;
  private final String name;

  public StateManager(String name, T defaultState) {
    this.name = name;
    this.state = defaultState;
  }

  public void set(T newState) {
    if (newState != state) {
      state = newState;
      Logger.recordOutput(name + "/State", state.toString());
    }
  }

  public T get() {
    return state;
  }
}