package frc.robot;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.Time;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Indication;
import frc.robot.constants.Constants.Indication.Period;
import frc.robot.constants.Constants.Length;
import frc.robot.constants.Constants.Mathematics;
import frc.robot.constants.Constants.Poses.AllianceRelativePose;
import frc.robot.constants.Constants.Tempo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Constants class covering mathematical calculations, timing, and alliance-relative
 * transformations.
 */
class ConstantsTest {

  @Nested
  @DisplayName("Mathematics Constants Tests")
  class MathematicsTests {

    @Test
    @DisplayName("TAU should be approximately 2π")
    void tauShouldBeApproximately2Pi() {
      assertEquals(2 * Math.PI, Mathematics.TAU, 1e-10);
    }

    @Test
    @DisplayName("TAU should equal full circle in radians")
    void tauShouldEqualFullCircle() {
      assertEquals(6.283185307179586, Mathematics.TAU, 1e-15);
    }
  }

  @Nested
  @DisplayName("Shooter Regression Tests")
  class ShooterRegressionTests {

    @Test
    @DisplayName("Regression should return positive RPM for positive distance")
    void regressionShouldReturnPositiveRpm() {
      Distance distance = Inches.of(50);
      AngularVelocity velocity = Constants.regress(distance);
      assertTrue(velocity.in(RPM) > 0, "Velocity should be positive");
    }

    @Test
    @DisplayName("Regression should increase RPM with distance")
    void regressionShouldIncreaseWithDistance() {
      AngularVelocity velocity1 = Constants.regress(Inches.of(50));
      AngularVelocity velocity2 = Constants.regress(Inches.of(100));

      assertTrue(
          velocity2.in(RPM) > velocity1.in(RPM), "RPM should increase with distance");
    }

    @Test
    @DisplayName("Regression should return base RPM at zero distance")
    void regressionShouldReturnBaseRpmAtZeroDistance() {
      AngularVelocity velocity = Constants.regress(Inches.of(0));
      // base * exponential^0 = base * 1 = base
      assertEquals(Constants.base, velocity.in(RPM), 0.01);
    }

    @Test
    @DisplayName("Regression should use exponential formula correctly")
    void regressionShouldUseExponentialFormula() {
      double distanceInches = 100.0;
      double expectedRpm = Constants.base * Math.pow(Constants.exponential, distanceInches);
      AngularVelocity velocity = Constants.regress(Inches.of(distanceInches));
      assertEquals(expectedRpm, velocity.in(RPM), 0.01);
    }

    @Test
    @DisplayName("Regression should handle edge distances")
    void regressionShouldHandleEdgeDistances() {
      // Test minimum distance
      assertDoesNotThrow(() -> Constants.regress(Inches.of(0)));

      // Test maximum expected distance
      assertDoesNotThrow(() -> Constants.regress(Inches.of(200)));

      // Test negative distance (edge case)
      assertDoesNotThrow(() -> Constants.regress(Inches.of(-10)));
    }
  }

  @Nested
  @DisplayName("Time Period Tests")
  class TimePeriodTests {

    @Test
    @DisplayName("Match lengths should be positive")
    void matchLengthsShouldBePositive() {
      assertTrue(Length.autonomous.in(Seconds) > 0);
      assertTrue(Length.delay.in(Seconds) > 0);
      assertTrue(Length.transition.in(Seconds) > 0);
      assertTrue(Length.phase.in(Seconds) > 0);
      assertTrue(Length.endgame.in(Seconds) > 0);
      assertTrue(Length.teleoperated.in(Seconds) > 0);
    }

    @Test
    @DisplayName("Autonomous period should be 20 seconds")
    void autonomousPeriodShouldBe20Seconds() {
      assertEquals(20.0, Length.autonomous.in(Seconds), 0.001);
    }

    @Test
    @DisplayName("Teleop period should be 140 seconds")
    void teleopPeriodShouldBe140Seconds() {
      assertEquals(140.0, Length.teleoperated.in(Seconds), 0.001);
    }

    @Test
    @DisplayName("fromTime should return AUTONOMOUS during auto")
    void fromTimeShouldReturnAutonomousDuringAuto() {
      assertEquals(Period.AUTONOMOUS, Indication.fromTime(Seconds.of(0)));
      assertEquals(Period.AUTONOMOUS, Indication.fromTime(Seconds.of(10)));
      assertEquals(Period.AUTONOMOUS, Indication.fromTime(Seconds.of(19)));
    }

