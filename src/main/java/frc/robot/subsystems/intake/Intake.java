package frc.robot.subsystems.intake;

import frc.robot.subsystems.motors.MotorIO.*;
import frc.robot.subsystems.motors.Motor.Application;

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
import frc.robot.subsystems.intake.IntakeIO.IntakeInputs;
import frc.robot.subsystems.motors.Motor;
import frc.robot.subsystems.vision.VisionIO.VisionInputs;

/**
 * <strong>Intake Subsystem</strong>
 * <p>Subsystem for controlling intake roller
 * motion.
 */
public class Intake extends SubsystemBase {
  
  // Subsystem abstraction.
  private final IntakeIO io;
  private final IntakeInputs inputs;

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
  final Motor rollers;

  public Intake(
    Trigger trigger
  ) {
    // Initialize abstraction.
    io = Constants.mode.equals(Mode.SIM) 
      ? new Simulation() 
      : new Articulate();
    this.inputs = new IntakeInputs();

    // Configure devices.
    rollers = new Motor(this, Constants.MotorIDs.ROLLERS);
    rollers.apply(
      new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(40)));
    
    // Triggers.
    trigger
      .whileTrue(runVelocity(Constants.Intake.kSpeed))
      .onFalse(runHalt());
  }

  @Override
  public void periodic() {
    // Update subsystem inputs.
    io.updateInputs(inputs);
    Logs.log(rollers);
  }

  /** 
   * Run intake rollers at velocity. 
   * 
   * @param velocity Angular velocity to run rollers at.
   */
  public Command runVelocity(AngularVelocity velocity) {
    return rollers.runVelocity(velocity).alongWith(
      manager.tag(() -> (velocity.gt(RPM.of(0)) 
        ? State.FORWARD 
        : State.REVERSE)
      ));
  }

  /** 
   * Halt intake rollers. 
   */
  public Command runHalt() {
    return rollers.runPercent(0).alongWith(
      manager.tag(() -> State.STOPPED));
  }
}