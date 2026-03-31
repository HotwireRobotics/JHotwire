package frc.robot.subsystems.intake;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Mode;
import frc.robot.hotwire.Logs;
import frc.robot.hotwire.StateManager;
import frc.robot.subsystems.intake.IntakeIO.IntakeInputs;
import frc.robot.subsystems.motors.Motor;
import frc.robot.subsystems.vision.VisionIO.VisionInputs;

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
  final Motor rollers;

  public Intake() {
    // Initialize abstraction.
    io = Constants.mode.equals(Mode.SIM) 
      ? new Simulated() 
      : new Truthful();
    this.inputs = new IntakeInputs();

    rollers = new Motor(this, Constants.MotorIDs.ROLLERS);
    rollers.runPercent(0.7);
  }

  @Override
  public void periodic() {
    // Update subsystem inputs.
    io.updateInputs(inputs);
    Logs.log(rollers);
  }
}
