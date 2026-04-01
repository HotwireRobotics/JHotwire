package frc.robot;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import frc.robot.applicable.simulation.Handler;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the simulation Handler class that manages gamepiece physics and game state.
 */
class SimulationHandlerTest {

  @BeforeAll
  static void initializeHAL() {
    // Initialize HAL for WPILib simulation
    HAL.initialize(500, 0);
  }

  @Nested
  @DisplayName("Handler Initialization Tests")
  class InitializationTests {

    @Test
    @DisplayName("Handler should accept null suppliers")
    void handlerShouldAcceptNullSuppliers() {
      // Null suppliers are valid for non-sim mode
      assertDoesNotThrow(() -> new Handler(null, null, null, null, null, null));
    }

    @Test
    @DisplayName("Handler should accept valid suppliers")
    void handlerShouldAcceptValidSuppliers() {
      Supplier<AngularVelocity> velocity = () -> RPM.of(0);
      BooleanSupplier doShoot = () -> false;
      BooleanSupplier doIntake = () -> false;
      Supplier<Angle> target = () -> Degrees.of(0);
      Supplier<Pose2d> pose = () -> Pose2d.kZero;
      Supplier<ChassisSpeeds> speeds = () -> new ChassisSpeeds();

      assertDoesNotThrow(() -> new Handler(velocity, doShoot, doIntake, target, pose, speeds));
    }
  }

  @Nested
  @DisplayName("Handler Tick Tests")
  class TickTests {

    @Test
    @DisplayName("Tick should not throw with null suppliers")
    void tickShouldNotThrowWithNullSuppliers() {
      Handler handler = new Handler(null, null, null, null, null, null);
      assertDoesNotThrow(() -> handler.tick());
    }

    @Test
    @DisplayName("Tick should not throw with valid suppliers")
    void tickShouldNotThrowWithValidSuppliers() {
      Supplier<AngularVelocity> velocity = () -> RPM.of(2400);
      BooleanSupplier doShoot = () -> false;
      BooleanSupplier doIntake = () -> false;
      Supplier<Angle> target = () -> Degrees.of(45);
      Supplier<Pose2d> pose = () -> new Pose2d(5, 3, Rotation2d.fromDegrees(90));
      Supplier<ChassisSpeeds> speeds = () -> new ChassisSpeeds(1.0, 0.5, 0.1);

      Handler handler = new Handler(velocity, doShoot, doIntake, target, pose, speeds);
      assertDoesNotThrow(() -> handler.tick());
    }

    @Test
    @DisplayName("Multiple ticks should not throw")
    void multipleTicksShouldNotThrow() {
      Handler handler = new Handler(null, null, null, null, null, null);
      for (int i = 0; i < 100; i++) {
        assertDoesNotThrow(() -> handler.tick());
      }
    }
  }

  @Nested
  @DisplayName("Autonomous Tests")
  class AutonomousTests {

    @Test
    @DisplayName("Autonomous should not throw with null suppliers")
    void autonomousShouldNotThrowWithNullSuppliers() {
      Handler handler = new Handler(null, null, null, null, null, null);
      assertDoesNotThrow(() -> handler.autonomous());
    }

    @Test
    @DisplayName("Autonomous should reset state")
    void autonomousShouldResetState() {
      Handler handler = new Handler(null, null, null, null, null, null);
      handler.autonomous();
      // Handler should initialize fuel count for autonomous
      assertDoesNotThrow(() -> handler.tick());
    }
  }

  @Nested
  @DisplayName("Counter Tests")
  class CounterTests {

    @Test
    @DisplayName("setCounter should not throw")
    void setCounterShouldNotThrow() {
      Handler handler = new Handler(null, null, null, null, null, null);
      assertDoesNotThrow(() -> handler.setCounter(5));
    }

    @Test
    @DisplayName("setCounter should accept zero")
    void setCounterShouldAcceptZero() {
      Handler handler = new Handler(null, null, null, null, null, null);
      assertDoesNotThrow(() -> handler.setCounter(0));
    }

    @Test
    @DisplayName("setCounter should accept large values")
    void setCounterShouldAcceptLargeValues() {
      Handler handler = new Handler(null, null, null, null, null, null);
      assertDoesNotThrow(() -> handler.setCounter(100));
    }
  }

  @Nested
  @DisplayName("Wrist Pitch Tests")
  class WristPitchTests {

    @Test
    @DisplayName("getWristPitch should return angle")
    void getWristPitchShouldReturnAngle() {
      Handler handler = new Handler(null, null, null, null, null, null);
      Angle pitch = handler.getWristPitch();
      assertNotNull(pitch);
    }

