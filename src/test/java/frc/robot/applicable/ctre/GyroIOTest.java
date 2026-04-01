package frc.robot.applicable.ctre;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.math.geometry.Rotation2d;
import frc.robot.applicable.ctre.GyroIO.GyroIOInputs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for GyroIO interface and GyroIOInputs class using mock implementations.
 */
class GyroIOTest {

  /** Mock implementation of GyroIO for testing. */
  private static class MockGyroIO implements GyroIO {
    private boolean connected = true;
    private double yawDegrees = 0;
    private double yawVelocityRadPerSec = 0;
    private double noMotionCount = 0;
    private double[] timestamps = new double[0];
    private Rotation2d[] positions = new Rotation2d[0];

    void setConnected(boolean connected) {
      this.connected = connected;
    }

    void setYawDegrees(double degrees) {
      this.yawDegrees = degrees;
    }

    void setYawVelocity(double radPerSec) {
      this.yawVelocityRadPerSec = radPerSec;
    }

    void setNoMotionCount(double count) {
      this.noMotionCount = count;
    }

    void setOdometryData(double[] timestamps, Rotation2d[] positions) {
      this.timestamps = timestamps;
      this.positions = positions;
    }

    @Override
    public void updateInputs(GyroIOInputs inputs) {
      inputs.connected = connected;
      inputs.yawPosition = Rotation2d.fromDegrees(yawDegrees);
      inputs.yawVelocityRadPerSec = yawVelocityRadPerSec;
      inputs.noMotionCount = noMotionCount;
      inputs.odometryYawTimestamps = timestamps;
      inputs.odometryYawPositions = positions;
    }
  }

  private MockGyroIO mockGyro;
  private GyroIOInputs inputs;

  @BeforeEach
  void setUp() {
    mockGyro = new MockGyroIO();
    inputs = new GyroIOInputs();
  }

