package frc.robot.subsystems.arm;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.RelativeEncoder;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DigitalOutput;
import frc.robot.RobotConstants;

public class ArmIOPhysical implements ArmIO {
  private final CANSparkMax extendMotor;
  private final RelativeEncoder extendEncoder;
  private final DigitalInput extendLimitSwitch;
  private final DigitalInput rotateHighLimitSwitch;
  private final DigitalInput rotateLowLimitSwitch;
  private final DigitalOutput ratchetSolenoid;

  private CANSparkMax rotateMotor;
  private RelativeEncoder rotateEncoder;

  public ArmIOPhysical(
      int extendMotorId,
      int rotateMotorId,
      int extendLimitSwitchId,
      int ratchetSolenoidId,
      int rotateHighLimitSwitchId,
      int rotateLowLimitSwitchId) {
    extendLimitSwitch = new DigitalInput(extendLimitSwitchId);
    rotateHighLimitSwitch = new DigitalInput(rotateHighLimitSwitchId);
    rotateLowLimitSwitch = new DigitalInput(rotateLowLimitSwitchId);

    ratchetSolenoid = new DigitalOutput(ratchetSolenoidId);

    extendMotor = new CANSparkMax(extendMotorId, MotorType.kBrushless);
    rotateMotor = new CANSparkMax(rotateMotorId, MotorType.kBrushless);

    extendEncoder = extendMotor.getEncoder();
    extendEncoder.setPositionConversionFactor(RobotConstants.get().armExtendConversionFactor());

    rotateEncoder = rotateMotor.getEncoder();
    rotateEncoder.setPositionConversionFactor(RobotConstants.get().armRotateConversionFactor());

    // Invert motors
    extendMotor.setInverted(false);
    rotateMotor.setInverted(false); // TODO: check if inverted

    extendMotor.enableVoltageCompensation(12);
    rotateMotor.enableVoltageCompensation(12);

    extendMotor.setIdleMode(IdleMode.kBrake);
    rotateMotor.setIdleMode(IdleMode.kBrake);
  }

  @Override
  public void updateInputs(ArmIOInputs inputs) {
    inputs.extendVelocity = extendEncoder.getVelocity();
    inputs.extendPosition = extendEncoder.getPosition();
    inputs.extendAppliedVolts = extendMotor.getBusVoltage();
    inputs.extendCurrent = extendMotor.getOutputCurrent();
    inputs.extendTemp = extendMotor.getMotorTemperature();
    inputs.extendLimitSwitch = extendLimitSwitch.get();
    inputs.rotateHighLimitSwitch = rotateHighLimitSwitch.get();
    inputs.rotateLowLimitSwitch = rotateLowLimitSwitch.get();
    inputs.rotatePosition = rotateEncoder.getPosition();
    inputs.rotateVelocity = rotateEncoder.getVelocity();
  }

  @Override
  public void setExtendVelocity(double velocity) {
    extendMotor.set(velocity);
  }

  @Override
  public void setRotateVelocity(double velocity) {
    rotateMotor.set(velocity);
  }

  @Override
  public void setExtendVoltage(double volts) {
    extendMotor.setVoltage(volts);
  }

  @Override
  public void setRotateVoltage(double volts) {
    rotateMotor.setVoltage(volts);
  }

  @Override
  public void resetEncoderPosition() {
    extendEncoder.setPosition(0);
  }

  @Override
  public void setRatchetLocked(boolean locked) {
    ratchetSolenoid.set(locked);
  }
}
