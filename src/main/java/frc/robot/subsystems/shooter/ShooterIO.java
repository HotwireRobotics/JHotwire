package frc.robot.subsystems.shooter;

import org.littletonrobotics.junction.AutoLog;

public interface ShooterIO {
    
  // Define inputs for intake subsystem.
  @AutoLog
  public class ShooterInputs {
    // Generic logging.
  }

  /**
   * Collect inputs from all vision systems.
   */
  public void updateInputs(ShooterInputs inputs);
}
