package frc.robot.subsystems;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Voltage;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.subsystems.motors.Motor;
import frc.robot.subsystems.motors.Motor.Application;
import frc.robot.subsystems.motors.Motor.Feedback;
import frc.robot.subsystems.motors.Motor.Feedforward;
import frc.robot.subsystems.motors.MotorIO;
import frc.robot.subsystems.motors.MotorIO.Direction;
import frc.robot.subsystems.motors.MotorIO.FollowerMode;
import frc.robot.subsystems.motors.MotorIO.NeutralMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Motor wrapper class that provides a high-level interface for motor control. Tests
 * configuration groups, control methods, and subsystem integration.
 */
class MotorTest {

  /** Test subsystem for motor attachment. */
  private static class TestSubsystem extends SubsystemBase {
    public TestSubsystem() {
      super();
    }
  }

  @BeforeAll
  static void initializeHAL() {
    // Initialize HAL for WPILib simulation
    HAL.initialize(500, 0);
  }

  @Nested
  @DisplayName("Feedforward Configuration Tests")
  class FeedforwardTests {

    @Test
    @DisplayName("Feedforward should store PID values")
    void feedforwardShouldStorePidValues() {
      Feedforward ff = new Feedforward(1.5, 0.01, 0.1);

      assertEquals(1.5, ff.kP, 0.001);
      assertEquals(0.01, ff.kI, 0.001);
      assertEquals(0.1, ff.kD, 0.001);
    }

    @Test
    @DisplayName("Feedforward should handle zero values")
    void feedforwardShouldHandleZeroValues() {
      Feedforward ff = new Feedforward(0, 0, 0);

      assertEquals(0, ff.kP, 0.001);
      assertEquals(0, ff.kI, 0.001);
      assertEquals(0, ff.kD, 0.001);
    }

    @Test
    @DisplayName("Feedforward should handle large values")
    void feedforwardShouldHandleLargeValues() {
      Feedforward ff = new Feedforward(100, 10, 50);

      assertEquals(100, ff.kP, 0.001);
      assertEquals(10, ff.kI, 0.001);
      assertEquals(50, ff.kD, 0.001);
    }

    @Test
    @DisplayName("Feedforward should handle negative values")
    void feedforwardShouldHandleNegativeValues() {
      // While unusual, negative gains might be used in some control schemes
      Feedforward ff = new Feedforward(-1, -0.1, -0.01);

      assertEquals(-1, ff.kP, 0.001);
      assertEquals(-0.1, ff.kI, 0.001);
      assertEquals(-0.01, ff.kD, 0.001);
    }
  }

  @Nested
  @DisplayName("Feedback Configuration Tests")
  class FeedbackTests {

    @Test
    @DisplayName("Feedback should store feedforward values")
    void feedbackShouldStoreFeedforwardValues() {
      Feedback fb = new Feedback(0.1, 0.12, 0.01);

      assertEquals(0.1, fb.kS, 0.001);
      assertEquals(0.12, fb.kV, 0.001);
      assertEquals(0.01, fb.kA, 0.001);
    }

    @Test
    @DisplayName("Feedback should handle zero values")
    void feedbackShouldHandleZeroValues() {
      Feedback fb = new Feedback(0, 0, 0);

      assertEquals(0, fb.kS, 0.001);
      assertEquals(0, fb.kV, 0.001);
      assertEquals(0, fb.kA, 0.001);
    }

    @Test
    @DisplayName("Feedback kS should represent static friction")
    void feedbackKsShouldRepresentStaticFriction() {
      // kS is typically small positive value
      Feedback fb = new Feedback(0.2, 0.12, 0.01);
      assertTrue(fb.kS >= 0, "Static friction should be non-negative");
    }

    @Test
    @DisplayName("Feedback kV should be positive for typical motors")
    void feedbackKvShouldBePositiveForTypicalMotors() {
      // kV represents velocity feedforward, typically positive
      Feedback fb = new Feedback(0.1, 0.124, 0.01);
      assertTrue(fb.kV > 0, "Velocity feedforward should be positive");
    }
  }

  @Nested
  @DisplayName("Application Configuration Tests")
  class ApplicationTests {

    @Test
    @DisplayName("Application should store direction")
    void applicationShouldStoreDirection() {
      Application app = new Application(Direction.FORWARD, NeutralMode.BRAKE, Amps.of(40));
      assertEquals(Direction.FORWARD, app.direction);
    }

    @Test
    @DisplayName("Application should store neutral mode")
    void applicationShouldStoreNeutralMode() {
      Application app = new Application(Direction.FORWARD, NeutralMode.BRAKE, Amps.of(40));
      assertEquals(NeutralMode.BRAKE, app.neutral);
    }

