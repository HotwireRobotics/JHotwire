package frc.robot.subsystems.vision;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.Seconds;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.wpilibj.Timer;

public class Limelight implements VisionIO {

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
    }
    
    /** Define localization mode. */
    private static enum Localization {
        MEGATAG1, MEGATAG2
    }

    /** Define pipeline for vision. */
    private static class Pipeline {
        /** Localization pipeline. */
        Localization localization = Localization.MEGATAG2;
        // TODO: Add other vision settings (exposed in the web interface).

        public Pipeline withLocalization(Localization localization) {
            this.localization = localization;
            return this;
        }
    }

    private class Camera {

        public Camera(
            Pose3d pose, Pipeline pipeline
        ) {

        }
    }
    
    public Limelight() {

    }

    /**
     * Provide a real camera pose estimate.
     * 
     * @return a real pose estimate.
     */
    public Measurement getMeasurement() {


        // Initialize a measurement object.
        Measurement measurement = new Measurement()
            .withPose(...)
            .withDistance(...)
            .withTimestamp(Seconds.of(Timer.getTimestamp()));
        return measurement;
    }
}
