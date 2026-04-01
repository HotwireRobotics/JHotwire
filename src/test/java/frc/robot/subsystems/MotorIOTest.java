package frc.robot.subsystems;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import frc.robot.subsystems.motors.MotorIO;
import frc.robot.subsystems.motors.MotorIO.Direction;
import frc.robot.subsystems.motors.MotorIO.FollowerMode;
import frc.robot.subsystems.motors.MotorIO.MotorInputs;
import frc.robot.subsystems.motors.MotorIO.NeutralMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the MotorIO interface and MotorInputs class. Uses a mock implementation to verify the
 * interface contract and input handling.
 */
class MotorIOTest {

  /** Mock implementation of MotorIO for testing interface behavior. */
  private static class MockMotorIO implements MotorIO {
    // Tracking fields for verifying method calls
    double lastVoltage = 0;
    double lastPosition = 0;
    double lastVelocity = 0;
    double lastPercent = 0;
    double lastCurrentLimit = 0;
    Direction lastDirection = Direction.FORWARD;
    NeutralMode lastNeutralMode = NeutralMode.COAST;
    int lastLeaderId = -1;
    FollowerMode lastFollowerMode = FollowerMode.ALIGNED;
    boolean stopped = false;
    
    // PID/FF tracking
    double kP = 0, kI = 0, kD = 0, kS = 0, kV = 0, kA = 0;

    private final int deviceId;

    MockMotorIO(int deviceId) {
      this.deviceId = deviceId;
    }

    @Override
    public void runVoltage(edu.wpi.first.units.measure.Voltage volts) {
      lastVoltage = volts.in(Volts);
      stopped = false;
    }

    @Override
    public void stop() {
      stopped = true;
      lastVoltage = 0;
    }

    @Override
    public void updateInputs(MotorInputs inputs) {
      inputs.position = lastPosition;
      inputs.velocity = lastVelocity;
      inputs.voltage = lastVoltage;
      inputs.connected = true;
    }

    @Override
    public void setCurrentLimit(edu.wpi.first.units.measure.Current limit) {
      lastCurrentLimit = limit.in(Amps);
    }

    @Override
    public void setDirection(Direction direction) {
      lastDirection = direction;
    }

    @Override
    public void setNeutralMode(NeutralMode mode) {
      lastNeutralMode = mode;
    }

    @Override
    public void follow(frc.robot.subsystems.motors.Motor leader, FollowerMode mode) {
      lastLeaderId = leader.getID();
      lastFollowerMode = mode;
    }

    @Override
    public void follow(int leader, FollowerMode mode) {
      lastLeaderId = leader;
      lastFollowerMode = mode;
    }

    @Override
    public void configureProportional(double kP) {
      this.kP = kP;
    }

    @Override
    public void configureIntegral(double kI) {
      this.kI = kI;
    }

    @Override
    public void configureDerivative(double kD) {
      this.kD = kD;
    }

    @Override
    public void configureStaticFriction(double kS) {
      this.kS = kS;
    }

    @Override
    public void configureVelocity(double kV) {
      this.kV = kV;
    }

    @Override
    public void configureAcceleration(double kA) {
      this.kA = kA;
    }

    @Override
    public void runPosition(edu.wpi.first.units.measure.Angle position) {
      lastPosition = position.in(Rotations);
      stopped = false;
    }

    @Override
    public void runVelocity(edu.wpi.first.units.measure.AngularVelocity velocity) {
      lastVelocity = velocity.in(RotationsPerSecond);
      stopped = false;
    }

    @Override
    public void runPercent(double percent) {
      lastPercent = percent;
      stopped = false;
    }

    @Override
    public edu.wpi.first.units.measure.AngularVelocity getVelocity() {
      return RotationsPerSecond.of(lastVelocity);
    }

    @Override
    public edu.wpi.first.units.measure.Angle getPosition() {
      return Rotations.of(lastPosition);
    }

    @Override
    public edu.wpi.first.units.measure.Current getCurrent() {
      return Amps.of(0);
    }

    @Override
    public int getID() {
      return deviceId;
    }
  }

  private MockMotorIO mockMotor;
  private MotorInputs inputs;

  @BeforeEach
  void setUp() {
    mockMotor = new MockMotorIO(1);
    inputs = new MotorInputs();
  }

  @Nested
  @DisplayName("MotorInputs Tests")
  class MotorInputsTests {

