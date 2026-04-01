package frc.robot.applicable.ctre;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.math.geometry.Rotation2d;
import frc.robot.applicable.ctre.ModuleIO.ModuleIOInputs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ModuleIO interface and ModuleIOInputs class representing swerve module hardware.
 */
class ModuleIOTest {

  /** Mock implementation of ModuleIO for testing swerve module behavior. */
  private static class MockModuleIO implements ModuleIO {
    // Drive motor state
    private double drivePositionRad = 0;
    private double driveVelocityRadPerSec = 0;
    private double driveAppliedVolts = 0;
    private double driveCurrentAmps = 0;
    private boolean driveConnected = true;

    // Turn motor state
    private Rotation2d turnAbsolutePosition = Rotation2d.kZero;
    private Rotation2d turnPosition = Rotation2d.kZero;
    private double turnVelocityRadPerSec = 0;
    private double turnAppliedVolts = 0;
    private double turnCurrentAmps = 0;
    private boolean turnConnected = true;
    private boolean turnEncoderConnected = true;

    // Odometry data
    private double[] odometryTimestamps = new double[0];
    private double[] odometryDrivePositionsRad = new double[0];
    private Rotation2d[] odometryTurnPositions = new Rotation2d[0];

    // Control tracking
    double lastDriveOpenLoop = 0;
    double lastTurnOpenLoop = 0;
    double lastDriveVelocitySetpoint = 0;
    Rotation2d lastTurnPositionSetpoint = Rotation2d.kZero;

    @Override
    public void updateInputs(ModuleIOInputs inputs) {
      inputs.driveConnected = driveConnected;
      inputs.drivePositionRad = drivePositionRad;
      inputs.driveVelocityRadPerSec = driveVelocityRadPerSec;
      inputs.driveAppliedVolts = driveAppliedVolts;
      inputs.driveCurrentAmps = driveCurrentAmps;

      inputs.turnConnected = turnConnected;
      inputs.turnEncoderConnected = turnEncoderConnected;
      inputs.turnAbsolutePosition = turnAbsolutePosition;
      inputs.turnPosition = turnPosition;
      inputs.turnVelocityRadPerSec = turnVelocityRadPerSec;
      inputs.turnAppliedVolts = turnAppliedVolts;
      inputs.turnCurrentAmps = turnCurrentAmps;

      inputs.odometryTimestamps = odometryTimestamps;
      inputs.odometryDrivePositionsRad = odometryDrivePositionsRad;
      inputs.odometryTurnPositions = odometryTurnPositions;
    }

    @Override
    public void setDriveOpenLoop(double output) {
      lastDriveOpenLoop = output;
      driveAppliedVolts = output * 12.0; // Assume 12V battery
    }

    @Override
    public void setTurnOpenLoop(double output) {
      lastTurnOpenLoop = output;
      turnAppliedVolts = output * 12.0;
    }

    @Override
    public void setDriveVelocity(double velocityRadPerSec) {
      lastDriveVelocitySetpoint = velocityRadPerSec;
      // Simulate reaching setpoint
      driveVelocityRadPerSec = velocityRadPerSec;
    }

    @Override
    public void setTurnPosition(Rotation2d rotation) {
      lastTurnPositionSetpoint = rotation;
      // Simulate reaching setpoint
      turnPosition = rotation;
      turnAbsolutePosition = rotation;
    }

    // Helper methods for test setup
    void setDriveState(double posRad, double velRadPerSec, double volts, double amps) {
      drivePositionRad = posRad;
      driveVelocityRadPerSec = velRadPerSec;
      driveAppliedVolts = volts;
      driveCurrentAmps = amps;
    }

    void setTurnState(Rotation2d absPos, Rotation2d pos, double velRadPerSec, double volts, double amps) {
      turnAbsolutePosition = absPos;
      turnPosition = pos;
      turnVelocityRadPerSec = velRadPerSec;
      turnAppliedVolts = volts;
      turnCurrentAmps = amps;
    }

    void setConnectionStatus(boolean drive, boolean turn, boolean encoder) {
      driveConnected = drive;
      turnConnected = turn;
      turnEncoderConnected = encoder;
    }

    void setOdometryData(double[] timestamps, double[] drivePos, Rotation2d[] turnPos) {
      odometryTimestamps = timestamps;
      odometryDrivePositionsRad = drivePos;
      odometryTurnPositions = turnPos;
    }
  }

