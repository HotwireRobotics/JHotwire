package frc.robot.subsystems.intake;

public class Intake {
    
    // Subsystem abstraction.
    private final IntakeIO io;
    private final IntakeInputs inputs;

    // State system.
    public enum State {
        FORWARD,
        REVERSE,
        STOPPED
    }
    /** Subsystem state. */
    public final StateManager<State> manager = new StateManager<State>(
        getName(), State.READING
    );
}
