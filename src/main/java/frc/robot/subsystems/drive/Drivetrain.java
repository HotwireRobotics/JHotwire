package frc.robot.subsystems.drive;

import static edu.wpi.first.units.Units.Inches;

import frc.robot.applicable.ctre.Drive;
import frc.robot.constants.Constants;

/**
 * <strong>Drive Subsystem</strong>
 * <p>Dependent subsystem for the CTR Electronics drivetrain 
 * components with added methods for better integration.
 * <p> This class should be used for game specific suppliers 
 * relating to the drivetrain or pose estimator.
 */
public class Drivetrain extends Drive {
  /**
   * Is the robot drivetrain located within the neutral zone?
   */
  private boolean isNeutralZone() {
    return (
      (getPose().getMeasureX().gt(Inches.of(180))) &&
      (getPose().getMeasureX().lt(Constants.middle.getMeasureX().times(2)
          .minus(Inches.of(180))))
    );
  }
  
  /**
   * Is the robot drivetrain located on the right side of the field?
   */
  private boolean isRightSide() {
    return ((getPose().getMeasureY().lt(Constants.middle.getMeasureY())));
  }

  public static enum Zone {
    RED, BLUE,
    NEUTRAL
  }

  public static enum Side {
    RIGHT, LEFT
  }

  /**
   * Which side is the robot located within?
   */
  public Side getSide() {
    return (isRightSide() ? Side.RIGHT : Side.LEFT);
  }

  /**
   * Which zone is the robot located within?
   */
  public Zone getZone() {
    if (isNeutralZone()) return Zone.NEUTRAL;
    return (getPose().getMeasureX().lt(Constants.middle.getMeasureX()) ? Zone.BLUE : Zone.RED);
  }

}