  private MockModuleIO mockModule;
  private ModuleIOInputs inputs;

  @BeforeEach
  void setUp() {
    mockModule = new MockModuleIO();
    inputs = new ModuleIOInputs();
  }

  @Nested
  @DisplayName("ModuleIOInputs Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("Drive inputs should have default values")
    void driveInputsShouldHaveDefaultValues() {
      ModuleIOInputs defaultInputs = new ModuleIOInputs();
      assertFalse(defaultInputs.driveConnected);
      assertEquals(0.0, defaultInputs.drivePositionRad, 0.001);
      assertEquals(0.0, defaultInputs.driveVelocityRadPerSec, 0.001);
      assertEquals(0.0, defaultInputs.driveAppliedVolts, 0.001);
      assertEquals(0.0, defaultInputs.driveCurrentAmps, 0.001);
    }

    @Test
    @DisplayName("Turn inputs should have default values")
    void turnInputsShouldHaveDefaultValues() {
      ModuleIOInputs defaultInputs = new ModuleIOInputs();
      assertFalse(defaultInputs.turnConnected);
      assertFalse(defaultInputs.turnEncoderConnected);
      assertEquals(Rotation2d.kZero, defaultInputs.turnAbsolutePosition);
      assertEquals(Rotation2d.kZero, defaultInputs.turnPosition);
      assertEquals(0.0, defaultInputs.turnVelocityRadPerSec, 0.001);
      assertEquals(0.0, defaultInputs.turnAppliedVolts, 0.001);
      assertEquals(0.0, defaultInputs.turnCurrentAmps, 0.001);
    }

    @Test
    @DisplayName("Odometry arrays should be empty by default")
    void odometryArraysShouldBeEmptyByDefault() {
      ModuleIOInputs defaultInputs = new ModuleIOInputs();
      assertEquals(0, defaultInputs.odometryTimestamps.length);
      assertEquals(0, defaultInputs.odometryDrivePositionsRad.length);
      assertEquals(0, defaultInputs.odometryTurnPositions.length);
    }
  }

  @Nested
  @DisplayName("Drive Motor Tests")
  class DriveMotorTests {

    @Test
    @DisplayName("Should report drive position correctly")
    void shouldReportDrivePositionCorrectly() {
      mockModule.setDriveState(10.0, 5.0, 6.0, 20.0);
      mockModule.updateInputs(inputs);
      assertEquals(10.0, inputs.drivePositionRad, 0.001);
    }