    @Test
    @DisplayName("MotorInputs should have default values")
    void motorInputsShouldHaveDefaultValues() {
      MotorInputs testInputs = new MotorInputs();

      assertEquals(0.0, testInputs.position);
      assertEquals(0.0, testInputs.velocity);
      assertEquals(0.0, testInputs.acceleration);
      assertEquals(0.0, testInputs.voltage);
      assertEquals(0.0, testInputs.supplyVoltage);
      assertEquals(0.0, testInputs.current);
      assertEquals(0.0, testInputs.statorCurrent);
      assertEquals(0.0, testInputs.temperature);
      assertEquals(0.0, testInputs.appliedOutput);
      assertEquals(0.0, testInputs.dutyCycle);
      assertTrue(testInputs.connected);
    }

    @Test
    @DisplayName("MotorInputs fields should be mutable")
    void motorInputsFieldsShouldBeMutable() {
      inputs.position = 5.0;
      inputs.velocity = 10.0;
      inputs.acceleration = 2.0;
      inputs.voltage = 12.0;
      inputs.current = 30.0;
      inputs.temperature = 45.0;
      inputs.connected = false;

      assertEquals(5.0, inputs.position);
      assertEquals(10.0, inputs.velocity);
      assertEquals(2.0, inputs.acceleration);
      assertEquals(12.0, inputs.voltage);
      assertEquals(30.0, inputs.current);
      assertEquals(45.0, inputs.temperature);
      assertFalse(inputs.connected);
    }
  }

  @Nested
  @DisplayName("Voltage Control Tests")
  class VoltageControlTests {

    @Test
    @DisplayName("runVoltage should accept positive voltage")
    void runVoltageShouldAcceptPositiveVoltage() {
      mockMotor.runVoltage(Volts.of(6.0));
      assertEquals(6.0, mockMotor.lastVoltage, 0.001);
      assertFalse(mockMotor.stopped);
    }

    @Test
    @DisplayName("runVoltage should accept negative voltage")
    void runVoltageShouldAcceptNegativeVoltage() {
      mockMotor.runVoltage(Volts.of(-6.0));
      assertEquals(-6.0, mockMotor.lastVoltage, 0.001);
    }

    @Test
    @DisplayName("runVoltage should accept zero voltage")
    void runVoltageShouldAcceptZeroVoltage() {
      mockMotor.runVoltage(Volts.of(0.0));
      assertEquals(0.0, mockMotor.lastVoltage, 0.001);
    }

    @Test
    @DisplayName("runVoltage should handle max voltage")
    void runVoltageShouldHandleMaxVoltage() {
      mockMotor.runVoltage(Volts.of(12.0));
      assertEquals(12.0, mockMotor.lastVoltage, 0.001);
    }
  }

  @Nested
  @DisplayName("Stop Tests")
  class StopTests {

    @Test
    @DisplayName("stop should set stopped flag")
    void stopShouldSetStoppedFlag() {
      mockMotor.runVoltage(Volts.of(6.0));
      assertFalse(mockMotor.stopped);

      mockMotor.stop();
      assertTrue(mockMotor.stopped);
    }

    @Test
    @DisplayName("stop should reset voltage to zero")
    void stopShouldResetVoltageToZero() {
      mockMotor.runVoltage(Volts.of(12.0));
      mockMotor.stop();
      assertEquals(0.0, mockMotor.lastVoltage);
    }
  }

  @Nested
  @DisplayName("Position Control Tests")
  class PositionControlTests {

    @Test
    @DisplayName("runPosition should accept positive angle")
    void runPositionShouldAcceptPositiveAngle() {
      mockMotor.runPosition(Rotations.of(5.0));
      assertEquals(5.0, mockMotor.lastPosition, 0.001);
    }

    @Test
    @DisplayName("runPosition should accept negative angle")
    void runPositionShouldAcceptNegativeAngle() {
      mockMotor.runPosition(Rotations.of(-3.0));
      assertEquals(-3.0, mockMotor.lastPosition, 0.001);
    }

    @Test
    @DisplayName("runPosition should convert from degrees")
    void runPositionShouldConvertFromDegrees() {
      mockMotor.runPosition(Degrees.of(360));
      assertEquals(1.0, mockMotor.lastPosition, 0.001);
    }

