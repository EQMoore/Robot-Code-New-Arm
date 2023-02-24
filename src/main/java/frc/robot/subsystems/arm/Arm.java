package frc.robot.subsystems.arm;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.RobotConstants;
import org.littletonrobotics.junction.Logger;

public class Arm extends SubsystemBase {

  private final Logger logger = Logger.getInstance();

  private final ArmIO armIO;
  private final ArmIOInputsAutoLogged armIOInputs = new ArmIOInputsAutoLogged();

  private enum ArmMode {
    AUTO,
    EXTEND_CHARACTERIZATION,
    ROTATE_CHARACTERIZATION,
    MANUAL,
    HOLD,
    CALIBRATE,
    KICKBACK,
  }

  private enum CalibrateMode {
    PHASE_ONE,
    PHASE_TWO,
    PHASE_THREE,
    PHASE_FOUR,
  }

  private enum AutoMode {
    RETRACT,
    RETRACT_FULL,
    ROTATE,
    EXTEND
  }

  private ArmMode mode = ArmMode.MANUAL;
  private AutoMode autoMode = AutoMode.RETRACT;
  private CalibrateMode calibrateMode = CalibrateMode.PHASE_ONE;

  private Kickback kickback;

  private double characterizationVoltage = 0.0;
  private double extendSetpoint = 0.0;
  private double rotateSetpoint = 0.0;
  private boolean isCalibrated = false;

  private boolean hasRotate = true;
  private boolean hasExtend = true;
  private static final double EXTEND_TOLERANCE_METERS = 0.008;
  private static final double ROTATE_TOLERANCE_METERS = 0.008;

  private static final double SAFE_ROTATE_AT_FULL_EXTENSION = 0.13;
  private static final double SAFE_EXTENSION_LENGTH = 0.02;
  private static final double SAFE_ROTATE_AT_PARTIAL_EXTENSION = 0.051;
  private static final double SAFE_EXTEND_AT_PARTIAL_EXTENSION = 0.229;

  private static final double SAFE_RETRACT_NON_HOME = 0.05;

  private static final double EXTEND_CALIBRATION_POSITION = 0.01;

  private double manualExtendVolts = 0.0;
  private double manualRotateVolts = 0.0;
  private PIDController extendPidController = new PIDController(50, 0, 0);
  private PIDController rotatePidController = new PIDController(400, 0, 0);
  private static final double BACK_FORCE = -1.3;

  private static final double CALIBRATE_EXTEND_VOLTAGE = -1;
  private static final double CALIBRATE_ROTATE_VOLTAGE = -7;

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

  public boolean isManual() {
    return mode == ArmMode.MANUAL;
  }

  public void stop() {
    mode = ArmMode.MANUAL;
    setExtendVoltage(0.0);
    setRotateVoltage(0.0);
    manualExtendVolts = 0;
    manualRotateVolts = 0;
  }

  public void manualExtend(double volts) {
    mode = ArmMode.MANUAL;
    manualExtendVolts = volts;
  }

  public void manualRotate(double volts) {
    mode = ArmMode.MANUAL;
    manualRotateVolts = volts;
  }

  public void calibrate() {
    calibrateMode = CalibrateMode.PHASE_ONE;
    mode = ArmMode.CALIBRATE;
  }

  public void hold() {
    hold(armIOInputs.extendPosition);
  }

  public void hold(double position) {
    mode = ArmMode.HOLD;
    setExtendVoltage(0.0);
    setRotateVoltage(0.0);
    manualRotateVolts = 0;
    manualExtendVolts = 0;
  }

  public boolean isHolding() {
    return mode == ArmMode.HOLD;
  }

  @Override
  public void periodic() {
    if (DriverStation.isDisabled()) {
      // Disable output while disabled
      setExtendVoltage(0.0);
      setRotateVoltage(0.0);
      return;
    }

    armIO.updateInputs(armIOInputs);
    logger.processInputs("Arm", armIOInputs);
    logger.recordOutput("Arm/Mode", mode.toString());
    logger.recordOutput("Arm/CalibrateMode", calibrateMode.toString());

    switch (mode) {
      case MANUAL:
        setExtendVoltage(manualExtendVolts);
        setRotateVoltage(manualRotateVolts);
        // TODO: Add back contraints for manual movement.
        // if (armIOInputs.extendPosition > RobotConstants.get().armExtendMaxMeters()
        //     && manualExtendVelocity > 0) {
        //   armIO.setExtendVelocity(0);
        // } else if (armIOInputs.extendPosition < RobotConstants.get().armExtendMinMeters()
        //     && manualExtendVelocity < 0) {
        //   setExtendVoltage(calculateExtendPid(RobotConstants.get().armExtendMinMeters()));
        // } else {
        // }
        // if (hasRotate) {
        // if (armIOInputs.rotatePosition > RobotConstants.get().armRotateMaxMeters()
        //     && manualRotateVolts > 0) {
        //   armIO.setRotateVelocity(0);
        // } else if (armIOInputs.rotatePosition < RobotConstants.get().armRotateMinMeters()
        //     && manualRotateVolts < 0) {
        //
        // armIO.setRotateVoltage(calculateRotatePid(RobotConstants.get().armRotateMinMeters()));
        // } else {
        //  armIO.setExtendVelocity(manualExtendVelocity);
        // }
        // }
        logger.recordOutput("Arm/ManualExtendVolts", manualRotateVolts);
        logger.recordOutput("Arm/ManualRotateVolts", manualRotateVolts);
        break;

      case AUTO:
        autoPeriodic();
        break;

      case EXTEND_CHARACTERIZATION:
        setExtendVoltage(characterizationVoltage);
        break;

      case ROTATE_CHARACTERIZATION:
        setRotateVoltage(characterizationVoltage);
        break;

      case HOLD:
        setExtendVoltage(0);
        break;

      case CALIBRATE:
        calibratePeriodic();
        break;

      case KICKBACK:
        kickback.periodic();
        break;
    }
  }

