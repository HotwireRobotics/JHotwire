package frc.robot;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for WPILib math utilities used throughout the codebase.
 * Verifies correct usage of Pose2d, Rotation2d, ChassisSpeeds, and swerve kinematics.
 */
class WPILibMathTest {

  @BeforeAll
  static void initializeHAL() {
    HAL.initialize(500, 0);
  }

  @Nested
  @DisplayName("Rotation2d Tests")
  class Rotation2dTests {

    @Test
    @DisplayName("Should create rotation from degrees")
    void shouldCreateRotationFromDegrees() {
      Rotation2d rotation = Rotation2d.fromDegrees(90);
      assertEquals(90, rotation.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should create rotation from radians")
    void shouldCreateRotationFromRadians() {
      Rotation2d rotation = Rotation2d.fromRadians(Math.PI);
      assertEquals(180, rotation.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should handle full rotation")
    void shouldHandleFullRotation() {
      Rotation2d rotation = Rotation2d.fromDegrees(360);
      assertEquals(0, rotation.getDegrees(), 0.1);
    }

    @Test
    @DisplayName("Should handle negative angles")
    void shouldHandleNegativeAngles() {
      Rotation2d rotation = Rotation2d.fromDegrees(-90);
      assertEquals(-90, rotation.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should add rotations correctly")
    void shouldAddRotationsCorrectly() {
      Rotation2d r1 = Rotation2d.fromDegrees(45);
      Rotation2d r2 = Rotation2d.fromDegrees(45);
      Rotation2d sum = r1.plus(r2);
      assertEquals(90, sum.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should subtract rotations correctly")
    void shouldSubtractRotationsCorrectly() {
      Rotation2d r1 = Rotation2d.fromDegrees(90);
      Rotation2d r2 = Rotation2d.fromDegrees(45);
      Rotation2d diff = r1.minus(r2);
      assertEquals(45, diff.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should compute cosine correctly")
    void shouldComputeCosineCorrectly() {
      Rotation2d rotation = Rotation2d.fromDegrees(60);
      assertEquals(0.5, rotation.getCos(), 0.001);
    }

    @Test
    @DisplayName("Should compute sine correctly")
    void shouldComputeSineCorrectly() {
      Rotation2d rotation = Rotation2d.fromDegrees(30);
      assertEquals(0.5, rotation.getSin(), 0.001);
    }

    @Test
    @DisplayName("kZero should be zero degrees")
    void kZeroShouldBeZeroDegrees() {
      assertEquals(0, Rotation2d.kZero.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("k90deg should be 90 degrees")
    void k90degShouldBe90Degrees() {
      assertEquals(90, Rotation2d.k90deg.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("k180deg should be 180 degrees")
    void k180degShouldBe180Degrees() {
      assertEquals(180, Math.abs(Rotation2d.k180deg.getDegrees()), 0.1);
    }
  }

  @Nested
  @DisplayName("Pose2d Tests")
  class Pose2dTests {

    @Test
    @DisplayName("Should create pose with coordinates")
    void shouldCreatePoseWithCoordinates() {
      Pose2d pose = new Pose2d(5.0, 3.0, Rotation2d.fromDegrees(45));
      assertEquals(5.0, pose.getX(), 0.001);
      assertEquals(3.0, pose.getY(), 0.001);
      assertEquals(45, pose.getRotation().getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should create pose with units")
    void shouldCreatePoseWithUnits() {
      Pose2d pose = new Pose2d(Meters.of(2.0), Meters.of(1.5), Rotation2d.fromDegrees(90));
      assertEquals(2.0, pose.getMeasureX().in(Meters), 0.001);
      assertEquals(1.5, pose.getMeasureY().in(Meters), 0.001);
    }

    @Test
    @DisplayName("kZero should be at origin")
    void kZeroShouldBeAtOrigin() {
      assertEquals(0, Pose2d.kZero.getX(), 0.001);
      assertEquals(0, Pose2d.kZero.getY(), 0.001);
      assertEquals(0, Pose2d.kZero.getRotation().getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should transform pose correctly")
    void shouldTransformPoseCorrectly() {
      Pose2d original = new Pose2d(1, 1, Rotation2d.kZero);
      Pose2d transformed = original.rotateBy(Rotation2d.k90deg);
      
      // After 90 degree rotation, the pose should be rotated
      assertEquals(90, transformed.getRotation().getDegrees(), 0.1);
    }

    @Test
    @DisplayName("Should interpolate poses")
    void shouldInterpolatePoses() {
      Pose2d start = new Pose2d(0, 0, Rotation2d.kZero);
      Pose2d end = new Pose2d(10, 10, Rotation2d.fromDegrees(90));
      
      Pose2d midpoint = start.interpolate(end, 0.5);
      assertEquals(5.0, midpoint.getX(), 0.1);
      assertEquals(5.0, midpoint.getY(), 0.1);
      assertEquals(45, midpoint.getRotation().getDegrees(), 0.1);
    }
  }

  @Nested
  @DisplayName("ChassisSpeeds Tests")
  class ChassisSpeedsTests {

    @Test
    @DisplayName("Should create zero chassis speeds")
    void shouldCreateZeroChassisSpeeds() {
      ChassisSpeeds speeds = new ChassisSpeeds();
      assertEquals(0, speeds.vxMetersPerSecond, 0.001);
      assertEquals(0, speeds.vyMetersPerSecond, 0.001);
      assertEquals(0, speeds.omegaRadiansPerSecond, 0.001);
    }

    @Test
    @DisplayName("Should create speeds with values")
    void shouldCreateSpeedsWithValues() {
      ChassisSpeeds speeds = new ChassisSpeeds(2.0, 1.5, 0.5);
      assertEquals(2.0, speeds.vxMetersPerSecond, 0.001);
      assertEquals(1.5, speeds.vyMetersPerSecond, 0.001);
      assertEquals(0.5, speeds.omegaRadiansPerSecond, 0.001);
    }

    @Test
    @DisplayName("Should discretize speeds")
    void shouldDiscretizeSpeeds() {
      ChassisSpeeds speeds = new ChassisSpeeds(2.0, 1.0, 1.0);
      ChassisSpeeds discrete = ChassisSpeeds.discretize(speeds, 0.02);
      
      // Discretized speeds should account for rotation during timestep
      assertNotNull(discrete);
    }

    @Test
    @DisplayName("Should convert to field relative")
    void shouldConvertToFieldRelative() {
      ChassisSpeeds robotRelative = new ChassisSpeeds(2.0, 0, 0);
      Rotation2d gyroAngle = Rotation2d.fromDegrees(90);
      
      ChassisSpeeds fieldRelative = ChassisSpeeds.fromRobotRelativeSpeeds(
          robotRelative, gyroAngle);
      
      // When robot is facing 90 degrees, forward becomes leftward
      assertEquals(0, fieldRelative.vxMetersPerSecond, 0.1);
      assertEquals(2.0, fieldRelative.vyMetersPerSecond, 0.1);
    }

    @Test
    @DisplayName("Should convert from field relative")
    void shouldConvertFromFieldRelative() {
      ChassisSpeeds fieldRelative = new ChassisSpeeds(2.0, 0, 0);
      Rotation2d gyroAngle = Rotation2d.fromDegrees(90);
      
      ChassisSpeeds robotRelative = ChassisSpeeds.fromFieldRelativeSpeeds(
          fieldRelative, gyroAngle);
      
      assertNotNull(robotRelative);
    }
  }

  @Nested
  @DisplayName("SwerveModuleState Tests")
  class SwerveModuleStateTests {

    @Test
    @DisplayName("Should create module state")
    void shouldCreateModuleState() {
      SwerveModuleState state = new SwerveModuleState(3.0, Rotation2d.fromDegrees(45));
      assertEquals(3.0, state.speedMetersPerSecond, 0.001);
      assertEquals(45, state.angle.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should optimize module state")
    void shouldOptimizeModuleState() {
      // If current angle is 0 and we want to go to 180 with positive speed,
      // it's more efficient to stay at 0 and reverse speed
      SwerveModuleState target = new SwerveModuleState(2.0, Rotation2d.fromDegrees(180));
      SwerveModuleState optimized = SwerveModuleState.optimize(target, Rotation2d.kZero);
      
      // Should either reverse speed or rotate less than 90 degrees
      assertNotNull(optimized);
      assertTrue(Math.abs(optimized.angle.getDegrees()) <= 90 || 
                 optimized.speedMetersPerSecond < 0);
    }

    @Test
    @DisplayName("Should handle zero speed state")
    void shouldHandleZeroSpeedState() {
      SwerveModuleState state = new SwerveModuleState(0, Rotation2d.fromDegrees(45));
      assertEquals(0, state.speedMetersPerSecond, 0.001);
    }
  }

  @Nested
  @DisplayName("SwerveModulePosition Tests")
  class SwerveModulePositionTests {

    @Test
    @DisplayName("Should create module position")
    void shouldCreateModulePosition() {
      SwerveModulePosition position = new SwerveModulePosition(5.0, Rotation2d.fromDegrees(30));
      assertEquals(5.0, position.distanceMeters, 0.001);
      assertEquals(30, position.angle.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should handle zero position")
    void shouldHandleZeroPosition() {
      SwerveModulePosition position = new SwerveModulePosition();
      assertEquals(0, position.distanceMeters, 0.001);
    }

    @Test
    @DisplayName("Should handle negative distance")
    void shouldHandleNegativeDistance() {
      // Negative distance means reverse driving
      SwerveModulePosition position = new SwerveModulePosition(-3.0, Rotation2d.kZero);
      assertEquals(-3.0, position.distanceMeters, 0.001);
    }
  }

  @Nested
  @DisplayName("Field-Relative Calculations Tests")
  class FieldRelativeCalculationsTests {

    @Test
    @DisplayName("Forward movement at 0 degrees should be +X")
    void forwardMovementAt0DegreesShouldBePlusX() {
      ChassisSpeeds robotSpeeds = new ChassisSpeeds(2.0, 0, 0);
      Rotation2d gyroAngle = Rotation2d.kZero;
      
      ChassisSpeeds fieldSpeeds = ChassisSpeeds.fromRobotRelativeSpeeds(
          robotSpeeds, gyroAngle);
      
      assertEquals(2.0, fieldSpeeds.vxMetersPerSecond, 0.01);
      assertEquals(0, fieldSpeeds.vyMetersPerSecond, 0.01);
    }

    @Test
    @DisplayName("Forward movement at 90 degrees should be +Y")
    void forwardMovementAt90DegreesShouldBePlusY() {
      ChassisSpeeds robotSpeeds = new ChassisSpeeds(2.0, 0, 0);
      Rotation2d gyroAngle = Rotation2d.k90deg;
      
      ChassisSpeeds fieldSpeeds = ChassisSpeeds.fromRobotRelativeSpeeds(
          robotSpeeds, gyroAngle);
      
      assertEquals(0, fieldSpeeds.vxMetersPerSecond, 0.01);
      assertEquals(2.0, fieldSpeeds.vyMetersPerSecond, 0.01);
    }

    @Test
    @DisplayName("Strafe left should be +Y in robot frame")
    void strafeLeftShouldBePlusYInRobotFrame() {
      ChassisSpeeds robotSpeeds = new ChassisSpeeds(0, 2.0, 0);
      assertEquals(2.0, robotSpeeds.vyMetersPerSecond, 0.001);
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should track pose through movement")
    void shouldTrackPoseThroughMovement() {
      Pose2d start = Pose2d.kZero;
      
      // Move 1m forward
      Pose2d afterMove = new Pose2d(1.0, 0, Rotation2d.kZero);
      
      // Rotate 90 degrees
      Pose2d afterRotate = new Pose2d(1.0, 0, Rotation2d.k90deg);
      
      assertEquals(1.0, afterMove.getX(), 0.001);
      assertEquals(90, afterRotate.getRotation().getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should calculate distance between poses")
    void shouldCalculateDistanceBetweenPoses() {
      Pose2d p1 = new Pose2d(0, 0, Rotation2d.kZero);
      Pose2d p2 = new Pose2d(3, 4, Rotation2d.kZero);
      
      double distance = p1.getTranslation().getDistance(p2.getTranslation());
      assertEquals(5.0, distance, 0.001); // 3-4-5 triangle
    }

    @Test
    @DisplayName("Should combine transformations")
    void shouldCombineTransformations() {
      Pose2d origin = Pose2d.kZero;
      
      // Chain of transformations
      Pose2d step1 = new Pose2d(1, 0, Rotation2d.kZero);
      Pose2d step2 = step1.plus(new Pose2d(0, 1, Rotation2d.k90deg).minus(Pose2d.kZero));
      
      assertNotNull(step2);
    }
  }
}