    @Test
    @DisplayName("getPosition should return last set position")
    void getPositionShouldReturnLastSetPosition() {
      mockMotor.runPosition(Rotations.of(7.5));
      assertEquals(7.5, mockMotor.getPosition().in(Rotations), 0.001);
    }
  }

  @Nested
  @DisplayName("Velocity Control Tests")
  class VelocityControlTests {

    @Test
    @DisplayName("runVelocity should accept positive velocity")
    void runVelocityShouldAcceptPositiveVelocity() {
      mockMotor.runVelocity(RotationsPerSecond.of(100));
      assertEquals(100.0, mockMotor.lastVelocity, 0.001);
    }

    @Test
    @DisplayName("runVelocity should accept negative velocity")
    void runVelocityShouldAcceptNegativeVelocity() {
      mockMotor.runVelocity(RotationsPerSecond.of(-50));
      assertEquals(-50.0, mockMotor.lastVelocity, 0.001);
    }

    @Test
    @DisplayName("runVelocity should convert from RPM")
    void runVelocityShouldConvertFromRpm() {
      mockMotor.runVelocity(RPM.of(600)); // 10 RPS
      assertEquals(10.0, mockMotor.lastVelocity, 0.001);
    }

    @Test
    @DisplayName("getVelocity should return last set velocity")
    void getVelocityShouldReturnLastSetVelocity() {
      mockMotor.runVelocity(RotationsPerSecond.of(25));
      assertEquals(25.0, mockMotor.getVelocity().in(RotationsPerSecond), 0.001);
    }
  }

  @Nested
  @DisplayName("Percent Output Tests")
  class PercentOutputTests {

    @Test
    @DisplayName("runPercent should accept value in range")
    void runPercentShouldAcceptValueInRange() {
      mockMotor.runPercent(0.5);
      assertEquals(0.5, mockMotor.lastPercent, 0.001);
    }

    @Test
    @DisplayName("runPercent should accept full forward")
    void runPercentShouldAcceptFullForward() {
      mockMotor.runPercent(1.0);
      assertEquals(1.0, mockMotor.lastPercent, 0.001);
    }

    @Test
    @DisplayName("runPercent should accept full reverse")
    void runPercentShouldAcceptFullReverse() {
      mockMotor.runPercent(-1.0);
      assertEquals(-1.0, mockMotor.lastPercent, 0.001);
    }

    @Test
    @DisplayName("runPercent should accept zero")
    void runPercentShouldAcceptZero() {
      mockMotor.runPercent(0.0);
      assertEquals(0.0, mockMotor.lastPercent, 0.001);
    }
  }

  @Nested
  @DisplayName("Current Limit Tests")
  class CurrentLimitTests {

    @Test
    @DisplayName("setCurrentLimit should store limit value")
    void setCurrentLimitShouldStoreLimitValue() {
      mockMotor.setCurrentLimit(Amps.of(40));
      assertEquals(40.0, mockMotor.lastCurrentLimit, 0.001);
    }

    @Test
    @DisplayName("setCurrentLimit should handle high values")
    void setCurrentLimitShouldHandleHighValues() {
      mockMotor.setCurrentLimit(Amps.of(80));
      assertEquals(80.0, mockMotor.lastCurrentLimit, 0.001);
    }
  }

  @Nested
  @DisplayName("Direction and Neutral Mode Tests")
  class DirectionNeutralModeTests {

    @Test
    @DisplayName("setDirection should set forward direction")
    void setDirectionShouldSetForwardDirection() {
      mockMotor.setDirection(Direction.FORWARD);
      assertEquals(Direction.FORWARD, mockMotor.lastDirection);
    }

    @Test
    @DisplayName("setDirection should set reverse direction")
    void setDirectionShouldSetReverseDirection() {
      mockMotor.setDirection(Direction.REVERSE);
      assertEquals(Direction.REVERSE, mockMotor.lastDirection);
    }

    @Test
    @DisplayName("setNeutralMode should set coast mode")
    void setNeutralModeShouldSetCoastMode() {
      mockMotor.setNeutralMode(NeutralMode.COAST);
      assertEquals(NeutralMode.COAST, mockMotor.lastNeutralMode);
    }

    @Test
    @DisplayName("setNeutralMode should set brake mode")
    void setNeutralModeShouldSetBrakeMode() {
      mockMotor.setNeutralMode(NeutralMode.BRAKE);
      assertEquals(NeutralMode.BRAKE, mockMotor.lastNeutralMode);
    }
  }