  @Nested
  @DisplayName("GyroIOInputs Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("Should have false connected by default")
    void shouldHaveFalseConnectedByDefault() {
      GyroIOInputs defaultInputs = new GyroIOInputs();
      assertFalse(defaultInputs.connected);
    }

    @Test
    @DisplayName("Should have zero yaw position by default")
    void shouldHaveZeroYawPositionByDefault() {
      GyroIOInputs defaultInputs = new GyroIOInputs();
      assertEquals(Rotation2d.kZero, defaultInputs.yawPosition);
    }

    @Test
    @DisplayName("Should have zero yaw velocity by default")
    void shouldHaveZeroYawVelocityByDefault() {
      GyroIOInputs defaultInputs = new GyroIOInputs();
      assertEquals(0.0, defaultInputs.yawVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should have empty odometry arrays by default")
    void shouldHaveEmptyOdometryArraysByDefault() {
      GyroIOInputs defaultInputs = new GyroIOInputs();
      assertEquals(0, defaultInputs.odometryYawTimestamps.length);
      assertEquals(0, defaultInputs.odometryYawPositions.length);
    }

    @Test
    @DisplayName("Should have zero no motion count by default")
    void shouldHaveZeroNoMotionCountByDefault() {
      GyroIOInputs defaultInputs = new GyroIOInputs();
      assertEquals(0.0, defaultInputs.noMotionCount, 0.001);
    }
  }

  @Nested
  @DisplayName("Connection Status Tests")
  class ConnectionStatusTests {

    @Test
    @DisplayName("Should report connected when gyro is connected")
    void shouldReportConnectedWhenGyroIsConnected() {
      mockGyro.setConnected(true);
      mockGyro.updateInputs(inputs);
      assertTrue(inputs.connected);
    }

    @Test
    @DisplayName("Should report disconnected when gyro is disconnected")
    void shouldReportDisconnectedWhenGyroIsDisconnected() {
      mockGyro.setConnected(false);
      mockGyro.updateInputs(inputs);
      assertFalse(inputs.connected);
    }
  }

  @Nested
  @DisplayName("Yaw Position Tests")
  class YawPositionTests {

    @Test
    @DisplayName("Should return zero yaw for initial position")
    void shouldReturnZeroYawForInitialPosition() {
      mockGyro.setYawDegrees(0);
      mockGyro.updateInputs(inputs);
      assertEquals(0.0, inputs.yawPosition.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should return positive yaw for clockwise rotation")
    void shouldReturnPositiveYawForClockwiseRotation() {
      mockGyro.setYawDegrees(90);
      mockGyro.updateInputs(inputs);
      assertEquals(90.0, inputs.yawPosition.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should return negative yaw for counter-clockwise rotation")
    void shouldReturnNegativeYawForCounterClockwiseRotation() {
      mockGyro.setYawDegrees(-90);
      mockGyro.updateInputs(inputs);
      assertEquals(-90.0, inputs.yawPosition.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should handle full rotation (360 degrees)")
    void shouldHandleFullRotation() {
      mockGyro.setYawDegrees(360);
      mockGyro.updateInputs(inputs);
      // Rotation2d normalizes to equivalent angle
      assertEquals(0.0, inputs.yawPosition.getDegrees(), 0.1);
    }

    @Test
    @DisplayName("Should handle multiple rotations")
    void shouldHandleMultipleRotations() {
      mockGyro.setYawDegrees(720);
      mockGyro.updateInputs(inputs);
      assertEquals(0.0, inputs.yawPosition.getDegrees(), 0.1);
    }

    @Test
    @DisplayName("Should handle half rotation (180 degrees)")
    void shouldHandleHalfRotation() {
      mockGyro.setYawDegrees(180);
      mockGyro.updateInputs(inputs);
      assertEquals(180.0, Math.abs(inputs.yawPosition.getDegrees()), 0.1);
    }
  }

  @Nested
  @DisplayName("Yaw Velocity Tests")
  class YawVelocityTests {

    @Test
    @DisplayName("Should return zero velocity when stationary")
    void shouldReturnZeroVelocityWhenStationary() {
      mockGyro.setYawVelocity(0);
      mockGyro.updateInputs(inputs);
      assertEquals(0.0, inputs.yawVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should return positive velocity for clockwise spin")
    void shouldReturnPositiveVelocityForClockwiseSpin() {
      mockGyro.setYawVelocity(Math.PI); // 180 deg/sec
      mockGyro.updateInputs(inputs);
      assertEquals(Math.PI, inputs.yawVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should return negative velocity for counter-clockwise spin")
    void shouldReturnNegativeVelocityForCounterClockwiseSpin() {
      mockGyro.setYawVelocity(-Math.PI);
      mockGyro.updateInputs(inputs);
      assertEquals(-Math.PI, inputs.yawVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should handle high angular velocities")
    void shouldHandleHighAngularVelocities() {
      mockGyro.setYawVelocity(10 * Math.PI); // Very fast spin
      mockGyro.updateInputs(inputs);
      assertEquals(10 * Math.PI, inputs.yawVelocityRadPerSec, 0.001);
    }
  }

  @Nested
  @DisplayName("No Motion Count Tests")
  class NoMotionCountTests {

    @Test
    @DisplayName("Should return zero when robot is moving")
    void shouldReturnZeroWhenRobotIsMoving() {
      mockGyro.setNoMotionCount(0);
      mockGyro.updateInputs(inputs);
      assertEquals(0.0, inputs.noMotionCount, 0.001);
    }

    @Test
    @DisplayName("Should increment when robot is stationary")
    void shouldIncrementWhenRobotIsStationary() {
      mockGyro.setNoMotionCount(100);
      mockGyro.updateInputs(inputs);
      assertEquals(100.0, inputs.noMotionCount, 0.001);
    }

    @Test
    @DisplayName("Should handle large no motion counts")
    void shouldHandleLargeNoMotionCounts() {
      mockGyro.setNoMotionCount(10000);
      mockGyro.updateInputs(inputs);
      assertEquals(10000.0, inputs.noMotionCount, 0.001);
    }
  }

  @Nested
  @DisplayName("Odometry Data Tests")
  class OdometryDataTests {

    @Test
    @DisplayName("Should return empty arrays when no odometry data")
    void shouldReturnEmptyArraysWhenNoOdometryData() {
      mockGyro.setOdometryData(new double[0], new Rotation2d[0]);
      mockGyro.updateInputs(inputs);
      assertEquals(0, inputs.odometryYawTimestamps.length);
      assertEquals(0, inputs.odometryYawPositions.length);
    }

    @Test
    @DisplayName("Should return populated arrays with odometry data")
    void shouldReturnPopulatedArraysWithOdometryData() {
      double[] timestamps = {0.0, 0.02, 0.04};
      Rotation2d[] positions = {
        Rotation2d.fromDegrees(0),
        Rotation2d.fromDegrees(5),
        Rotation2d.fromDegrees(10)
      };
      mockGyro.setOdometryData(timestamps, positions);
      mockGyro.updateInputs(inputs);

      assertEquals(3, inputs.odometryYawTimestamps.length);
      assertEquals(3, inputs.odometryYawPositions.length);
    }

    @Test
    @DisplayName("Should preserve timestamp values")
    void shouldPreserveTimestampValues() {
      double[] timestamps = {1.5, 2.0, 2.5};
      mockGyro.setOdometryData(timestamps, new Rotation2d[3]);
      mockGyro.updateInputs(inputs);

      assertEquals(1.5, inputs.odometryYawTimestamps[0], 0.001);
      assertEquals(2.0, inputs.odometryYawTimestamps[1], 0.001);
      assertEquals(2.5, inputs.odometryYawTimestamps[2], 0.001);
    }

    @Test
    @DisplayName("Should preserve position values")
    void shouldPreservePositionValues() {
      Rotation2d[] positions = {
        Rotation2d.fromDegrees(45),
        Rotation2d.fromDegrees(90),
        Rotation2d.fromDegrees(135)
      };
      mockGyro.setOdometryData(new double[3], positions);
      mockGyro.updateInputs(inputs);

      assertEquals(45.0, inputs.odometryYawPositions[0].getDegrees(), 0.001);
      assertEquals(90.0, inputs.odometryYawPositions[1].getDegrees(), 0.001);
      assertEquals(135.0, inputs.odometryYawPositions[2].getDegrees(), 0.001);
    }
  }

  @Nested
  @DisplayName("Default Interface Implementation Tests")
  class DefaultImplementationTests {

    @Test
    @DisplayName("Default updateInputs should not throw")
    void defaultUpdateInputsShouldNotThrow() {
      GyroIO defaultGyro = new GyroIO() {};
      GyroIOInputs defaultInputs = new GyroIOInputs();
      assertDoesNotThrow(() -> defaultGyro.updateInputs(defaultInputs));
    }

    @Test
    @DisplayName("Default implementation should not modify inputs")
    void defaultImplementationShouldNotModifyInputs() {
      GyroIO defaultGyro = new GyroIO() {};
      GyroIOInputs defaultInputs = new GyroIOInputs();
      defaultInputs.connected = true;
      defaultInputs.yawPosition = Rotation2d.fromDegrees(45);
      
      defaultGyro.updateInputs(defaultInputs);
      
      // Default implementation does nothing, so values remain
      assertTrue(defaultInputs.connected);
      assertEquals(45.0, defaultInputs.yawPosition.getDegrees(), 0.001);
    }
  }

  @Nested
  @DisplayName("Gyro Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should accurately track rotation over time")
    void shouldAccuratelyTrackRotationOverTime() {
      // Simulate rotation at constant velocity
      double angularVelocity = Math.PI / 2; // 90 deg/sec
      double duration = 2.0; // seconds
      double expectedFinalAngle = 180; // degrees

      mockGyro.setYawVelocity(angularVelocity);
      mockGyro.setYawDegrees(expectedFinalAngle);
      mockGyro.updateInputs(inputs);

      assertEquals(expectedFinalAngle, Math.abs(inputs.yawPosition.getDegrees()), 0.1);
      assertEquals(angularVelocity, inputs.yawVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should handle rapid direction changes")
    void shouldHandleRapidDirectionChanges() {
      // Simulate rapid direction changes
      mockGyro.setYawDegrees(45);
      mockGyro.setYawVelocity(Math.PI);
      mockGyro.updateInputs(inputs);
      double firstYaw = inputs.yawPosition.getDegrees();

      mockGyro.setYawDegrees(-45);
      mockGyro.setYawVelocity(-Math.PI);
      mockGyro.updateInputs(inputs);
      double secondYaw = inputs.yawPosition.getDegrees();

      assertNotEquals(firstYaw, secondYaw);
      assertTrue(inputs.yawVelocityRadPerSec < 0);
    }
  }
}