  private void autoPeriodic() {
    if (isFinished()) {
      // Reached target.
      hold();
      return;
    }
    logger.recordOutput("Arm/AutoMode", autoMode.toString());
    switch (autoMode) {
      case RETRACT:
        if (armIOInputs.extendPosition < SAFE_RETRACT_NON_HOME + EXTEND_TOLERANCE_METERS) {
          setExtendVoltage(0);
          autoMode = rotateSetpoint > 0 ? AutoMode.ROTATE : AutoMode.RETRACT_FULL;
        } else {
          setExtendVoltage(calculateExtendPid(SAFE_RETRACT_NON_HOME));
        }
        break;

      case RETRACT_FULL:
        if (armIOInputs.extendPosition
            < RobotConstants.get().armExtendMinMeters() + EXTEND_TOLERANCE_METERS) {
          setExtendVoltage(0);
          autoMode = AutoMode.ROTATE;
        } else {
          setExtendVoltage(calculateExtendPid(RobotConstants.get().armExtendMinMeters()));
        }
        break;

      case ROTATE:
        setRotateVoltage(calculateRotatePid(rotateSetpoint));
        if (isRotateFinished()) {
          autoMode = AutoMode.EXTEND;
          setRotateVoltage(0);
        }
        break;

      case EXTEND:
        double extendFbOutput = calculateExtendPid(extendSetpoint);
        setExtendVoltage(extendFbOutput);
        break;
    }

    logger.recordOutput("Arm/ExtendSetpoint", extendSetpoint);
    logger.recordOutput("Arm/RotateSetpoint", rotateSetpoint);
  }

  private void calibratePeriodic() {
    switch (calibrateMode) {
      case PHASE_ONE:
        // Drive Extend Motor until hit limit switch
        if (armIOInputs.extendReverseLimitSwitch && armIOInputs.rotateLowLimitSwitch) {
          isCalibrated = true;
          hold();
          break;
        }
        if (hasExtend) {
          if (armIOInputs.extendReverseLimitSwitch) {
            armIO.resetExtendEncoderPosition();
            calibrateMode = CalibrateMode.PHASE_TWO;
          } else {
            setExtendVoltage(CALIBRATE_EXTEND_VOLTAGE);
          }
        } else {
          calibrateMode = CalibrateMode.PHASE_THREE;
        }

        break;
      case PHASE_TWO:
        // Drive Extend Motor a little bit outwards
        if (hasExtend) {
          setExtendVoltage(calculateExtendPidUnsafe(EXTEND_CALIBRATION_POSITION));
          if (isExtendPositionNear(EXTEND_CALIBRATION_POSITION)) {
            calibrateMode = CalibrateMode.PHASE_THREE;
            setExtendVoltage(0);
          }
        }
        break;
      case PHASE_THREE:
        // Drive rotate motor until hit lower limit switch
        if (hasRotate) {
          if (armIOInputs.rotateLowLimitSwitch) {
            armIO.resetRotateEncoderPosition();
            armIO.setRotateVoltage(0);
            calibrateMode = CalibrateMode.PHASE_FOUR;
          } else {
            setRotateVoltage(CALIBRATE_ROTATE_VOLTAGE);
          }
        } else {
          calibrateMode = CalibrateMode.PHASE_FOUR;
        }

        break;
      case PHASE_FOUR:
        // Drive Extend Motor until hit limit switch
        if (hasExtend) {
          if (armIOInputs.extendReverseLimitSwitch) {
            armIO.resetExtendEncoderPosition();
            isCalibrated = true;
            hold();
          } else {
            setExtendVoltage(CALIBRATE_EXTEND_VOLTAGE);
          }
        } else {
          isCalibrated = true;
          hold();
        }
        break;
    }
  }