  @Nested
  @DisplayName("Follower Tests")
  class FollowerTests {

    @Test
    @DisplayName("follow by ID should set leader and mode")
    void followByIdShouldSetLeaderAndMode() {
      mockMotor.follow(5, FollowerMode.ALIGNED);
      assertEquals(5, mockMotor.lastLeaderId);
      assertEquals(FollowerMode.ALIGNED, mockMotor.lastFollowerMode);
    }

    @Test
    @DisplayName("follow should support inverse mode")
    void followShouldSupportInverseMode() {
      mockMotor.follow(10, FollowerMode.INVERSE);
      assertEquals(10, mockMotor.lastLeaderId);
      assertEquals(FollowerMode.INVERSE, mockMotor.lastFollowerMode);
    }
  }

  @Nested
  @DisplayName("PID Configuration Tests")
  class PidConfigurationTests {

    @Test
    @DisplayName("configureProportional should store kP")
    void configureProportionalShouldStoreKp() {
      mockMotor.configureProportional(1.5);
      assertEquals(1.5, mockMotor.kP, 0.001);
    }

    @Test
    @DisplayName("configureIntegral should store kI")
    void configureIntegralShouldStoreKi() {
      mockMotor.configureIntegral(0.01);
      assertEquals(0.01, mockMotor.kI, 0.001);
    }

    @Test
    @DisplayName("configureDerivative should store kD")
    void configureDerivativeShouldStoreKd() {
      mockMotor.configureDerivative(0.1);
      assertEquals(0.1, mockMotor.kD, 0.001);
    }
  }

  @Nested
  @DisplayName("Feedforward Configuration Tests")
  class FeedforwardConfigurationTests {

    @Test
    @DisplayName("configureStaticFriction should store kS")
    void configureStaticFrictionShouldStoreKs() {
      mockMotor.configureStaticFriction(0.2);
      assertEquals(0.2, mockMotor.kS, 0.001);
    }

    @Test
    @DisplayName("configureVelocity should store kV")
    void configureVelocityShouldStoreKv() {
      mockMotor.configureVelocity(0.12);
      assertEquals(0.12, mockMotor.kV, 0.001);
    }

    @Test
    @DisplayName("configureAcceleration should store kA")
    void configureAccelerationShouldStoreKa() {
      mockMotor.configureAcceleration(0.01);
      assertEquals(0.01, mockMotor.kA, 0.001);
    }
  }

  @Nested
  @DisplayName("Device ID Tests")
  class DeviceIdTests {

    @Test
    @DisplayName("getID should return device ID")
    void getIdShouldReturnDeviceId() {
      assertEquals(1, mockMotor.getID());
    }

    @Test
    @DisplayName("Different motors should have different IDs")
    void differentMotorsShouldHaveDifferentIds() {
      MockMotorIO motor2 = new MockMotorIO(2);
      MockMotorIO motor3 = new MockMotorIO(3);

      assertNotEquals(mockMotor.getID(), motor2.getID());
      assertNotEquals(motor2.getID(), motor3.getID());
    }
  }

  @Nested
  @DisplayName("Update Inputs Tests")
  class UpdateInputsTests {

    @Test
    @DisplayName("updateInputs should populate position")
    void updateInputsShouldPopulatePosition() {
      mockMotor.runPosition(Rotations.of(10));
      mockMotor.updateInputs(inputs);
      assertEquals(10.0, inputs.position, 0.001);
    }

    @Test
    @DisplayName("updateInputs should populate velocity")
    void updateInputsShouldPopulateVelocity() {
      mockMotor.runVelocity(RotationsPerSecond.of(50));
      mockMotor.updateInputs(inputs);
      assertEquals(50.0, inputs.velocity, 0.001);
    }

    @Test
    @DisplayName("updateInputs should populate voltage")
    void updateInputsShouldPopulateVoltage() {
      mockMotor.runVoltage(Volts.of(8));
      mockMotor.updateInputs(inputs);
      assertEquals(8.0, inputs.voltage, 0.001);
    }

    @Test
    @DisplayName("updateInputs should set connected flag")
    void updateInputsShouldSetConnectedFlag() {
      mockMotor.updateInputs(inputs);
      assertTrue(inputs.connected);
    }
  }
}
