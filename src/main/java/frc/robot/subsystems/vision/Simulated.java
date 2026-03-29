package frc.robot.subsystems.vision;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.Seconds;

import java.util.List;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.Timer;

public class Simulated implements VisionIO {
    
    public Simulated() {}

    /**
     * Provide a simulated camera pose estimate.
     * 
     * @return a simulated pose estimate.
     */
    public List<Measurement> getMeasurements() {
      return null;
    }
}