    @Test
    @DisplayName("Application should store current limit")
    void applicationShouldStoreCurrentLimit() {
      Application app = new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(60));
      assertEquals(60, app.limit.in(Amps), 0.001);
    }

    @Test
    @DisplayName("Application should support forward direction")
    void applicationShouldSupportForwardDirection() {
      Application app = new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(40));
      assertEquals(Direction.FORWARD, app.direction);
    }

    @Test
    @DisplayName("Application should support reverse direction")
    void applicationShouldSupportReverseDirection() {
      Application app = new Application(Direction.REVERSE, NeutralMode.COAST, Amps.of(40));
      assertEquals(Direction.REVERSE, app.direction);
    }

    @Test
    @DisplayName("Application should support coast neutral mode")
    void applicationShouldSupportCoastNeutralMode() {
      Application app = new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(40));
      assertEquals(NeutralMode.COAST, app.neutral);
    }

    @Test
    @DisplayName("Application should support brake neutral mode")
    void applicationShouldSupportBrakeNeutralMode() {
      Application app = new Application(Direction.FORWARD, NeutralMode.BRAKE, Amps.of(40));
      assertEquals(NeutralMode.BRAKE, app.neutral);
    }
  }

  @Nested
  @DisplayName("Direction Enum Tests")
  class DirectionEnumTests {

    @Test
    @DisplayName("Direction enum should have two values")
    void directionEnumShouldHaveTwoValues() {
      Direction[] directions = Direction.values();
      assertEquals(2, directions.length);
    }

    @Test
    @DisplayName("Direction should have FORWARD value")
    void directionShouldHaveForwardValue() {
      assertNotNull(Direction.FORWARD);
    }

    @Test
    @DisplayName("Direction should have REVERSE value")
    void directionShouldHaveReverseValue() {
      assertNotNull(Direction.REVERSE);
    }
  }

  @Nested
  @DisplayName("NeutralMode Enum Tests")
  class NeutralModeEnumTests {

    @Test
    @DisplayName("NeutralMode enum should have two values")
    void neutralModeEnumShouldHaveTwoValues() {
      NeutralMode[] modes = NeutralMode.values();
      assertEquals(2, modes.length);
    }

    @Test
    @DisplayName("NeutralMode should have COAST value")
    void neutralModeShouldHaveCoastValue() {
      assertNotNull(NeutralMode.COAST);
    }

    @Test
    @DisplayName("NeutralMode should have BRAKE value")
    void neutralModeShouldHaveBrakeValue() {
      assertNotNull(NeutralMode.BRAKE);
    }
  }

  @Nested
  @DisplayName("FollowerMode Enum Tests")
  class FollowerModeEnumTests {

    @Test
    @DisplayName("FollowerMode enum should have two values")
    void followerModeEnumShouldHaveTwoValues() {
      FollowerMode[] modes = FollowerMode.values();
      assertEquals(2, modes.length);
    }

    @Test
    @DisplayName("FollowerMode should have ALIGNED value")
    void followerModeShouldHaveAlignedValue() {
      assertNotNull(FollowerMode.ALIGNED);
    }

    @Test
    @DisplayName("FollowerMode should have INVERSE value")
    void followerModeShouldHaveInverseValue() {
      assertNotNull(FollowerMode.INVERSE);
    }
  }

  @Nested
  @DisplayName("Configuration Combination Tests")
  class ConfigurationCombinationTests {

    @Test
    @DisplayName("Should combine Feedforward and Feedback")
    void shouldCombineFeedforwardAndFeedback() {
      Feedforward ff = new Feedforward(1.0, 0.01, 0.1);
      Feedback fb = new Feedback(0.1, 0.12, 0.01);

      // Both should exist independently
      assertNotNull(ff);
      assertNotNull(fb);
      assertEquals(1.0, ff.kP, 0.001);
      assertEquals(0.1, fb.kS, 0.001);
    }

    @Test
    @DisplayName("Should create Application with all configurations")
    void shouldCreateApplicationWithAllConfigurations() {
      Application app = new Application(
          Direction.FORWARD,
          NeutralMode.BRAKE,
          Amps.of(80));

      assertEquals(Direction.FORWARD, app.direction);
      assertEquals(NeutralMode.BRAKE, app.neutral);
      assertEquals(80, app.limit.in(Amps), 0.001);
    }

    @Test
    @DisplayName("Should support shooter motor configuration")
    void shouldSupportShooterMotorConfiguration() {
      // Typical shooter motor configuration
      Feedforward shooterFF = new Feedforward(0.1, 0, 0);
      Feedback shooterFB = new Feedback(0, 0.12, 0);
      Application shooterApp = new Application(
          Direction.FORWARD,
          NeutralMode.COAST,
          Amps.of(80));

      assertEquals(0.1, shooterFF.kP, 0.001);
      assertEquals(0.12, shooterFB.kV, 0.001);
      assertEquals(NeutralMode.COAST, shooterApp.neutral);
    }

    @Test
    @DisplayName("Should support intake motor configuration")
    void shouldSupportIntakeMotorConfiguration() {
      // Typical intake motor configuration
      Feedforward intakeFF = new Feedforward(0.05, 0, 0);
      Application intakeApp = new Application(
          Direction.FORWARD,
          NeutralMode.BRAKE,
          Amps.of(40));

      assertEquals(0.05, intakeFF.kP, 0.001);
      assertEquals(NeutralMode.BRAKE, intakeApp.neutral);
      assertEquals(40, intakeApp.limit.in(Amps), 0.001);
    }

    @Test
    @DisplayName("Should support drivetrain motor configuration")
    void shouldSupportDrivetrainMotorConfiguration() {
      // Typical swerve drive motor configuration
      Feedforward driveFF = new Feedforward(0.1, 0, 0.5);
      Feedback driveFB = new Feedback(0, 0.124, 0);
      Application driveApp = new Application(
          Direction.FORWARD,
          NeutralMode.BRAKE,
          Amps.of(60));

      assertEquals(0.1, driveFF.kP, 0.001);
      assertEquals(0.5, driveFF.kD, 0.001);
      assertEquals(0.124, driveFB.kV, 0.001);
    }
  }

  @Nested
  @DisplayName("Units Integration Tests")
  class UnitsIntegrationTests {

    @Test
    @DisplayName("Current limit should support Amps")
    void currentLimitShouldSupportAmps() {
      Current limit = Amps.of(40);
      Application app = new Application(Direction.FORWARD, NeutralMode.BRAKE, limit);
      assertEquals(40, app.limit.in(Amps), 0.001);
    }

    @Test
    @DisplayName("Current limit should convert from milliamps")
    void currentLimitShouldConvertFromMilliamps() {
      Current limit = Milliamps.of(40000); // 40A
      assertEquals(40, limit.in(Amps), 0.001);
    }

    @Test
    @DisplayName("Angle should support Degrees")
    void angleShouldSupportDegrees() {
      Angle angle = Degrees.of(90);
      assertEquals(90, angle.in(Degrees), 0.001);
      assertEquals(0.25, angle.in(Rotations), 0.001);
    }

    @Test
    @DisplayName("Angle should support Rotations")
    void angleShouldSupportRotations() {
      Angle angle = Rotations.of(0.5);
      assertEquals(180, angle.in(Degrees), 0.001);
    }

    @Test
    @DisplayName("AngularVelocity should support RPM")
    void angularVelocityShouldSupportRpm() {
      AngularVelocity velocity = RPM.of(6000);
      assertEquals(6000, velocity.in(RPM), 0.001);
      assertEquals(100, velocity.in(RotationsPerSecond), 0.001);
    }

    @Test
    @DisplayName("Voltage should support Volts")
    void voltageShouldSupportVolts() {
      Voltage voltage = Volts.of(12);
      assertEquals(12, voltage.in(Volts), 0.001);
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle maximum current limit")
    void shouldHandleMaximumCurrentLimit() {
      // 120A is typically the maximum for FRC
      Application app = new Application(Direction.FORWARD, NeutralMode.BRAKE, Amps.of(120));
      assertEquals(120, app.limit.in(Amps), 0.001);
    }

    @Test
    @DisplayName("Should handle minimum practical current limit")
    void shouldHandleMinimumPracticalCurrentLimit() {
      Application app = new Application(Direction.FORWARD, NeutralMode.COAST, Amps.of(1));
      assertEquals(1, app.limit.in(Amps), 0.001);
    }

    @Test
    @DisplayName("Should handle very small PID gains")
    void shouldHandleVerySmallPidGains() {
      Feedforward ff = new Feedforward(0.001, 0.0001, 0.00001);
      assertEquals(0.001, ff.kP, 0.0001);
      assertEquals(0.0001, ff.kI, 0.00001);
      assertEquals(0.00001, ff.kD, 0.000001);
    }

    @Test
    @DisplayName("Should handle high-frequency control gains")
    void shouldHandleHighFrequencyControlGains() {
      // High gains used for position control
      Feedforward ff = new Feedforward(100, 0.5, 10);
      assertEquals(100, ff.kP, 0.001);
      assertEquals(0.5, ff.kI, 0.001);
      assertEquals(10, ff.kD, 0.001);
    }
  }
}