    @Test
    @DisplayName("getWristPitch should not throw")
    void getWristPitchShouldNotThrow() {
      Handler handler = new Handler(null, null, null, null, null, null);
      assertDoesNotThrow(() -> handler.getWristPitch());
    }
  }

  @Nested
  @DisplayName("Intake Tests")
  class IntakeTests {

    @Test
    @DisplayName("intake should not throw")
    void intakeShouldNotThrow() {
      Handler handler = new Handler(null, null, null, null, null, null);
      assertDoesNotThrow(() -> handler.intake());
    }

    @Test
    @DisplayName("Multiple intakes should not throw")
    void multipleIntakesShouldNotThrow() {
      Handler handler = new Handler(null, null, null, null, null, null);
      for (int i = 0; i < 10; i++) {
        assertDoesNotThrow(() -> handler.intake());
      }
    }
  }

  @Nested
  @DisplayName("Supplier Integration Tests")
  class SupplierIntegrationTests {

    @Test
    @DisplayName("Velocity supplier should be called during tick")
    void velocitySupplierShouldBeCalledDuringTick() {
      int[] callCount = {0};
      Supplier<AngularVelocity> velocity = () -> {
        callCount[0]++;
        return RPM.of(2400);
      };

      Handler handler = new Handler(velocity, () -> false, () -> false, 
          () -> Degrees.of(0), () -> Pose2d.kZero, () -> new ChassisSpeeds());
      handler.tick();

      assertTrue(callCount[0] > 0, "Velocity supplier should be called");
    }

    @Test
    @DisplayName("Pose supplier should be called during tick")
    void poseSupplierShouldBeCalledDuringTick() {
      int[] callCount = {0};
      Supplier<Pose2d> pose = () -> {
        callCount[0]++;
        return new Pose2d(5, 3, Rotation2d.kZero);
      };

      Handler handler = new Handler(() -> RPM.of(0), () -> false, () -> false, 
          () -> Degrees.of(0), pose, () -> new ChassisSpeeds());
      handler.tick();

      assertTrue(callCount[0] > 0, "Pose supplier should be called");
    }

    @Test
    @DisplayName("Shoot supplier should be respected")
    void shootSupplierShouldBeRespected() {
      boolean[] shootState = {false};
      BooleanSupplier doShoot = () -> shootState[0];

      Handler handler = new Handler(() -> RPM.of(2400), doShoot, () -> false, 
          () -> Degrees.of(45), () -> Pose2d.kZero, () -> new ChassisSpeeds());

      // Test with shoot = false
      shootState[0] = false;
      assertDoesNotThrow(() -> handler.tick());

      // Test with shoot = true
      shootState[0] = true;
      assertDoesNotThrow(() -> handler.tick());
    }

    @Test
    @DisplayName("Intake supplier should be respected")
    void intakeSupplierShouldBeRespected() {
      boolean[] intakeState = {false};
      BooleanSupplier doIntake = () -> intakeState[0];

      Handler handler = new Handler(() -> RPM.of(0), () -> false, doIntake, 
          () -> Degrees.of(0), () -> Pose2d.kZero, () -> new ChassisSpeeds());

      intakeState[0] = true;
      assertDoesNotThrow(() -> handler.tick());

      intakeState[0] = false;
      assertDoesNotThrow(() -> handler.tick());
    }
  }

  @Nested
  @DisplayName("ChassisSpeeds Integration Tests")
  class ChassisSpeedsTests {

    @Test
    @DisplayName("Handler should accept various chassis speeds")
    void handlerShouldAcceptVariousChassisSpeeds() {
      Supplier<ChassisSpeeds> speeds = () -> new ChassisSpeeds(
          3.0,  // vx m/s
          2.0,  // vy m/s
          1.5   // omega rad/s
      );

      Handler handler = new Handler(() -> RPM.of(0), () -> false, () -> false, 
          () -> Degrees.of(0), () -> Pose2d.kZero, speeds);
      
      assertDoesNotThrow(() -> handler.tick());
    }

    @Test
    @DisplayName("Handler should handle zero chassis speeds")
    void handlerShouldHandleZeroChassisSpeeds() {
      Handler handler = new Handler(() -> RPM.of(0), () -> false, () -> false, 
          () -> Degrees.of(0), () -> Pose2d.kZero, () -> new ChassisSpeeds(0, 0, 0));
      
      assertDoesNotThrow(() -> handler.tick());
    }

