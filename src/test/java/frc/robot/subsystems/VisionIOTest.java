package frc.robot.subsystems;

import static edu.wpi.first.units.Units.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.Time;
import frc.robot.subsystems.vision.VisionIO;
import frc.robot.subsystems.vision.VisionIO.Measurement;
import frc.robot.subsystems.vision.VisionIO.VisionInputs;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for VisionIO interface and related classes. Tests vision measurement handling, pose
 * estimation inputs, and standard deviation calculations.
 */
class VisionIOTest {

  /** Mock implementation of VisionIO for testing. */
  private static class MockVisionIO implements VisionIO {
    private final List<Measurement> measurements = new ArrayList<>();
    private boolean detecting = false;
    private double[] distances = new double[0];
    private Pose2d[] estimates = new Pose2d[0];
    private int tagCount = 0;

    void addMeasurement(Measurement measurement) {
      measurements.add(measurement);
    }

    void clearMeasurements() {
      measurements.clear();
    }

    void setDetecting(boolean detecting) {
      this.detecting = detecting;
    }

    void setDistances(double... distances) {
      this.distances = distances;
    }

    void setEstimates(Pose2d... estimates) {
      this.estimates = estimates;
    }

    void setTagCount(int count) {
      this.tagCount = count;
    }

    @Override
    public List<Measurement> getMeasurements() {
      return new ArrayList<>(measurements);
    }

    @Override
    public void updateInputs(VisionInputs inputs) {
      inputs.detecting = detecting;
      inputs.distances = distances;
      inputs.estimates = estimates;
      inputs.count = tagCount;
    }
  }

  private MockVisionIO mockVision;
  private VisionInputs inputs;

  @BeforeEach
  void setUp() {
    mockVision = new MockVisionIO();
    inputs = new VisionInputs();
  }

  @Nested
  @DisplayName("VisionInputs Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("Should have false detecting by default")
    void shouldHaveFalseDetectingByDefault() {
      VisionInputs defaultInputs = new VisionInputs();
      assertFalse(defaultInputs.detecting);
    }

    @Test
    @DisplayName("Should have empty distances array by default")
    void shouldHaveEmptyDistancesArrayByDefault() {
      VisionInputs defaultInputs = new VisionInputs();
      assertEquals(0, defaultInputs.distances.length);
    }

    @Test
    @DisplayName("Should have empty estimates array by default")
    void shouldHaveEmptyEstimatesArrayByDefault() {
      VisionInputs defaultInputs = new VisionInputs();
      assertEquals(0, defaultInputs.estimates.length);
    }

    @Test
    @DisplayName("Should have zero tag count by default")
    void shouldHaveZeroTagCountByDefault() {
      VisionInputs defaultInputs = new VisionInputs();
      assertEquals(0, defaultInputs.count);
    }
  }

  @Nested
  @DisplayName("Measurement Class Tests")
  class MeasurementTests {

    @Test
    @DisplayName("Measurement should store pose correctly")
    void measurementShouldStorePoseCorrectly() {
      Pose2d pose = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(45));
      Distance distance = Meters.of(5.0);
      Time timestamp = Seconds.of(1.5);
      int count = 2;
      Matrix<N3, N1> devs = new Matrix<>(N3.instance, N1.instance);

      Measurement measurement = new Measurement(pose, distance, timestamp, count, devs);

      assertEquals(pose, measurement.pose);
      assertEquals(distance, measurement.distance);
      assertEquals(timestamp, measurement.timestamp);
      assertEquals(count, measurement.count);
      assertEquals(devs, measurement.devs);
    }

    @Test
    @DisplayName("Measurement should handle zero values")
    void measurementShouldHandleZeroValues() {
      Pose2d pose = new Pose2d(0, 0, Rotation2d.kZero);
      Distance distance = Meters.of(0);
      Time timestamp = Seconds.of(0);
      int count = 0;
      Matrix<N3, N1> devs = new Matrix<>(N3.instance, N1.instance);

      Measurement measurement = new Measurement(pose, distance, timestamp, count, devs);

      assertEquals(0, measurement.pose.getX(), 0.001);
      assertEquals(0, measurement.distance.in(Meters), 0.001);
      assertEquals(0, measurement.timestamp.in(Seconds), 0.001);
      assertEquals(0, measurement.count);
    }

