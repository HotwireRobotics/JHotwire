package frc.robot.subsystems.actuator;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.Radians;
import static edu.wpi.first.units.Units.Rotations;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Mode;
import frc.robot.hotwire.Logs;
import frc.robot.hotwire.StateManager;
import frc.robot.subsystems.actuator.ActuatorIO.ActuatorInputs;
import frc.robot.subsystems.motors.Motor;
import frc.robot.subsystems.motors.Motor.Application;
import frc.robot.subsystems.motors.Motor.Feedforward;
import frc.robot.subsystems.motors.MotorIO.Direction;
import frc.robot.subsystems.motors.MotorIO.FollowerMode;
import frc.robot.subsystems.motors.MotorIO.NeutralMode;

/**
 * <strong>Actuator Subsystem</strong>
 * <p>Deploys and retracts the intake by driving it linearly along a fixed
 * incline. Composed of two motors and a CANcoder: the right motor carries the
 * CANcoder and leads, the left motor follows it.
 */
public class Actuator extends SubsystemBase {

  // Subsystem abstraction.
  private final ActuatorIO io;
  private final ActuatorInputs inputs;

  // State system.
  public enum State {
    EXTENDED,
    RETRACTED
  }
  /** Subsystem state. */
  public final StateManager<State> manager = new StateManager<State>(
    getName(), State.EXTENDED
  );

  // Initialize device representatives.
  /** Right actuator motor; carries the CANcoder and leads. */
  final Motor right;
  /** Left actuator motor; follows the right motor. */
  final Motor left;

  // Test toggle for commanding the actuator in/out; defaults to extended.
  private boolean test = true;

  public Actuator(
    Trigger trigger
  ) {
    // Initialize abstraction.
    io = Constants.mode.equals(Mode.SIM)
      ? new Simulation()
      : new Clypeus();
    this.inputs = new ActuatorInputs();

    // Configure devices.
    Application configuration = new Application(Direction.FORWARD, NeutralMode.BRAKE, Amps.of(40));
    right = new Motor(this, Constants.MotorIDs.ACTUATOR_RIGHT);
    right.apply(configuration);
    right.apply(new Feedforward(Constants.Actuator.kP, 0, 0));
    left = new Motor(this, Constants.MotorIDs.ACTUATOR_LEFT);
    left.apply(configuration);

    // The left motor mirrors the leader.
    left.follow(right, FollowerMode.ALIGNED);

    // Triggers.
    trigger.onTrue(Commands.runOnce(this::toggle));
  }

  @Override
  public void periodic() {
    // Update subsystem inputs.
    io.updateInputs(inputs);

    // Drive the leader (the follower tracks it) toward the active target.
    Angle target = test ? Constants.Actuator.kExtended : Constants.Actuator.kRetracted;
    right.putPosition(target);
    io.setTarget(target);

    // Log device and derived state.
    Logs.log(right);
    Logger.recordOutput("Actuator/Position", inputs.position);
    Logger.recordOutput("Actuator/Extension", getExtension());
  }

  /**
   * Toggle the actuator between its extended and retracted states. Takes effect
   * immediately; {@code periodic()} drives the mechanism to the new target.
   */
  public void toggle() {
    if (test) retract(); else extend();
  }

  /**
   * Extend the actuator.
   */
  public void extend() {
    test = true;
    manager.set(State.EXTENDED);
  }

  /**
   * Retract the actuator.
   */
  public void retract() {
    test = false;
    manager.set(State.RETRACTED);
  }

  /**
   * Fraction of full extension, in [0, 1], derived from the measured CANcoder
   * position. This is smoothly interpolated in simulation.
   *
   * @return extension fraction.
   */
  public double getExtension() {
    double range = Constants.Actuator.kExtended
      .minus(Constants.Actuator.kRetracted).in(Rotations);
    double travelled = inputs.position - Constants.Actuator.kRetracted.in(Rotations);
    return MathUtil.clamp(travelled / range, 0, 1);
  }

  /**
   * Linear displacement of the intake caused by the current actuator extension.
   * The intake travels {@link Constants.Actuator#kTravel} along a
   * {@link Constants.Actuator#kAngle} incline, moving forward and downward.
   *
   * @return intake displacement, in the robot frame.
   */
  public Translation3d getDisplacement() {
    double distance = Constants.Actuator.kTravel.in(Meters) * getExtension();
    double angle = Constants.Actuator.kAngle.in(Radians);
    return new Translation3d(
      distance * Math.cos(angle),
      0,
      -distance * Math.sin(angle));
  }
}