    @Test
    @DisplayName("Should report drive velocity correctly")
    void shouldReportDriveVelocityCorrectly() {
      mockModule.setDriveState(0, 15.0, 0, 0);
      mockModule.updateInputs(inputs);
      assertEquals(15.0, inputs.driveVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should report drive voltage correctly")
    void shouldReportDriveVoltageCorrectly() {
      mockModule.setDriveState(0, 0, 8.5, 0);
      mockModule.updateInputs(inputs);
      assertEquals(8.5, inputs.driveAppliedVolts, 0.001);
    }

    @Test
    @DisplayName("Should report drive current correctly")
    void shouldReportDriveCurrentCorrectly() {
      mockModule.setDriveState(0, 0, 0, 35.0);
      mockModule.updateInputs(inputs);
      assertEquals(35.0, inputs.driveCurrentAmps, 0.001);
    }

    @Test
    @DisplayName("Should handle negative drive velocity")
    void shouldHandleNegativeDriveVelocity() {
      mockModule.setDriveState(0, -20.0, 0, 0);
      mockModule.updateInputs(inputs);
      assertEquals(-20.0, inputs.driveVelocityRadPerSec, 0.001);
    }
  }

  @Nested
  @DisplayName("Turn Motor Tests")
  class TurnMotorTests {

    @Test
    @DisplayName("Should report turn absolute position correctly")
    void shouldReportTurnAbsolutePositionCorrectly() {
      Rotation2d targetAngle = Rotation2d.fromDegrees(45);
      mockModule.setTurnState(targetAngle, targetAngle, 0, 0, 0);
      mockModule.updateInputs(inputs);
      assertEquals(45.0, inputs.turnAbsolutePosition.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should report turn position correctly")
    void shouldReportTurnPositionCorrectly() {
      Rotation2d targetAngle = Rotation2d.fromDegrees(90);
      mockModule.setTurnState(targetAngle, targetAngle, 0, 0, 0);
      mockModule.updateInputs(inputs);
      assertEquals(90.0, inputs.turnPosition.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should report turn velocity correctly")
    void shouldReportTurnVelocityCorrectly() {
      mockModule.setTurnState(Rotation2d.kZero, Rotation2d.kZero, Math.PI, 0, 0);
      mockModule.updateInputs(inputs);
      assertEquals(Math.PI, inputs.turnVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should handle full rotation angles")
    void shouldHandleFullRotationAngles() {
      Rotation2d fullRotation = Rotation2d.fromDegrees(360);
      mockModule.setTurnState(fullRotation, fullRotation, 0, 0, 0);
      mockModule.updateInputs(inputs);
      // Rotation2d normalizes to [0, 360)
      assertEquals(0.0, inputs.turnPosition.getDegrees(), 0.1);
    }

    @Test
    @DisplayName("Should handle negative angles")
    void shouldHandleNegativeAngles() {
      Rotation2d negativeAngle = Rotation2d.fromDegrees(-45);
      mockModule.setTurnState(negativeAngle, negativeAngle, 0, 0, 0);
      mockModule.updateInputs(inputs);
      assertEquals(-45.0, inputs.turnAbsolutePosition.getDegrees(), 0.001);
    }
  }

  @Nested
  @DisplayName("Connection Status Tests")
  class ConnectionStatusTests {

    @Test
    @DisplayName("Should report all connected when everything works")
    void shouldReportAllConnectedWhenEverythingWorks() {
      mockModule.setConnectionStatus(true, true, true);
      mockModule.updateInputs(inputs);
      assertTrue(inputs.driveConnected);
      assertTrue(inputs.turnConnected);
      assertTrue(inputs.turnEncoderConnected);
    }

    @Test
    @DisplayName("Should report drive disconnected")
    void shouldReportDriveDisconnected() {
      mockModule.setConnectionStatus(false, true, true);
      mockModule.updateInputs(inputs);
      assertFalse(inputs.driveConnected);
      assertTrue(inputs.turnConnected);
    }

    @Test
    @DisplayName("Should report turn disconnected")
    void shouldReportTurnDisconnected() {
      mockModule.setConnectionStatus(true, false, true);
      mockModule.updateInputs(inputs);
      assertTrue(inputs.driveConnected);
      assertFalse(inputs.turnConnected);
    }

    @Test
    @DisplayName("Should report encoder disconnected")
    void shouldReportEncoderDisconnected() {
      mockModule.setConnectionStatus(true, true, false);
      mockModule.updateInputs(inputs);
      assertTrue(inputs.turnConnected);
      assertFalse(inputs.turnEncoderConnected);
    }

    @Test
    @DisplayName("Should report all disconnected")
    void shouldReportAllDisconnected() {
      mockModule.setConnectionStatus(false, false, false);
      mockModule.updateInputs(inputs);
      assertFalse(inputs.driveConnected);
      assertFalse(inputs.turnConnected);
      assertFalse(inputs.turnEncoderConnected);
    }
  }

  @Nested
  @DisplayName("Open Loop Control Tests")
  class OpenLoopControlTests {

    @Test
    @DisplayName("setDriveOpenLoop should accept full forward")
    void setDriveOpenLoopShouldAcceptFullForward() {
      mockModule.setDriveOpenLoop(1.0);
      assertEquals(1.0, mockModule.lastDriveOpenLoop, 0.001);
      assertEquals(12.0, mockModule.driveAppliedVolts, 0.001);
    }

    @Test
    @DisplayName("setDriveOpenLoop should accept full reverse")
    void setDriveOpenLoopShouldAcceptFullReverse() {
      mockModule.setDriveOpenLoop(-1.0);
      assertEquals(-1.0, mockModule.lastDriveOpenLoop, 0.001);
      assertEquals(-12.0, mockModule.driveAppliedVolts, 0.001);
    }

    @Test
    @DisplayName("setDriveOpenLoop should accept zero")
    void setDriveOpenLoopShouldAcceptZero() {
      mockModule.setDriveOpenLoop(0.0);
      assertEquals(0.0, mockModule.lastDriveOpenLoop, 0.001);
      assertEquals(0.0, mockModule.driveAppliedVolts, 0.001);
    }

    @Test
    @DisplayName("setTurnOpenLoop should apply voltage")
    void setTurnOpenLoopShouldApplyVoltage() {
      mockModule.setTurnOpenLoop(0.5);
      assertEquals(0.5, mockModule.lastTurnOpenLoop, 0.001);
      assertEquals(6.0, mockModule.turnAppliedVolts, 0.001);
    }
  }

  @Nested
  @DisplayName("Velocity Control Tests")
  class VelocityControlTests {

    @Test
    @DisplayName("setDriveVelocity should set velocity setpoint")
    void setDriveVelocityShouldSetVelocitySetpoint() {
      mockModule.setDriveVelocity(10.0);
      assertEquals(10.0, mockModule.lastDriveVelocitySetpoint, 0.001);
    }

    @Test
    @DisplayName("setDriveVelocity should reach setpoint")
    void setDriveVelocityShouldReachSetpoint() {
      mockModule.setDriveVelocity(15.0);
      mockModule.updateInputs(inputs);
      assertEquals(15.0, inputs.driveVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("setDriveVelocity should handle zero")
    void setDriveVelocityShouldHandleZero() {
      mockModule.setDriveVelocity(0.0);
      assertEquals(0.0, mockModule.lastDriveVelocitySetpoint, 0.001);
    }

    @Test
    @DisplayName("setDriveVelocity should handle negative velocity")
    void setDriveVelocityShouldHandleNegativeVelocity() {
      mockModule.setDriveVelocity(-10.0);
      mockModule.updateInputs(inputs);
      assertEquals(-10.0, inputs.driveVelocityRadPerSec, 0.001);
    }
  }

  @Nested
  @DisplayName("Position Control Tests")
  class PositionControlTests {

    @Test
    @DisplayName("setTurnPosition should set position setpoint")
    void setTurnPositionShouldSetPositionSetpoint() {
      Rotation2d target = Rotation2d.fromDegrees(45);
      mockModule.setTurnPosition(target);
      assertEquals(45.0, mockModule.lastTurnPositionSetpoint.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("setTurnPosition should reach setpoint")
    void setTurnPositionShouldReachSetpoint() {
      Rotation2d target = Rotation2d.fromDegrees(90);
      mockModule.setTurnPosition(target);
      mockModule.updateInputs(inputs);
      assertEquals(90.0, inputs.turnPosition.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("setTurnPosition should handle zero")
    void setTurnPositionShouldHandleZero() {
      mockModule.setTurnPosition(Rotation2d.kZero);
      mockModule.updateInputs(inputs);
      assertEquals(0.0, inputs.turnPosition.getDegrees(), 0.001);
    }

    @Test
    @DisplayName("setTurnPosition should handle 180 degrees")
    void setTurnPositionShouldHandle180Degrees() {
      Rotation2d target = Rotation2d.fromDegrees(180);
      mockModule.setTurnPosition(target);
      mockModule.updateInputs(inputs);
      assertEquals(180.0, Math.abs(inputs.turnPosition.getDegrees()), 0.1);
    }
  }

  @Nested
  @DisplayName("Odometry Tests")
  class OdometryTests {

    @Test
    @DisplayName("Should return empty arrays when no odometry data")
    void shouldReturnEmptyArraysWhenNoOdometryData() {
      mockModule.setOdometryData(new double[0], new double[0], new Rotation2d[0]);
      mockModule.updateInputs(inputs);
      assertEquals(0, inputs.odometryTimestamps.length);
      assertEquals(0, inputs.odometryDrivePositionsRad.length);
      assertEquals(0, inputs.odometryTurnPositions.length);
    }

    @Test
    @DisplayName("Should populate timestamps correctly")
    void shouldPopulateTimestampsCorrectly() {
      double[] timestamps = {0.0, 0.02, 0.04, 0.06};
      mockModule.setOdometryData(timestamps, new double[4], new Rotation2d[4]);
      mockModule.updateInputs(inputs);

      assertEquals(4, inputs.odometryTimestamps.length);
      assertEquals(0.0, inputs.odometryTimestamps[0], 0.001);
      assertEquals(0.02, inputs.odometryTimestamps[1], 0.001);
      assertEquals(0.04, inputs.odometryTimestamps[2], 0.001);
      assertEquals(0.06, inputs.odometryTimestamps[3], 0.001);
    }

    @Test
    @DisplayName("Should populate drive positions correctly")
    void shouldPopulateDrivePositionsCorrectly() {
      double[] drivePos = {0.0, 1.0, 2.0, 3.0};
      mockModule.setOdometryData(new double[4], drivePos, new Rotation2d[4]);
      mockModule.updateInputs(inputs);

      assertEquals(4, inputs.odometryDrivePositionsRad.length);
      assertEquals(0.0, inputs.odometryDrivePositionsRad[0], 0.001);
      assertEquals(3.0, inputs.odometryDrivePositionsRad[3], 0.001);
    }

    @Test
    @DisplayName("Should populate turn positions correctly")
    void shouldPopulateTurnPositionsCorrectly() {
      Rotation2d[] turnPos = {
        Rotation2d.fromDegrees(0),
        Rotation2d.fromDegrees(30),
        Rotation2d.fromDegrees(60),
        Rotation2d.fromDegrees(90)
      };
      mockModule.setOdometryData(new double[4], new double[4], turnPos);
      mockModule.updateInputs(inputs);

      assertEquals(4, inputs.odometryTurnPositions.length);
      assertEquals(0.0, inputs.odometryTurnPositions[0].getDegrees(), 0.001);
      assertEquals(90.0, inputs.odometryTurnPositions[3].getDegrees(), 0.001);
    }
  }

  @Nested
  @DisplayName("Default Interface Implementation Tests")
  class DefaultImplementationTests {

    @Test
    @DisplayName("Default updateInputs should not throw")
    void defaultUpdateInputsShouldNotThrow() {
      ModuleIO defaultModule = new ModuleIO() {};
      ModuleIOInputs defaultInputs = new ModuleIOInputs();
      assertDoesNotThrow(() -> defaultModule.updateInputs(defaultInputs));
    }

    @Test
    @DisplayName("Default setDriveOpenLoop should not throw")
    void defaultSetDriveOpenLoopShouldNotThrow() {
      ModuleIO defaultModule = new ModuleIO() {};
      assertDoesNotThrow(() -> defaultModule.setDriveOpenLoop(1.0));
    }

    @Test
    @DisplayName("Default setTurnOpenLoop should not throw")
    void defaultSetTurnOpenLoopShouldNotThrow() {
      ModuleIO defaultModule = new ModuleIO() {};
      assertDoesNotThrow(() -> defaultModule.setTurnOpenLoop(0.5));
    }

    @Test
    @DisplayName("Default setDriveVelocity should not throw")
    void defaultSetDriveVelocityShouldNotThrow() {
      ModuleIO defaultModule = new ModuleIO() {};
      assertDoesNotThrow(() -> defaultModule.setDriveVelocity(10.0));
    }

    @Test
    @DisplayName("Default setTurnPosition should not throw")
    void defaultSetTurnPositionShouldNotThrow() {
      ModuleIO defaultModule = new ModuleIO() {};
      assertDoesNotThrow(() -> defaultModule.setTurnPosition(Rotation2d.fromDegrees(45)));
    }
  }

  @Nested
  @DisplayName("Swerve Module Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should coordinate drive and turn for straight movement")
    void shouldCoordinateDriveAndTurnForStraightMovement() {
      mockModule.setTurnPosition(Rotation2d.fromDegrees(0));
      mockModule.setDriveVelocity(5.0);
      mockModule.updateInputs(inputs);

      assertEquals(0.0, inputs.turnPosition.getDegrees(), 0.001);
      assertEquals(5.0, inputs.driveVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should coordinate drive and turn for 45 degree strafe")
    void shouldCoordinateDriveAndTurnFor45DegreeStrafe() {
      mockModule.setTurnPosition(Rotation2d.fromDegrees(45));
      mockModule.setDriveVelocity(10.0);
      mockModule.updateInputs(inputs);

      assertEquals(45.0, inputs.turnPosition.getDegrees(), 0.001);
      assertEquals(10.0, inputs.driveVelocityRadPerSec, 0.001);
    }

    @Test
    @DisplayName("Should handle rapid setpoint changes")
    void shouldHandleRapidSetpointChanges() {
      for (int i = 0; i < 100; i++) {
        double angle = (i * 3.6) % 360;
        double velocity = Math.sin(i * 0.1) * 10;
        
        mockModule.setTurnPosition(Rotation2d.fromDegrees(angle));
        mockModule.setDriveVelocity(velocity);
        mockModule.updateInputs(inputs);
        
        assertEquals(velocity, inputs.driveVelocityRadPerSec, 0.001);
      }
    }
  }
}
