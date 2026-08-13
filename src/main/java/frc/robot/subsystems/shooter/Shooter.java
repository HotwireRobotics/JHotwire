package frc.robot.subsystems.shooter;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Inches;
import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.RPM;
import static edu.wpi.first.units.Units.RotationsPerSecond;

import java.util.Arrays;

import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Mode;
import frc.robot.hotwire.StateManager;
import frc.robot.subsystems.shooter.ShooterIO.ShooterInputs;
import frc.robot.subsystems.motors.Motor;
import frc.robot.subsystems.motors.Motor.Application;
import frc.robot.subsystems.motors.Motor.Feedback;
import frc.robot.subsystems.motors.Motor.Feedforward;
import frc.robot.subsystems.motors.MotorIO.Direction;
import frc.robot.subsystems.motors.MotorIO.FollowerMode;
import frc.robot.subsystems.motors.MotorIO.NeutralMode;
import frc.robot.subsystems.shooter.WideShooter;

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
  /** Tertiary shooter. */
  final Motor tertiary;
  /** Quaternary shooter. */
  final Motor quaternary;

  // Shooting array.
  final Motor[] shooting;

  public Shooter(
    Trigger trigger
  ) {
    // Initialize abstraction.
    io = Constants.mode.equals(Mode.SIM) 
      ? new Simulation() 
      : new WideShooter();
    this.inputs = new ShooterInputs();

    Feedforward feedforward = new Feedforward(10, 0, 0);

    // Configure devices.
    feeder = new Motor(this, Constants.MotorIDs.FEEDER);
    feeder.apply(
      new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(40)));
    feeder.apply(
      feedforward);
      shooter = new Motor(this, Constants.MotorIDs.SHOOTER);
    shooter.apply(
      new Application(Direction.REVERSE, NeutralMode.COAST, Amps.of(40)));
    shooter.apply(
      feedforward);
    
    // Configure shooter motors with identical settings. 
    Application forward = new Application(Direction.REVERSE, NeutralMode.COAST, Amps.of(40));
    Application reverse = new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(40));
    primary    = new Motor(this, Constants.MotorIDs.PRIMARY); 
    primary.apply(
      forward);
    primary.apply(
      feedforward);
    secondary  = new Motor(this, Constants.MotorIDs.SECONDARY); 
    secondary.apply(
      reverse);
    secondary.apply(
      feedforward);
    tertiary   = new Motor(this, Constants.MotorIDs.TERTIARY); 
    tertiary.apply(
      reverse);
    tertiary.apply(
      feedforward);
    quaternary = new Motor(this, Constants.MotorIDs.QUATERNARY); 
    quaternary.apply(
      forward); 
    quaternary.apply(
      feedforward);

    // Set follower control.
    secondary.follow(primary, FollowerMode.ALIGNED);
    tertiary.follow(primary,  FollowerMode.INVERSE);
    quaternary.follow(primary, FollowerMode.ALIGNED);

    // Join all devices in a list for iteration.
    shooting = new Motor[] {
      feeder, shooter, primary, secondary, tertiary, quaternary};

    // Triggers.
    trigger
      .whileTrue(runVelocity(Constants.Shooter.kSpeed))
      .onFalse(runHalt());
  }

  @Override
  public void periodic() {
    // Update subsystem inputs.
    io.updateInputs(inputs);
  }

  /**
   * Run velocity control on shooter motors.
   * 
   * @param velocity Target velocity.
   */
  private Command runVelocity(AngularVelocity velocity) {
    // Stream shooting commands.
    return Commands.parallel(Arrays.stream(shooting)
      .map(motor -> motor.runVelocity(velocity))
      .toArray(Command[]::new))
        .alongWith(manager.tag(State.SHOOTING));
  }

  /**
   * Stop shooter motors.
   */
  private Command runHalt() {
    // Stream halt commands.
    return Commands.parallel(Arrays.stream(shooting)
      .map(motor -> motor.runPercent(0))
      .toArray(Command[]::new))
        .alongWith(manager.tag(State.STOPPED));
  }

  
  /**
   * Run intake rollers.
   */
  public Command run() {
    return runVelocity(Constants.Shooter.kSpeed);
  }

  /**
   * Stop intake rollers.
   */
  public Command stop() {
    return runHalt();
  }

  public static class ShootingVector {
    private final Pose3d position;
    private final Translation3d velocity;

    public ShootingVector(AngularVelocity velocity, Pose3d position) {
      this.position = position;
      final double vel = velocity.in(RotationsPerSecond) * Constants.Shooter.kWheelRadius.in(Meters);

      // Normalize to vector.
      this.velocity = new Translation3d(
        Meters.of(Math.cos(vel)), 
        Meters.of(Math.sin(vel)), 
        Meters.of(0));
    }

    /**
     * Get shooting position.
     * 
     * @return position of shooter.
     */
    public Pose3d getPosition() {
      return position;
    }

    /**
     * Get shooting velocity vector.
     * 
     * @return velocity vector of shooter.
     */
    public Translation3d getVelocity() {
      return velocity;
    }
  }
  /**
   * Get shooting position and vector.
   */
  public ShootingVector getShootingVector() {
    return new ShootingVector(primary.getVelocity(), new Pose3d(
      Meters.of(-0.183302), Meters.of(0.31), Inches.of(14.759196),
    Rotation3d.kZero));
  }
}
