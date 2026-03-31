package frc.robot.subsystems.shooter;

import static edu.wpi.first.units.Units.Amps;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Mode;
import frc.robot.hotwire.StateManager;
import frc.robot.subsystems.shooter.ShooterIO.ShooterInputs;
import frc.robot.subsystems.motors.Motor;
import frc.robot.subsystems.motors.Motor.Application;
import frc.robot.subsystems.motors.MotorIO.Direction;
import frc.robot.subsystems.motors.MotorIO.NeutralMode;

public class Shooter extends SubsystemBase {
    
  // Subsystem abstraction.
  private final ShooterIO io;
  private final ShooterInputs inputs;

  // State system.
  public enum State {
    SHOOTING,
    STOPPED,
  }
  /** Subsystem state. */
  public final StateManager<State> manager = new StateManager<State>(
    getName(), State.STOPPED
  );
  
  // Initialize device representatives.
  /** Feeder rollers. */
  final Motor feeder;
  /** Shooting rollers. */
  final Motor shooter;
  /** Primary shooter. */
  final Motor primary;
  /** Secondary shooter. */
  final Motor secondary;

  // Shooting array.
  final Motor[] rollers = {
    
  };

  public Shooter() {
    // Initialize abstraction.
    io = Constants.mode.equals(Mode.SIM) 
      ? new Simulated() 
      : new Truthful();
    this.inputs = new ShooterInputs();

    // Configure devices.
    feeder = new Motor(this, Constants.MotorIDs.ROLLERS);
    feeder.apply(
      new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(40)));
  }
}
