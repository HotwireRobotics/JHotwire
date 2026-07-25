package frc.robot.subsystems.actuator;

import static edu.wpi.first.units.Units.Rotations;

import edu.wpi.first.units.measure.Angle;
import frc.robot.constants.Constants;

/**
 * Simulated actuator IO. The real mechanism drives a CANcoder toward a target
 * with a motor; here we emulate that by exponentially interpolating the reported
 * position toward the commanded target every loop, giving smooth, life-like
 * motion between the retracted and extended setpoints.
 */
public class Simulation implements ActuatorIO {

  // Simulated CANcoder state (rotations). Defaults to extended.
  private double position = Constants.Actuator.kRetracted.in(Rotations);
  private double target =   Constants.Actuator.kExtended.in(Rotations);

  public Simulation() {}

  @Override
  public void setTarget(Angle target) {
    this.target = target.in(Rotations);
  }

  @Override
  public void updateInputs(ActuatorInputs inputs) {
    // Step the simulated position toward the target with exponential smoothing.
    double previous = position;
    position += (target - position) * Constants.Actuator.kSmoothing;

    inputs.position = position;
    inputs.velocity = (position - previous) / 0.02;
  }
}