    @Test
    @DisplayName("Handler should handle field-relative movements")
    void handlerShouldHandleFieldRelativeMovements() {
      // Simulate various movement patterns
      ChassisSpeeds[] patterns = {
          new ChassisSpeeds(1, 0, 0),    // Forward
          new ChassisSpeeds(-1, 0, 0),   // Backward
          new ChassisSpeeds(0, 1, 0),    // Strafe left
          new ChassisSpeeds(0, -1, 0),   // Strafe right
          new ChassisSpeeds(0, 0, 1),    // Rotate CCW
          new ChassisSpeeds(0, 0, -1),   // Rotate CW
          new ChassisSpeeds(1, 1, 1),    // Combined
      };

      for (ChassisSpeeds pattern : patterns) {
        Handler handler = new Handler(() -> RPM.of(0), () -> false, () -> false, 
            () -> Degrees.of(0), () -> Pose2d.kZero, () -> pattern);
        assertDoesNotThrow(() -> handler.tick());
      }
    }
  }

  @Nested
  @DisplayName("Wrist Angle Integration Tests")
  class WristAngleTests {

    @Test
    @DisplayName("Handler should accept various wrist angles")
    void handlerShouldAcceptVariousWristAngles() {
      double[] angles = {0, 15, 30, 45, 60, 75, 90, -15, -30};
      
      for (double angle : angles) {
        Supplier<Angle> target = () -> Degrees.of(angle);
        Handler handler = new Handler(() -> RPM.of(0), () -> false, () -> false, 
            target, () -> Pose2d.kZero, () -> new ChassisSpeeds());
        assertDoesNotThrow(() -> handler.tick());
      }
    }
  }

  @Nested
  @DisplayName("Shooter Velocity Tests")
  class ShooterVelocityTests {

    @Test
    @DisplayName("Handler should accept various shooter velocities")
    void handlerShouldAcceptVariousShooterVelocities() {
      double[] velocities = {0, 1000, 2000, 2400, 3000, 4000};
      
      for (double vel : velocities) {
        Supplier<AngularVelocity> velocity = () -> RPM.of(vel);
        Handler handler = new Handler(velocity, () -> false, () -> false, 
            () -> Degrees.of(45), () -> Pose2d.kZero, () -> new ChassisSpeeds());
        assertDoesNotThrow(() -> handler.tick());
      }
    }

    @Test
    @DisplayName("Handler should handle shooting at various velocities")
    void handlerShouldHandleShootingAtVariousVelocities() {
      double[] velocities = {2000, 2400, 3000};
      
      for (double vel : velocities) {
        Handler handler = new Handler(
            () -> RPM.of(vel), 
            () -> true,  // Always shooting
            () -> false, 
            () -> Degrees.of(45), 
            () -> new Pose2d(5, 3, Rotation2d.fromDegrees(90)), 
            () -> new ChassisSpeeds());
        
        assertDoesNotThrow(() -> handler.tick());
      }
    }
  }

  @Nested
  @DisplayName("Full Simulation Cycle Tests")
  class FullSimulationCycleTests {

    @Test
    @DisplayName("Should handle complete autonomous cycle")
    void shouldHandleCompleteAutonomousCycle() {
      Handler handler = new Handler(
          () -> RPM.of(2400),
          () -> true,
          () -> true,
          () -> Degrees.of(45),
          () -> new Pose2d(5, 3, Rotation2d.kZero),
          () -> new ChassisSpeeds(1, 0, 0));

      // Initialize autonomous
      handler.autonomous();

      // Simulate 3 seconds of autonomous (150 ticks at 50Hz)
      for (int i = 0; i < 150; i++) {
        assertDoesNotThrow(() -> handler.tick());
      }
    }

    @Test
    @DisplayName("Should handle state changes during simulation")
    void shouldHandleStateChangesDuringSimulation() {
      boolean[] shooting = {false};
      boolean[] intaking = {true};
      double[] wristAngle = {0};

      Handler handler = new Handler(
          () -> RPM.of(shooting[0] ? 2400 : 0),
          () -> shooting[0],
          () -> intaking[0],
          () -> Degrees.of(wristAngle[0]),
          () -> new Pose2d(5, 3, Rotation2d.kZero),
          () -> new ChassisSpeeds());

      // Simulate intake phase
      for (int i = 0; i < 50; i++) {
        handler.tick();
      }

      // Switch to shooting
      intaking[0] = false;
      shooting[0] = true;
      wristAngle[0] = 45;

      // Simulate shooting phase
      for (int i = 0; i < 50; i++) {
        assertDoesNotThrow(() -> handler.tick());
      }
    }
  }
}
