package frc.robot.subsystems.actuator;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.hardware.CANcoder;

import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import frc.robot.constants.Constants;

/**
 * Real actuator IO. Reads the CANcoder carried by the right (leader) motor; the
 * motors themselves are owned by the {@link Actuator} subsystem and close their
 * loop on this sensor.
 */
public class Clypeus implements ActuatorIO {

  // Position feedback device on the leader motor.
  private final CANcoder encoder;
  private final StatusSignal<Angle> position;
  private final StatusSignal<AngularVelocity> velocity;

  public Clypeus() {
    encoder = new CANcoder(Constants.MotorIDs.ACTUATOR_ENCODER);
    position = encoder.getAbsolutePosition();
    velocity = encoder.getVelocity();
  }

  @Override
  public void setTarget(Angle target) {}

  @Override
  public void updateInputs(ActuatorInputs inputs) {
    BaseStatusSignal.refreshAll(position, velocity);
    inputs.position = position.getValueAsDouble();
    inputs.velocity = velocity.getValueAsDouble();
  }
}