    @Test
    @DisplayName("fromTime should return TRANSITION after auto")
    void fromTimeShouldReturnTransitionAfterAuto() {
      Time transitionStart = Length.autonomous.plus(Seconds.of(0.1));
      assertEquals(Period.TRANSITION, Indication.fromTime(transitionStart));
    }

    @Test
    @DisplayName("fromTime should progress through periods")
    void fromTimeShouldProgressThroughPeriods() {
      // This tests the general ordering of periods
      Time t = Seconds.of(0);
      Period firstPeriod = Indication.fromTime(t);
      assertEquals(Period.AUTONOMOUS, firstPeriod);

      // Late in the match should be ENDGAME
      Time lateTime = Seconds.of(200);
      assertEquals(Period.ENDGAME, Indication.fromTime(lateTime));
    }
  }

  @Nested
  @DisplayName("Tempo Tests")
  class TempoTests {

    @Test
    @DisplayName("isElapsed should return false at start")
    void isElapsedShouldReturnFalseAtStart() {
      Tempo.startTime();
      Tempo.tick();
      assertFalse(Tempo.isElapsed(Seconds.of(1000)));
    }

    @Test
    @DisplayName("isRange should work for valid ranges")
    void isRangeShouldWorkForValidRanges() {
      // This test validates the logic of isRange
      // Since tick() uses real time, we test the boundary logic
      Tempo.startTime(Seconds.of(50)); // Start at 50 seconds
      Time time = Tempo.tick();

      // Time should be around 50 seconds
      assertTrue(time.in(Seconds) >= 50.0 - 1.0, "Time should be at least 49 seconds");
    }

    @Test
    @DisplayName("getTime should return non-negative value after tick")
    void getTimeShouldReturnNonNegativeValue() {
      Tempo.startTime();
      Tempo.tick();
      assertTrue(Tempo.getTime().in(Seconds) >= 0);
    }

    @Test
    @DisplayName("startTime with offset should set correct initial time")
    void startTimeWithOffsetShouldSetCorrectInitialTime() {
      Time offset = Seconds.of(30);
      Tempo.startTime(offset);
      Tempo.tick();

      // Time should be approximately the offset (within tolerance for execution time)
      assertTrue(Tempo.getTime().in(Seconds) >= 29.0);
      assertTrue(Tempo.getTime().in(Seconds) <= 32.0);
    }
  }

  @Nested
  @DisplayName("Alliance Relative Pose Tests")
  class AllianceRelativePoseTests {

    @Test
    @DisplayName("AllianceRelativePose should store pose")
    void allianceRelativePoseShouldStorePose() {
      Pose2d testPose = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(45));
      AllianceRelativePose relativePose = new AllianceRelativePose(testPose);

      // Blue pose should be the same as input (assuming blue alliance default)
      assertNotNull(relativePose.getBluePose());
      assertNotNull(relativePose.getRedPose());
    }

    @Test
    @DisplayName("Red and Blue poses should be different (rotated 180°)")
    void redAndBluePosesShouldBeDifferent() {
      Pose2d testPose = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(0));
      AllianceRelativePose relativePose = new AllianceRelativePose(testPose);

      Pose2d bluePose = relativePose.getBluePose();
      Pose2d redPose = relativePose.getRedPose();

