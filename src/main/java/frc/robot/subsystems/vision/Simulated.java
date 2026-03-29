package frc.robot.subsystems.vision;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.Seconds;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.Timer;

public class Simulated implements VisionIO {
    
    public Simulated() {

    }

    /**
     * Provide a simulated camera pose estimate.
     * 
     * @return a simulated pose estimate.
     */
    public Measurement getMeasurement() {
        // Initialize a measurement object.
        Measurement measurement = new Measurement()
            .withPose(Pose2d.kZero)
            .withDistance(Meters.of(0))
            .withTimestamp(Seconds.of(Timer.getTimestamp()));
        return measurement;
    }
}
