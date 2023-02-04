package frc.robot.subsystems.arm;

import edu.wpi.first.math.controller.BangBangController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.RobotConstants;
import org.littletonrobotics.junction.Logger;

public class Arm extends SubsystemBase {

  private final Logger logger = Logger.getInstance();

  private static final SimpleMotorFeedforward extendFF =
      RobotConstants.get().moduleDriveFF().getFeedforward();
  private static final SimpleMotorFeedforward rotateFF =
      RobotConstants.get().moduleTurnFF().getFeedforward();

  private final ArmIO armIO;
  private final ArmIOInputsAutoLogged armIOInputs = new ArmIOInputsAutoLogged();

  private enum ArmMode {
    NORMAL,
    EXTEND_CHARACTERIZATION,
    ROTATE_CHARACTERIZATION,
    MANUAL,
    DISABLED,
    HOLD
  }

  private ArmMode mode = ArmMode.MANUAL;
  private double currentPosition;
  private double angle = 0;
  private double characterizationVoltage = 0.0;
  private double extendSetpoint = 0.6;
  private double rotateSetpoint = 0.0;

  public static final double EXTEND_TOLERANCE_METERS = 0.05;
  public static final double ROTATE_TOLERANCE_DEGREES = 2.0;

  private boolean enabled = false;

  private double manualExtend = 0.0;
  private double manualRotate = 0.0;

  // private PIDController extendPID = new PIDController(0.1, 0.0, 0.0);
  BangBangController controller = new BangBangController();
  /**
   * Configures the arm subsystem
   *
   * @param armIO Arm IO
   */
  public Arm(ArmIO armIO) {
    super();
    this.armIO = armIO;

    armIO.updateInputs(armIOInputs);
  }

  public void enable() {
    enabled = true;
  }

  public void disable() {
    stop();
    enabled = false;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isManual() {
    return mode == ArmMode.MANUAL;
  }

  public void stop() {
    mode = ArmMode.MANUAL;
    armIO.setExtendVoltage(0.0);
    armIO.setRotateVoltage(0.0);
    manualExtend = 0;
    manualRotate = 0;
  }

  public void manualExtend(double direction) {
    mode = ArmMode.MANUAL;
    manualExtend = direction;
  }

  public void manualRotate(double direction) {
    mode = ArmMode.MANUAL;
    manualRotate = direction;
  }

  public void hold() {
    currentPosition = armIOInputs.extendPosition;
    mode = ArmMode.HOLD;
  }

  /* (non-Javadoc)
   * @see edu.wpi.first.wpilibj2.command.Subsystem#periodic()
   */
  @Override
  public void periodic() {
    // Update inputs for IOs
    armIO.updateInputs(armIOInputs);
    logger.processInputs("Arm", armIOInputs);
    controller.setTolerance(EXTEND_TOLERANCE_METERS);
    // double pidOutput = controller.calculate(armIOInputs.extendPositionAbsolute, extendSetpoint);

    double pidOutput = 0.0;
    if (armIOInputs.extendPositionAbsolute > extendSetpoint) {
      pidOutput = -0.1;
    } else {
      pidOutput = 0.1;
    }
    if (Math.abs(armIOInputs.extendPositionAbsolute - extendSetpoint) <= 0.2) {
      hold();
    }

    double holdPIDOutput = 0.0;
    if (armIOInputs.extendPosition > extendSetpoint) {
      holdPIDOutput = -0.1;
    } else {
      holdPIDOutput = 0.1;
    }
    logger.recordOutput("arm/pidoutput", pidOutput);

    if (DriverStation.isDisabled()) {
      // Disable output while disabled
      armIO.setExtendVoltage(0.0);
      armIO.setRotateVoltage(0.0);

    } else {
      switch (mode) {
        case MANUAL:
          armIO.setExtendVelocity(manualExtend);
          armIO.setRotateVelocity(manualRotate);
          break;
        case NORMAL:
          if (armIOInputs.extendPositionAbsolute > extendSetpoint) {
            pidOutput = -0.1;
          } else {
            pidOutput = 0.1;
          }
          if (Math.abs(armIOInputs.extendPositionAbsolute - extendSetpoint) <= 0.2) {
            hold();
          }
          armIO.setExtendVelocity(pidOutput);

          logger.recordOutput("ArmExtendSetpoint", extendSetpoint);
          logger.recordOutput("ArmRotateSetpoint", rotateSetpoint);
          break;

        case EXTEND_CHARACTERIZATION:
          armIO.setExtendVoltage(characterizationVoltage);
          break;

        case ROTATE_CHARACTERIZATION:
          armIO.setRotateVoltage(characterizationVoltage);
          break;
        case DISABLED:
          break;
        case HOLD:
          armIO.setExtendVelocity(holdPIDOutput);
          break;
      }
    }

    // TODO: Proper conversions
    // armIO.setExtendPosition(distanceTargetInches);
    // armIO.setRotatePosition(rotateTargetDegrees);
  }

  public void setExtendSetpoint(double setpoint) {
    mode = ArmMode.NORMAL;
    extendSetpoint = setpoint;
  }

  public void setRotateSetpoint(double setpoint) {
    mode = ArmMode.NORMAL;
    rotateSetpoint = setpoint;
  }

  public void characterizeExtend() {
    mode = ArmMode.EXTEND_CHARACTERIZATION;
  }

  public void characterizeRotate() {
    mode = ArmMode.ROTATE_CHARACTERIZATION;
  }

  public void runCharacterizationVolts(double volts) {
    characterizationVoltage = volts;
  }

  public double getCharacterizationVelocity() {
    if (mode == ArmMode.EXTEND_CHARACTERIZATION) {
      return armIOInputs.extendVelocity;
    } else if (mode == ArmMode.ROTATE_CHARACTERIZATION) {
      return armIOInputs.rotateVelocity;
    } else {
      return 0.0;
    }
  }

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
  }

  @Override
  public void initSendable(SendableBuilder builder) {
    super.initSendable(builder);
  }

  public boolean isStopped() {
    if (armIOInputs.extendVelocity <= 0.1 && armIOInputs.rotateVelocity <= 0.1) return true;
    return false;
  }

  public boolean finished() {
    double currentDistance = armIOInputs.extendPosition; // NEEDS CONVERSION
    // TODO: CHANGE to Lidar

    // double currentAngle = armRotateMotor.getEncoder().getPosition(); // NEEDS CONVERSION
    double currentAngle = 0;
    if (currentDistance >= (extendSetpoint - EXTEND_TOLERANCE_METERS)
        && (currentDistance <= (extendSetpoint + EXTEND_TOLERANCE_METERS)
            && (currentAngle >= (rotateSetpoint - EXTEND_TOLERANCE_METERS)
                && (currentAngle <= (rotateSetpoint + EXTEND_TOLERANCE_METERS))))) {
      return true;
    }

    return false;
  }

  public void resetEncoderPosition() {
    armIO.resetEncoderPosition();
  }
}