    @Test
    @DisplayName("Measurement should handle large distances")
    void measurementShouldHandleLargeDistances() {
      Pose2d pose = new Pose2d(8.0, 4.0, Rotation2d.kZero);
      Distance distance = Meters.of(10.0);
      Time timestamp = Seconds.of(5.0);
      int count = 1;
      Matrix<N3, N1> devs = new Matrix<>(N3.instance, N1.instance);

      Measurement measurement = new Measurement(pose, distance, timestamp, count, devs);

      assertEquals(10.0, measurement.distance.in(Meters), 0.001);
    }
  }

  @Nested
  @DisplayName("Detection Status Tests")
  class DetectionStatusTests {

    @Test
    @DisplayName("Should report not detecting when no tags visible")
    void shouldReportNotDetectingWhenNoTagsVisible() {
      mockVision.setDetecting(false);
      mockVision.updateInputs(inputs);
      assertFalse(inputs.detecting);
    }

    @Test
    @DisplayName("Should report detecting when tags visible")
    void shouldReportDetectingWhenTagsVisible() {
      mockVision.setDetecting(true);
      mockVision.updateInputs(inputs);
      assertTrue(inputs.detecting);
    }

    @Test
    @DisplayName("Detection status should change dynamically")
    void detectionStatusShouldChangeDynamically() {
      mockVision.setDetecting(true);
      mockVision.updateInputs(inputs);
      assertTrue(inputs.detecting);

      mockVision.setDetecting(false);
      mockVision.updateInputs(inputs);
      assertFalse(inputs.detecting);
    }
  }

  @Nested
  @DisplayName("Distance Array Tests")
  class DistanceArrayTests {

    @Test
    @DisplayName("Should return single distance")
    void shouldReturnSingleDistance() {
      mockVision.setDistances(5.0);
      mockVision.updateInputs(inputs);

      assertEquals(1, inputs.distances.length);
      assertEquals(5.0, inputs.distances[0], 0.001);
    }

    @Test
    @DisplayName("Should return multiple distances")
    void shouldReturnMultipleDistances() {
      mockVision.setDistances(2.0, 4.0, 6.0, 8.0);
      mockVision.updateInputs(inputs);

      assertEquals(4, inputs.distances.length);
      assertEquals(2.0, inputs.distances[0], 0.001);
      assertEquals(8.0, inputs.distances[3], 0.001);
    }

    @Test
    @DisplayName("Should handle empty distances")
    void shouldHandleEmptyDistances() {
      mockVision.setDistances();
      mockVision.updateInputs(inputs);
      assertEquals(0, inputs.distances.length);
    }
  }

  @Nested
  @DisplayName("Pose Estimates Tests")
  class PoseEstimatesTests {

    @Test
    @DisplayName("Should return single pose estimate")
    void shouldReturnSinglePoseEstimate() {
      Pose2d pose = new Pose2d(5.0, 3.0, Rotation2d.fromDegrees(90));
      mockVision.setEstimates(pose);
      mockVision.updateInputs(inputs);

      assertEquals(1, inputs.estimates.length);
      assertEquals(5.0, inputs.estimates[0].getX(), 0.001);
      assertEquals(3.0, inputs.estimates[0].getY(), 0.001);
      assertEquals(90.0, inputs.estimates[0].getRotation().getDegrees(), 0.001);
    }

    @Test
    @DisplayName("Should return multiple pose estimates")
    void shouldReturnMultiplePoseEstimates() {
      Pose2d pose1 = new Pose2d(1.0, 1.0, Rotation2d.kZero);
      Pose2d pose2 = new Pose2d(2.0, 2.0, Rotation2d.k90deg);
      mockVision.setEstimates(pose1, pose2);
      mockVision.updateInputs(inputs);

      assertEquals(2, inputs.estimates.length);
    }

    @Test
    @DisplayName("Should handle empty estimates")
    void shouldHandleEmptyEstimates() {
      mockVision.setEstimates();
      mockVision.updateInputs(inputs);
      assertEquals(0, inputs.estimates.length);
    }
  }

  @Nested
  @DisplayName("Tag Count Tests")
  class TagCountTests {

    @Test
    @DisplayName("Should report zero tags when none visible")
    void shouldReportZeroTagsWhenNoneVisible() {
      mockVision.setTagCount(0);
      mockVision.updateInputs(inputs);
      assertEquals(0, inputs.count);
    }

    @Test
    @DisplayName("Should report single tag")
    void shouldReportSingleTag() {
      mockVision.setTagCount(1);
      mockVision.updateInputs(inputs);
      assertEquals(1, inputs.count);
    }

    @Test
    @DisplayName("Should report multiple tags")
    void shouldReportMultipleTags() {
      mockVision.setTagCount(4);
      mockVision.updateInputs(inputs);
      assertEquals(4, inputs.count);
    }

    @Test
    @DisplayName("Should handle maximum expected tags")
    void shouldHandleMaximumExpectedTags() {
      // FRC fields typically have 8-16 AprilTags
      mockVision.setTagCount(16);
      mockVision.updateInputs(inputs);
      assertEquals(16, inputs.count);
    }
  }

  @Nested
  @DisplayName("Get Measurements Tests")
  class GetMeasurementsTests {

    @Test
    @DisplayName("Should return empty list when no measurements")
    void shouldReturnEmptyListWhenNoMeasurements() {
      List<Measurement> measurements = mockVision.getMeasurements();
      assertTrue(measurements.isEmpty());
    }

    @Test
    @DisplayName("Should return measurements when available")
    void shouldReturnMeasurementsWhenAvailable() {
      Measurement m = new Measurement(
          new Pose2d(1, 1, Rotation2d.kZero),
          Meters.of(2.0),
          Seconds.of(0.5),
          1,
          new Matrix<>(N3.instance, N1.instance));
      mockVision.addMeasurement(m);

      List<Measurement> measurements = mockVision.getMeasurements();
      assertEquals(1, measurements.size());
    }

    @Test
    @DisplayName("Should return multiple measurements")
    void shouldReturnMultipleMeasurements() {
      for (int i = 0; i < 5; i++) {
        Measurement m = new Measurement(
            new Pose2d(i, i, Rotation2d.kZero),
            Meters.of(i + 1.0),
            Seconds.of(i * 0.1),
            1,
            new Matrix<>(N3.instance, N1.instance));
        mockVision.addMeasurement(m);
      }

      List<Measurement> measurements = mockVision.getMeasurements();
      assertEquals(5, measurements.size());
    }

    @Test
    @DisplayName("Should clear measurements correctly")
    void shouldClearMeasurementsCorrectly() {
      Measurement m = new Measurement(
          new Pose2d(1, 1, Rotation2d.kZero),
          Meters.of(2.0),
          Seconds.of(0.5),
          1,
          new Matrix<>(N3.instance, N1.instance));
      mockVision.addMeasurement(m);
      assertEquals(1, mockVision.getMeasurements().size());

      mockVision.clearMeasurements();
      assertEquals(0, mockVision.getMeasurements().size());
    }

    @Test
    @DisplayName("Should return copy of measurements list")
    void shouldReturnCopyOfMeasurementsList() {
      Measurement m = new Measurement(
          new Pose2d(1, 1, Rotation2d.kZero),
          Meters.of(2.0),
          Seconds.of(0.5),
          1,
          new Matrix<>(N3.instance, N1.instance));
      mockVision.addMeasurement(m);

      List<Measurement> measurements1 = mockVision.getMeasurements();
      List<Measurement> measurements2 = mockVision.getMeasurements();

      assertNotSame(measurements1, measurements2);
      assertEquals(measurements1.size(), measurements2.size());
    }
  }

  @Nested
  @DisplayName("Standard Deviation Tests")
  class StandardDeviationTests {

    @Test
    @DisplayName("Matrix should be 3x1 for pose")
    void matrixShouldBe3x1ForPose() {
      Matrix<N3, N1> devs = new Matrix<>(N3.instance, N1.instance);
      assertEquals(3, devs.getNumRows());
      assertEquals(1, devs.getNumCols());
    }

    @Test
    @DisplayName("Matrix should accept standard deviation values")
    void matrixShouldAcceptStandardDeviationValues() {
      Matrix<N3, N1> devs = new Matrix<>(N3.instance, N1.instance);
      devs.set(0, 0, 0.1); // X std dev
      devs.set(1, 0, 0.1); // Y std dev
      devs.set(2, 0, 0.05); // Theta std dev

      assertEquals(0.1, devs.get(0, 0), 0.001);
      assertEquals(0.1, devs.get(1, 0), 0.001);
      assertEquals(0.05, devs.get(2, 0), 0.001);
    }

    @Test
    @DisplayName("Measurement should preserve standard deviations")
    void measurementShouldPreserveStandardDeviations() {
      Matrix<N3, N1> devs = new Matrix<>(N3.instance, N1.instance);
      devs.set(0, 0, 0.5);
      devs.set(1, 0, 0.5);
      devs.set(2, 0, 0.1);

      Measurement m = new Measurement(
          Pose2d.kZero,
          Meters.of(1.0),
          Seconds.of(0.0),
          1,
          devs);

      assertEquals(0.5, m.devs.get(0, 0), 0.001);
      assertEquals(0.5, m.devs.get(1, 0), 0.001);
      assertEquals(0.1, m.devs.get(2, 0), 0.001);
    }
  }

  @Nested
  @DisplayName("Default Interface Implementation Tests")
  class DefaultImplementationTests {

    @Test
    @DisplayName("Default getMeasurements should return empty list")
    void defaultGetMeasurementsShouldReturnEmptyList() {
      VisionIO defaultVision = new VisionIO() {
        @Override
        public void updateInputs(VisionInputs inputs) {}
      };

      List<Measurement> measurements = defaultVision.getMeasurements();
      assertTrue(measurements.isEmpty());
    }

    @Test
    @DisplayName("Default updateInputs should not throw")
    void defaultUpdateInputsShouldNotThrow() {
      VisionIO defaultVision = new VisionIO() {};
      VisionInputs defaultInputs = new VisionInputs();
      assertDoesNotThrow(() -> defaultVision.updateInputs(defaultInputs));
    }
  }

  @Nested
  @DisplayName("Vision Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should handle rapid measurement updates")
    void shouldHandleRapidMeasurementUpdates() {
      for (int i = 0; i < 100; i++) {
        mockVision.setDetecting(i % 2 == 0);
        mockVision.setTagCount(i % 4);
        mockVision.updateInputs(inputs);

        assertEquals(i % 2 == 0, inputs.detecting);
        assertEquals(i % 4, inputs.count);
      }
    }

    @Test
    @DisplayName("Should handle realistic measurement sequence")
    void shouldHandleRealisticMeasurementSequence() {
      // Simulate robot driving and seeing different numbers of tags
      Pose2d[] poses = {
        new Pose2d(1, 1, Rotation2d.kZero),
        new Pose2d(2, 1.5, Rotation2d.fromDegrees(10)),
        new Pose2d(3, 2, Rotation2d.fromDegrees(20)),
        new Pose2d(4, 2.5, Rotation2d.fromDegrees(30))
      };

      for (int i = 0; i < poses.length; i++) {
        mockVision.setDetecting(true);
        mockVision.setEstimates(poses[i]);
        mockVision.setTagCount(i + 1);
        mockVision.setDistances(2.0 + i * 0.5);
        mockVision.updateInputs(inputs);

        assertTrue(inputs.detecting);
        assertEquals(i + 1, inputs.count);
        assertEquals(poses[i].getX(), inputs.estimates[0].getX(), 0.001);
      }
    }
  }
}
