package frc.robot.subsystems.drive;

import static edu.wpi.first.units.Units.Inches;
import static edu.wpi.first.units.Units.Radians;

import java.util.function.Supplier;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.applicable.ctre.Drive;
import frc.robot.applicable.ctre.DriveCommands;
import frc.robot.constants.Constants;

/**
 * <strong>Drive Subsystem</strong>
 * <p>Dependent subsystem for the CTR Electronics drivetrain 
 * components with added methods for better integration.
 * <p> This class should be used for game specific suppliers 
 * relating to the drivetrain or pose estimator.
 */
public class Drivetrain extends Drive {

  public Drivetrain(
    Trigger trigger
  ) {
    super();

    // Triggers.
    trigger
      .whileTrue(firingOrientation());
  }

  
  /** Returns the Rotation2d the robot needs to face the hub. */
  private Rotation2d calculateHubRotation() {
    // Get poses.
    Pose2d robotPose = getPose();
    Pose2d hubPose = Constants.Poses.hub.getPose();

    // Pose differences.
    double dx = hubPose.getX() - robotPose.getX();
    double dy = hubPose.getY() - robotPose.getY();

    // Angle from robot to hub
    Rotation2d rotation = new Rotation2d(
        Radians.of(Math.IEEEremainder(
            Math.atan2(dy, dx), 
            Constants.Mathematics.TAU))).rotateBy(Rotation2d.k180deg);

    // Log the pointer
    Pose2d pointer = new Pose2d(robotPose.getX(), robotPose.getY(), rotation);
    Logger.recordOutput("Hub Pointer", pointer);

    // Update drive target.
    setRotationTarget(rotation);

    return getRotationTarget();
  }

  private Rotation2d calculatePassingRotation() {
    // Get poses.
    Pose2d robotPose = getPose();
    Pose2d pointer = Constants.Poses.pointer.getPose();

    pointer = (getSide().equals(Side.RIGHT)) ? pointer : Constants.mirror(pointer);
    
    // Pose differences.
    double dx = pointer.getX() - robotPose.getX();
    double dy = pointer.getY() - robotPose.getY();

    // Angle from robot to hub
    Angle toPass = (Radians.of(Math.IEEEremainder(Math.atan2(dy, dx), Constants.Mathematics.TAU)));
    
    // Update drive target.
    setRotationTarget(new Rotation2d(toPass));

    return getRotationTarget();
  }


  /**
   * Orient the robot to face a supplied angle.
   *
   * @param rotation
   */
  private Command pointToAngle(Supplier<Rotation2d> rotation) {
    return DriveCommands.joystickDriveAtAngle(
        this,
        () -> -Constants.Joysticks.driver.getLeftY(),
        () -> -Constants.Joysticks.driver.getLeftX(),
        rotation);
  }
  
  /** Orient robot to face the hub. */
  private Command firingOrientation() {
    return Commands.either(
      pointToAngle(this::calculateHubRotation),
      pointToAngle(this::calculatePassingRotation),
      () -> getZone().equals(
        Constants.getAlliance()
          .equals(Alliance.Blue) 
            ? Zone.BLUE 
            : Zone.RED));
  }

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
