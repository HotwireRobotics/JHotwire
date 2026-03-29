// Hotwire-Robotics™ : 
package frc.robot.subsystems.vision;

import java.util.function.Consumer;
import java.util.function.Supplier;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.Mode;
import frc.robot.constants.LimelightHelpers.PoseEstimate;
import frc.robot.subsystems.vision.VisionIO.Measurement;

/**
 * <strong>Vision Subsystem</strong>
 * <p>Camera reads apriltag markers and supplies the drivetrain with 
 * pose estimates. The vision subsystem takes suppliers for the robot
 * location and rotation. Function also requires a pose estimate consumer.
 * 
 * @param location
 * @param rotation
 * @param estimate
 */
public class Vision extends SubsystemBase {

    // Declare orientation and location suppliers.
    private final Supplier<Pose2d> location;
    private final Supplier<Rotation2d> rotation;

    // Declare pose estimate consumer.
    private final Consumer<Pose2d> estimate;

    // Subsystem abstraction.
    private final VisionIO io;
    
    public Vision(
        Supplier<Pose2d>     location,
        Supplier<Rotation2d> rotation,
        Consumer<Pose2d>     estimate
    ) {
        // Initialize abstraction
        io = Constants.mode.equals(Mode.SIM) 
            ? new Simulated() 
            : new Limelight();

        // Initialize orientation suppliers.
        this.location = location;
        this.rotation = rotation;

        // Initialize pose estimate consumer.
        this.estimate = estimate;
    }

    /**
     * Provides the drivetrain with a pose estimate.
     * 
     * @param pose
     */
    private void supply(Pose2d pose) {
        estimate.accept(pose);
    }

    /**
     * Get drivetrain pose.
     * 
     * @return the drivetrain pose measurement.
     */
    private Pose2d getPose() {
        return location.get();
    }

    /**
     * Get drivetrain rotation.
     * 
     * @return the gyroscope yaw measurement.
     */
    private Rotation2d getRotation() {
        return rotation.get();
    }

    
    /**
     * Determine whether a given pose estimate is valid based on the number of detected tags and the
     * average tag distance.
     *
     * @param estimation
     * @return
     */
    public boolean isValidEstimate(Measurement estimation) {
        if (estimation == null) return false;
        return ((estimation.count > 0)
            && (estimation.distance <= Configuration.maxDistance.in(Meters)));
    }

    @Override
    public void periodic() {
        // Get a pose estimate from the camera.
        Measurement measurement = io.getMeasurement();

        // Supply a pose estimate to the drivetrain.
        if (isValidEstimate(measurement)) supply(measurement.pose);
    }
}