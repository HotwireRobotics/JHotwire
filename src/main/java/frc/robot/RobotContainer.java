package frc.robot;

import static edu.wpi.first.units.Units.*;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.commands.PathPlannerAuto;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import frc.robot.applicable.ctre.DriveCommands;
import frc.robot.applicable.simulation.Handler;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Mode;
import frc.robot.subsystems.drive.Drivetrain;
import frc.robot.subsystems.vision.Vision;

import java.util.function.Supplier;

import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

public class RobotContainer {
  // Declare subsystems.
  public final Drivetrain drive;
  public final Vision vision;

  // Simualtion
  public final Handler simulation;

  // Dashboard inputs
  private final LoggedDashboardChooser<Command> autoChooser;
  private final LoggedDashboardChooser<Boolean> localization;
  private final LoggedDashboardChooser<Boolean> alignment;

  public RobotContainer() {
    // Initialize subsystems.
    drive = new Drivetrain();
    vision = new Vision(
      drive::getPose, drive::getRotation,
      drive::addVisionMeasurement);

    // Initialize simulation.
    if (Constants.mode.equals(Mode.SIM)) simulation = new Handler(
      () -> RPM.of(1400), () -> true, () -> true, () -> Degrees.of(0),
      drive::getPose, drive::getChassisSpeeds
    ); else simulation = new Handler(null, null, null, null, null, null);

    // Configure button bindings.
    configureButtonBindings();

    // Configure dashboard inputs.
    alignment = new LoggedDashboardChooser<>("Dashboard/alignment", new SendableChooser<Boolean>());
    alignment.addDefaultOption("Required", true);
    alignment.addOption("Supersede",      false);
    // TODO: Add on-change method for alignment requirement.

    localization = new LoggedDashboardChooser<>("Dashboard/localization", new SendableChooser<Boolean>());
    localization.addDefaultOption("Enabled", true);
    localization.addOption("Disabled",      false);
    localization.onChange(v -> vision.setEnabled(v));

    // Declare pathplanner events.
    final Command stopDrive = Commands.runOnce(() -> drive.stop());
    final Command lockDrive = Commands.runOnce(() -> drive.stopWithX());

    // Autonomous
    if (!Constants.mode.equals(Mode.COMPETITION)) {

      // Create autonomous selector and add options.
      autoChooser = new LoggedDashboardChooser<>("Auto Choices", new SendableChooser<Command>()); // new SendableChooser<Command>()
      
      // Drivetrain characterization routines.
      autoChooser.addOption(
          "Drive Wheel Radius Characterization", DriveCommands.wheelRadiusCharacterization(drive));
      autoChooser.addOption(
          "Drive Simple FF Characterization", DriveCommands.feedforwardCharacterization(drive));
      autoChooser.addOption(
          "Drive SysId (Quasistatic Forward)",
          drive.sysIdQuasistatic(SysIdRoutine.Direction.kForward));
      autoChooser.addOption(
          "Drive SysId (Quasistatic Reverse)",
          drive.sysIdQuasistatic(SysIdRoutine.Direction.kReverse));
      autoChooser.addOption(
          "Drive SysId (Dynamic Forward)", drive.sysIdDynamic(SysIdRoutine.Direction.kForward));
      autoChooser.addOption(
          "Drive SysId (Dynamic Reverse)", drive.sysIdDynamic(SysIdRoutine.Direction.kReverse));

    } else {
      autoChooser = new LoggedDashboardChooser<>("Auto Choices", AutoBuilder.buildAutoChooser());
    }

    // Primary autonomous routine.
    autoChooser.addOption("A-Unineutral Right", new PathPlannerAuto("A-Unineutral", false));
    autoChooser.addOption("A-Unineutral Left", new PathPlannerAuto("A-Unineutral", true));
    
    autoChooser.addOption("A-Short-Unineutral Right", new PathPlannerAuto("A-Short-Unineutral", false));
    autoChooser.addOption("A-Short-Unineutral Left", new PathPlannerAuto("A-Short-Unineutral", true));

    // Tertiary autonomous routine.
    autoChooser.addOption("A-Shoot-Depot", new PathPlannerAuto("A-Shoot-Depot"));
  }

