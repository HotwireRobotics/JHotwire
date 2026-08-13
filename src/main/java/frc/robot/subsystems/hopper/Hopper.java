package frc.robot.subsystems.hopper;

import frc.robot.subsystems.motors.MotorIO.*;
import frc.robot.subsystems.motors.Motor.Application;
import frc.robot.subsystems.motors.Motor.Feedforward;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.RPM;

import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Mode;
import frc.robot.hotwire.Logs;
import frc.robot.hotwire.StateManager;
import frc.robot.subsystems.hopper.HopperIO.HopperInputs;
import frc.robot.subsystems.intake.IntakeIO.IntakeInputs;
import frc.robot.subsystems.motors.Motor;
import frc.robot.subsystems.vision.VisionIO.VisionInputs;

/**
 * <strong>Intake Subsystem</strong>
 * <p>Subsystem for controlling intake roller
 * motion.
 */
public class Hopper extends SubsystemBase {
  
  // Subsystem abstraction.
  private final HopperIO io;
  private final HopperInputs inputs;

  // State system.
  public enum State {
    FORWARD,
    REVERSE,
    STOPPED
  }
  /** Subsystem state. */
  public final StateManager<State> manager = new StateManager<State>(
    getName(), State.STOPPED
  );

  // Initialize device representatives.
  /** Intake rollers. */
  final Motor hopper;

  public Hopper(
    Trigger trigger
  ) {
    // Initialize abstraction.
    io = Constants.mode.equals(Mode.SIM) 
      ? new Simulation() 
      : new Belly();
    this.inputs = new HopperInputs();

    // Configure devices.
    hopper = new Motor(this, Constants.MotorIDs.HOPPER);
    hopper.apply(
      new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(40)));
    hopper.apply(new Feedforward(1, 0, 0));
    
    // Triggers.
    trigger
      .whileTrue(runVelocity(Constants.Hopper.kSpeed))
      .onFalse(runHalt());
  }

  @Override
  public void periodic() {
    // Update subsystem inputs.
    io.updateInputs(inputs);
    Logs.log(hopper);
  }

  /** 
   * Run intake rollers at velocity. 
   * 
   * @param velocity Angular velocity to run rollers at.
   */
  private Command runVelocity(AngularVelocity velocity) {
    return hopper.runVelocity(velocity).alongWith(
      manager.tag(() -> (velocity.gt(RPM.of(0)) 
        ? State.FORWARD 
        : State.REVERSE)
      ));
  }

  /** 
   * Halt intake rollers. 
   */
  private Command runHalt() {
    return hopper.runPercent(0).alongWith(
      manager.tag(State.STOPPED));
  }

  /**
   * Run intake rollers.
   */
  public Command run() {
    return runVelocity(Constants.Intake.kSpeed);
  }

  /**
   * Stop intake rollers.
   */
  public Command stop() {
    return runHalt();
  }
}