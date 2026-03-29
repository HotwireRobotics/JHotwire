package frc.robot.subsystems.vision;

import org.littletonrobotics.junction.AutoLog;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.Time;

public interface VisionIO {
    /**
     * Structure for camera pose estimates.
     */
    public static class Measurement {
        /** Pose estimate. */
        public Pose2d pose;
        /** Average estimated distance from target. */
        public Distance distance;
        /** Marks the measurement time. */
        public Time timestamp;
        /** Tag count. */
        public int count;

        /**
         * Supply measurement pose.
         * 
         * @param pose
         */
        public Measurement withPose(Pose2d pose) {
            this.pose = pose;
            return this;
        }
        
        /**
         * Supply measurement average distance.
         * 
         * @param pose
         */
        public Measurement withDistance(Distance distance) {
            this.distance = distance;
            return this;
        }
         
        /**
         * Supply measurement average distance.
         * 
         * @param pose
         */
        public Measurement withTimestamp(Time time) {
            this.timestamp = time;
            return this;
        }

        /**
         * Supply measurement tag count.
         * 
         * @param count
         */
        public Measurement withTagCount(int count) {
            this.count = count;
            return this;
        }
    }

    /**
     * Provide camera-estimated pose.
     * 
     * @return a pose estimate if the camera detects a valid target.
     */
    public Measurement getMeasurement();

    // Define inputs for vision subsystem.
    @AutoLog
    public class VisionInputs {
        /** Is the camera detecting a target? */
        boolean detecting;

        /** Average distance between the estimated pose and a target. */
        Distance distance;

        /** Pose estimation by camera. */
        Pose2d estimate;

        // Limelight specific logging.
        /** Status of the inertial measurement unit (IMU) */
        IMUMode imu;
    }
}