  /** Returns the Rotation2d the robot needs to face the hub. */
  private Rotation2d calculateHubRotation() {
    // Get poses.
    Pose2d robotPose = drive.getPose();
    Pose2d hubPose = Constants.Poses.hub.getPose();

    // Pose differences.
    double dx = hubPose.getX() - robotPose.getX();
    double dy = hubPose.getY() - robotPose.getY();

    // Angle from robot to hub
    Rotation2d rotation = new Rotation2d(
        Radians.of(Math.IEEEremainder(
            Math.atan2(dy, dx), 
            Constants.Mathematics.TAU)));

    // Log the pointer
    Pose2d pointer = new Pose2d(robotPose.getX(), robotPose.getY(), rotation);
    Logger.recordOutput("Hub Pointer", pointer);

    // Update drive target.
    drive.setRotationTarget(rotation);

    return drive.getRotationTarget();
  }

  private Rotation2d calculatePassingRotation() {
    // Get poses.
    Pose2d robotPose = drive.getPose();
    Pose2d pointer = Constants.Poses.pointer.getPose();

    // pointer = (drive.isRightSide()) ? pointer : Constants.mirror(pointer);
    
    // Pose differences.
    double dx = pointer.getX() - robotPose.getX();
    double dy = pointer.getY() - robotPose.getY();

    // Angle from robot to hub
    Angle toPass = (Radians.of(Math.IEEEremainder(Math.atan2(dy, dx), Constants.Mathematics.TAU)));
    
    // Update drive target.
    drive.setRotationTarget(new Rotation2d(toPass));

    return drive.getRotationTarget();
  }

  /** Orient robot to face the hub. */
  private Command firingOrientation() {
    // return drive.isNeutralZone() 
    //   ? pointToAngle(this::calculatePassingRotation)
    //   : pointToAngle(this::calculateHubRotation);
    return pointToAngle(this::calculateHubRotation);
  }

  /**
   * Orient the robot to face a supplied angle.
   *
   * @param rotation
   */
  private Command pointToAngle(Supplier<Rotation2d> rotation) {
    return DriveCommands.joystickDriveAtAngle(
        drive,
        () -> -Constants.Joysticks.driver.getLeftY(),
        () -> -Constants.Joysticks.driver.getLeftX(),
        rotation);
  }

  private void configureButtonBindings() {
    // Third person drive command.
    drive.setDefaultCommand(
        DriveCommands.joystickDrive(
            drive,
            () -> -Constants.Joysticks.driver.getLeftY(),
            () -> -Constants.Joysticks.driver.getLeftX(),
            () -> -Constants.Joysticks.driver.getRightX()));
  
    // Hold wheel position.
    Constants.Joysticks.driver.rightBumper().onTrue(Commands.runOnce(drive::stopWithX, drive));

    // Zero pose heading.
    Constants.Joysticks.driver
        .a()
        .onTrue(Commands.runOnce(
          () -> drive.setPose(new Pose2d(drive.getPose()
              .getTranslation(), Rotation2d.kZero)), drive)
              .ignoringDisable(true));
  }

  /**
   * Supplies the autonomous command selected on the dashboard.
   *
   * @return
   */
  public Command getAutonomousCommand() {
    return autoChooser.get();
  }

  /**
   * Set the robot's pose to the starting pose of the selected autonomous command, if it exists.
   *
   * @param autonomousCommand
   */
  public void seedAutonomousPose(Command autonomousCommand) {
    if (!(autonomousCommand instanceof PathPlannerAuto selectedAuto)) {
      return;
    }

    // Get autonomous starting pose.
    Pose2d startingPose = selectedAuto.getStartingPose();
    if (startingPose == null) {
      return;
    }

    drive.setPose(startingPose);
    Logger.recordOutput("AutoSeedPose", startingPose);
  }
}

// ./gradlew deploy --no-daemon
