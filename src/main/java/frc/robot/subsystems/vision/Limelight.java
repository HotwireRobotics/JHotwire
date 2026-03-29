package frc.robot.subsystems.vision;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.Seconds;

import java.util.List;
import java.util.stream.Collectors;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.constants.LimelightHelpers;
import frc.robot.constants.LimelightHelpers.PoseEstimate;

public class Limelight implements VisionIO {

    /** List of cameras. */
    private final List<Camera> cameras;

    // Poses for each camera.
    public static class Poses {
      private static final Pose3d gamma = 
        new Pose3d(
          Meters.of(-0.250824), 
          Meters.of(0.2794), 
          Meters.of(0.2413),
          new Rotation3d(
            Degrees.of(0), 
            Degrees.of(30), 
            Degrees.of(-90)
          ));
      private static final Pose3d alpha = 
        new Pose3d(
          Meters.of(-0.1651), 
          Meters.of(-0.29209), 
          Meters.of(0.5206),
          new Rotation3d(
            Degrees.of(0), 
            Degrees.of(27), 
            Degrees.of(0)
          ));
    }

    /** Define assist mode for the internal IMU. */
    public static enum IMUMode {
      OFF(0),
      SEED(1),
      INTERNAL(2),
      MT1ASSIST(3),
      EXTERNAL(4);

      public final int mode;

      IMUMode(int mode) {
        this.mode = mode;
      }

      public int get() {
        return this.mode;
      }
    }
    
    /** Define localization mode. */
    private static enum Localization {
      MEGATAG1, MEGATAG2
    }

    /** Define pipeline for vision. */
    private static class Pipeline {
      /** Localization pipeline. */
      Localization localization = Localization.MEGATAG2;
      /** Pipeline index. */
      int index = 0;
      /** IMU mode. */
      IMUMode mode = IMUMode.OFF;
      // TODO: Add other vision settings (exposed in the web interface).

      /**
       * Supply pipeline with specified localization mode.
       * 
       * @param localization
       */
      public Pipeline withLocalization(Localization localization) {
        this.localization = localization;
        return this;
      }

      /**
       * Supply pipeline with specified IMU mode.
       * 
       * @param mode
       */
      public Pipeline withIMUMode(IMUMode mode) {
        this.mode = mode;
        return this;
      }
      
      /**
       * Supply pipeline with specified index.
       * 
       * @param index
       */
      public Pipeline withIndex(int index) {
        this.index = index;
        return this;
      }
    }

    private class Camera {

        /** String identifier. */
        private final String name;
        /** Pose object. */
        private final Pose3d pose;
        /** processing pipeline. */
        private Pipeline pipeline;

        public Camera(
            String name, Pose3d pose, Pipeline pipeline
        ) {
          // Initialize name.
          this.name = name;
          this.pose = pose;

          // Set pipeline.
          this.pipeline = pipeline;

          // Initialize robot-relative pose.
          LimelightHelpers.setCameraPose_RobotSpace(name, 
            pose.getMeasureX().in(Meters),
            pose.getMeasureY().in(Meters),
            pose.getMeasureZ().in(Meters),
            pose.getRotation().getMeasureX().in(Degrees),
            pose.getRotation().getMeasureY().in(Degrees),
            pose.getRotation().getMeasureZ().in(Degrees)
          );

          // Set configuration.
          setIMUMode(pipeline.mode);
        }

        /**
         * Select IMU mode for configuration.
         */
        private void setIMUMode(IMUMode mode) {
          LimelightHelpers.SetIMUMode(name, mode.get());
        }

        /**
         * Provide a real camera pose estimate.
         * 
         * @return a real pose estimate.
         */
        public Measurement getMeasurement() {
          // Select pose estimation method by localization mode.
          PoseEstimate estimation = pipeline.localization.equals(Localization.MEGATAG2)
            ? LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2(name)
            : LimelightHelpers.getBotPoseEstimate_wpiBlue(name);

          // Validate and share measurement.
          if (estimation == null) {
            return null;
          } else {
            // Initialize a measurement object.
            Measurement measurement = new Measurement(estimation)
              .withStandardDeviation(Configuration.standardDeviation);
            return measurement;
          }
        }

        /** 
         * Get the camera pose. 
         * 
         * @return the camera pose.
         */
        public Pose3d getPose() {
          return this.pose;
        }
    }
    
    public Limelight() {
      // Generic pipeline.
      final Pipeline pipeline = new Pipeline()
        .withIndex(0)
        .withLocalization(Localization.MEGATAG2)
        .withIMUMode(IMUMode.INTERNAL);
      // Initialize cameras.
      cameras = List.of(
        // Primary camera mounted perpendicular.
        new Camera("limelight-gamma", Poses.gamma, pipeline),
        // Secondary camera mounted forward.
        new Camera("limelight-alpha", Poses.alpha, pipeline)
      );
    }

    /**
     * Provide a real camera pose estimate.
     * 
     * @return a real pose estimate.
     */
    public List<Measurement> getMeasurements() {
      // Stream cameras to collect all pose estimates.
      return cameras.stream()
        .map(camera -> camera.getMeasurement())
        .collect(Collectors.toList());
    }
}