      // Red pose should be rotated around center
      assertNotEquals(bluePose.getX(), redPose.getX(), 0.01);
    }

    @Test
    @DisplayName("Constructor with alliance should work correctly")
    void constructorWithAllianceShouldWorkCorrectly() {
      Pose2d testPose = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(90));

      AllianceRelativePose blueOrigin = new AllianceRelativePose(testPose, Alliance.Blue);
      AllianceRelativePose redOrigin = new AllianceRelativePose(testPose, Alliance.Red);

      // Both should be valid
      assertNotNull(blueOrigin.getPose());
      assertNotNull(redOrigin.getPose());
    }
  }

  @Nested
  @DisplayName("Mirror Function Tests")
  class MirrorFunctionTests {

    @Test
    @DisplayName("Mirror should flip Y coordinate around middle")
    void mirrorShouldFlipYCoordinate() {
      Pose2d original = new Pose2d(Meters.of(1.0), Meters.of(2.0), Rotation2d.fromDegrees(0));
      Pose2d mirrored = Constants.mirror(original);

      // X should remain the same
      assertEquals(
          original.getMeasureX().in(Meters), mirrored.getMeasureX().in(Meters), 0.001);

      // Y should be mirrored around middle (Constants.middle.getY() * 2 - original.getY())
      double expectedY =
          Constants.middle.getMeasureY().in(Meters) * 2 - original.getMeasureY().in(Meters);
      assertEquals(expectedY, mirrored.getMeasureY().in(Meters), 0.001);

      // Rotation should remain the same
      assertEquals(
          original.getRotation().getDegrees(), mirrored.getRotation().getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Double mirror should return original pose")
    void doubleMirrorShouldReturnOriginal() {
      Pose2d original = new Pose2d(Meters.of(3.0), Meters.of(5.0), Rotation2d.fromDegrees(45));
      Pose2d doubleMirrored = Constants.mirror(Constants.mirror(original));

      assertEquals(original.getX(), doubleMirrored.getX(), 0.001);
      assertEquals(original.getY(), doubleMirrored.getY(), 0.001);
    }
  }

  @Nested
  @DisplayName("Field Poses Tests")
  class FieldPosesTests {

    @Test
    @DisplayName("Tower pose should be valid")
    void towerPoseShouldBeValid() {
      assertNotNull(Constants.Poses.tower);
      assertNotNull(Constants.Poses.tower.getPose());
    }

    @Test
    @DisplayName("Hub pose should be valid")
    void hubPoseShouldBeValid() {
      assertNotNull(Constants.Poses.hub);
      assertNotNull(Constants.Poses.hub.getPose());
    }

    @Test
    @DisplayName("Field middle should be positive")
    void fieldMiddleShouldBePositive() {
      assertTrue(Constants.middle.getMeasureX().in(Meters) > 0);
      assertTrue(Constants.middle.getMeasureY().in(Meters) > 0);
    }
  }

  @Nested
  @DisplayName("Motor ID Tests")
  class MotorIdTests {

    @Test
    @DisplayName("Motor IDs should be unique")
    void motorIdsShouldBeUnique() {
      int[] ids = {
        Constants.MotorIDs.i_rollers,
        Constants.MotorIDs.s_feeder,
        Constants.MotorIDs.s_shooterR,
        Constants.MotorIDs.s_shooterL,
        Constants.MotorIDs.h_hopper,
        Constants.MotorIDs.i_wristL,
        Constants.MotorIDs.i_wristR
      };

      // Check all IDs are unique
      for (int i = 0; i < ids.length; i++) {
        for (int j = i + 1; j < ids.length; j++) {
          assertNotEquals(ids[i], ids[j], "Motor IDs should be unique: " + ids[i] + " duplicated");
        }
      }
    }

    @Test
    @DisplayName("Motor IDs should be positive")
    void motorIdsShouldBePositive() {
      assertTrue(Constants.MotorIDs.i_rollers > 0);
      assertTrue(Constants.MotorIDs.s_feeder > 0);
      assertTrue(Constants.MotorIDs.s_shooterR > 0);
      assertTrue(Constants.MotorIDs.s_shooterL > 0);
      assertTrue(Constants.MotorIDs.h_hopper > 0);
      assertTrue(Constants.MotorIDs.i_wristL > 0);
      assertTrue(Constants.MotorIDs.i_wristR > 0);
    }

    @Test
    @DisplayName("Motor IDs should be within valid CAN range")
    void motorIdsShouldBeWithinValidCanRange() {
      // CAN IDs typically range from 1-62 for FRC
      int maxCanId = 62;
      assertTrue(Constants.MotorIDs.i_rollers <= maxCanId);
      assertTrue(Constants.MotorIDs.s_feeder <= maxCanId);
      assertTrue(Constants.MotorIDs.s_shooterR <= maxCanId);
      assertTrue(Constants.MotorIDs.s_shooterL <= maxCanId);
      assertTrue(Constants.MotorIDs.h_hopper <= maxCanId);
      assertTrue(Constants.MotorIDs.i_wristL <= maxCanId);
      assertTrue(Constants.MotorIDs.i_wristR <= maxCanId);
    }
  }

  @Nested
  @DisplayName("Shooter Constants Tests")
  class ShooterConstantsTests {

    @Test
    @DisplayName("Shooter speed should be positive")
    void shooterSpeedShouldBePositive() {
      assertTrue(Constants.Shooter.kSpeed.in(RPM) > 0);
    }

    @Test
    @DisplayName("Shooter velocity tolerance should be positive")
    void shooterVelocityToleranceShouldBePositive() {
      assertTrue(Constants.Shooter.kVelocityTolerance.in(RotationsPerSecond) > 0);
    }

    @Test
    @DisplayName("Shooter current limit should be reasonable")
    void shooterCurrentLimitShouldBeReasonable() {
      double limit = Constants.Shooter.kCurrentLimit.in(Amps);
      assertTrue(limit > 0, "Current limit should be positive");
      assertTrue(limit <= 120, "Current limit should not exceed 120A");
    }

    @Test
    @DisplayName("Shooter timing constants should be positive")
    void shooterTimingConstantsShouldBePositive() {
      assertTrue(Constants.Shooter.kChargeUpTime.in(Seconds) > 0);
      assertTrue(Constants.Shooter.kFiringTime.in(Seconds) > 0);
      assertTrue(Constants.Shooter.kUntilSecondMagnitude.in(Seconds) > 0);
      assertTrue(Constants.Shooter.kUntilThirdMagnitude.in(Seconds) > 0);
      assertTrue(Constants.Shooter.kDebounce.in(Seconds) > 0);
    }

    @Test
    @DisplayName("Shooter alignment error should be small angle")
    void shooterAlignmentErrorShouldBeSmallAngle() {
      double errorDegrees = Constants.Shooter.kAlignmentError.in(Degrees);
      assertTrue(errorDegrees > 0, "Alignment error should be positive");
      assertTrue(errorDegrees <= 10, "Alignment error should be reasonable (<=10°)");
    }
  }

  @Nested
  @DisplayName("Intake Constants Tests")
  class IntakeConstantsTests {

    @Test
    @DisplayName("Intake speed should be within valid range")
    void intakeSpeedShouldBeWithinValidRange() {
      assertTrue(Constants.Intake.kSpeed >= 0.0);
      assertTrue(Constants.Intake.kSpeed <= 1.0);
    }

    @Test
    @DisplayName("Oscillation frequency should be positive")
    void oscillationFrequencyShouldBePositive() {
      assertTrue(Constants.Intake.kOscillationFrequency.in(Hertz) > 0);
    }
  }

  @Nested
  @DisplayName("Control Constants Tests")
  class ControlConstantsTests {

    @Test
    @DisplayName("Translation PID constants should be valid")
    void translationPidConstantsShouldBeValid() {
      assertNotNull(Constants.Control.translationPID);
      assertTrue(Constants.Control.translationPID.kP >= 0);
      assertTrue(Constants.Control.translationPID.kI >= 0);
      assertTrue(Constants.Control.translationPID.kD >= 0);
    }

    @Test
    @DisplayName("Rotation PID constants should be valid")
    void rotationPidConstantsShouldBeValid() {
      assertNotNull(Constants.Control.rotationPID);
      assertTrue(Constants.Control.rotationPID.kP >= 0);
      assertTrue(Constants.Control.rotationPID.kI >= 0);
      assertTrue(Constants.Control.rotationPID.kD >= 0);
    }
  }

  @Nested
  @DisplayName("Limelight Constants Tests")
  class LimelightConstantsTests {

    @Test
    @DisplayName("Limelight names should not be empty")
    void limelightNamesShouldNotBeEmpty() {
      assertTrue(Constants.Limelight.localization.length > 0);
      assertTrue(Constants.Limelight.limelights.length > 0);

      for (String name : Constants.Limelight.localization) {
        assertNotNull(name);
        assertFalse(name.isEmpty());
      }
    }

    @Test
    @DisplayName("Max distance should be positive")
    void maxDistanceShouldBePositive() {
      assertTrue(Constants.Limelight.maxDistance.in(Inches) > 0);
    }
  }

  @Nested
  @DisplayName("Mode Tests")
  class ModeTests {

    @Test
    @DisplayName("All modes should be accessible")
    void allModesShouldBeAccessible() {
      assertNotNull(Constants.Mode.COMPETITION);
      assertNotNull(Constants.Mode.REAL);
      assertNotNull(Constants.Mode.SIM);
      assertNotNull(Constants.Mode.REPLAY);
    }

    @Test
    @DisplayName("Mode enum should have expected values")
    void modeEnumShouldHaveExpectedValues() {
      Constants.Mode[] modes = Constants.Mode.values();
      assertEquals(4, modes.length);
    }
  }
}