  public void setTargetPositions(double extendSetpoint, double rotateSetpoint) {
    mode = ArmMode.AUTO;
    autoMode = AutoMode.RETRACT;
    this.extendSetpoint =
        MathUtil.clamp(
            extendSetpoint,
            RobotConstants.get().armExtendMinMeters(),
            RobotConstants.get().armExtendMaxMeters());
    this.rotateSetpoint =
        MathUtil.clamp(
            rotateSetpoint,
            RobotConstants.get().armRotateMinMeters(),
            RobotConstants.get().armRotateMaxMeters());
  }

  public void setTargetPositionExtend(double extendSetpoint) {
    setTargetPositions(extendSetpoint, armIOInputs.rotatePosition);
  }

  public void setTargetPositionRotate(double rotateSetpoint) {
    setTargetPositions(armIOInputs.extendPosition, rotateSetpoint);
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

  public boolean isStopped() {
    return mode == ArmMode.MANUAL && manualExtendVolts == 0 && manualRotateVolts == 0;
  }

  public boolean isFinished() {
    return ((!hasExtend || isExtendPositionNear(extendSetpoint)) && isRotateFinished());
  }

  public boolean isRotateFinished() {
    return !hasRotate
        || Math.abs(armIOInputs.rotatePosition - rotateSetpoint) <= ROTATE_TOLERANCE_METERS;
  }

  private void setRotateVoltage(double volts) {
    armIO.setRotateVoltage(volts);
  }

  private void setExtendVoltage(double volts) {
    if (!maybeKickback(volts)) {
      if (volts == 0) {
        armIO.setExtendVoltage(0);
      } else if (volts < 0) {
        armIO.setExtendVoltage(volts + BACK_FORCE);
      } else {
        armIO.setExtendVoltage(volts);
      }
    }
  }

  private double calculateExtendPid(double targetPosition) {
    if (!isExtendSafe(targetPosition)) {
      return 0;
    }
    return calculateExtendPidUnsafe(targetPosition);
  }

  private double calculateExtendPidUnsafe(double targetPosition) {
    double pidValue = extendPidController.calculate(armIOInputs.extendPosition, targetPosition);
    logger.recordOutput("Arm/ExtendFbOutput", pidValue);
    return pidValue;
  }

  private double calculateRotatePid(double targetPosition) {
    if (!isRotateSafe(targetPosition)) {
      return 0;
    }
    double pidValue = rotatePidController.calculate(armIOInputs.rotatePosition, targetPosition);
    logger.recordOutput("Arm/RotateFbOutput", pidValue);
    return pidValue;
  }

  private boolean isExtendPositionNear(double targetPosition) {
    return Math.abs(armIOInputs.extendPosition - targetPosition) <= EXTEND_TOLERANCE_METERS;
  }

  private boolean isRotateSafe(double rotatePosition) {
    boolean isSafe =
        isCalibrated
            && (rotatePosition > armIOInputs.rotatePosition
                || rotatePosition > SAFE_ROTATE_AT_FULL_EXTENSION
                || armIOInputs.extendPosition < SAFE_EXTENSION_LENGTH
                || (rotatePosition > SAFE_ROTATE_AT_PARTIAL_EXTENSION - ROTATE_TOLERANCE_METERS
                    && armIOInputs.extendPosition
                        < SAFE_EXTEND_AT_PARTIAL_EXTENSION - EXTEND_TOLERANCE_METERS));
    logger.recordOutput("Arm/IsRotateSafe", isSafe);
    return isSafe;
  }

  private boolean isExtendSafe(double extendPosition) {
    boolean isSafe =
        isCalibrated
            && (extendPosition < armIOInputs.extendPosition
                || armIOInputs.rotatePosition > SAFE_ROTATE_AT_FULL_EXTENSION
                || (armIOInputs.rotatePosition > SAFE_ROTATE_AT_PARTIAL_EXTENSION
                    && extendPosition < SAFE_EXTEND_AT_PARTIAL_EXTENSION));
    logger.recordOutput("Arm/IsExtendSafe", isSafe);
    return isSafe;
  }

  private boolean maybeKickback(double targetVolts) {
    if (mode != ArmMode.KICKBACK && targetVolts > 0 && armIOInputs.ratchetLocked) {
      kickback = new Kickback(targetVolts);
      mode = ArmMode.KICKBACK;
      return true;
    }
    return false;
  }

  private class Kickback {
    private final ArmMode returnToMode;
    private final double startPosition;
    private static final double TRAVEL_DISTANCE_METERS = 0.03;
    private final double targetVolts;

    Kickback(double targetVolts) {
      returnToMode = mode;
      startPosition = armIOInputs.extendPosition;
      armIO.setExtendVoltage(-2);
      this.targetVolts = targetVolts;
    }

    void periodic() {
      if (armIOInputs.extendPosition <= startPosition + TRAVEL_DISTANCE_METERS) {
        setExtendVoltage(targetVolts);
        mode = returnToMode;
        kickback = null;
      }
    }
  }
}
