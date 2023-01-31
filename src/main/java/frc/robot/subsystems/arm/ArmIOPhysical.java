package frc.robot.subsystems.arm;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxRelativeEncoder;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DigitalInput;
import frc.robot.RobotConstants;

public class ArmIOPhysical implements ArmIO {
  private final CANSparkMax extendMotor;
  private final RelativeEncoder extendEncoder;

  private CANSparkMax rotateMotor;
  private RelativeEncoder rotateEncoder;

  private final LidarLitePWM lidar;

  // private int resetCount = 0;

  public ArmIOPhysical(
      int extendMotorId, int rotateMotorId, int rotateAbsEncoderId, int lidarId, int index) {
    lidar = new LidarLitePWM(new DigitalInput(lidarId));
    extendMotor = new CANSparkMax(extendMotorId, MotorType.kBrushless);
    // rotateMotor = new CANSparkMax(rotateMotorId, MotorType.kBrushless);
    extendEncoder = extendMotor.getEncoder(SparkMaxRelativeEncoder.Type.kHallSensor, 42);
    extendEncoder.setPosition(lidar.getDistance());
    extendEncoder.setPositionConversionFactor(10);

    rotateMotor = null;

    // rotateEncoder = rotateMotor.getEncoder();

    // Convert rotations to radians
    double extendRotationsToRads =
        Units.rotationsToRadians(1)
            * RobotConstants.get().armExtendGearRatio().getRotationsPerInput();
    // extendEncoder.setPositionConversionFactor(extendRotationsToRads);

    // double rotateRotationsToRads =
    //     Units.rotationsToRadians(1)
    //         * RobotConstants.get().armRotateGearRatio().getRotationsPerInput();
    // rotateEncoder.setPositionConversionFactor(rotateRotationsToRads);

    // // Convert rotations per minute to radians per second
    // extendEncoder.setVelocityConversionFactor(extendRotationsToRads / 60);
    // rotateEncoder.setVelocityConversionFactor(rotateRotationsToRads / 60);

    // // Convert rotations per minute to radians per second
    // extendEncoder.setPositionConversionFactor(1);

    // // Invert motors
    // extendMotor.setInverted(false);
    // rotateMotor.setInverted(false); // TODO: check if inverted

    // extendMotor.enableVoltageCompensation(12);
    // rotateMotor.enableVoltageCompensation(12);
  }

  @Override
  public void updateInputs(ArmIOInputs inputs) {

    inputs.extendPositionAbsolute = lidar.getDistance() / 100.0;

    // TODO: Use the Lidar to get the absolute position of the arm
    // Reset the turn encoder sometimes when not moving
    // if (turnEncoder.getVelocity() < Units.degreesToRadians(0.5)) {
    //   if (++resetCount >= 500) {
    //     resetCount = 0;
    //     turnEncoder.setPosition(
    //         Rotation2d.fromDegrees(turnEncoderAbsolute.getAbsolutePosition())
    //             .minus(RobotConstants.get().absoluteAngleOffset()[index])
    //             .getRadians());
    //   }
    // } else {
    //   resetCount = 0;
    // }
    // inputs.turnPositionAbsolute =
    //     Rotation2d.fromDegrees(turnEncoderAbsolute.getAbsolutePosition())
    //         .minus(RobotConstants.get().absoluteAngleOffset()[index])
    //         .getRadians();

    inputs.extendVelocity = extendEncoder.getVelocity();
    inputs.extendPosition = extendEncoder.getPosition();
    // inputs.rotateVelocity = rotateEncoder.getVelocity();
    // inputs.rotatePosition = rotateEncoder.getPosition();
  }

  @Override
  public void setExtendVoltage(double volts) {
    extendMotor.setVoltage(volts);
  }

  @Override
  public void setRotateVoltage(double volts) {
    // rotateMotor.setVoltage(volts);
  }

  @Override
  public void setExtendBrakeMode(boolean brake) {
    extendMotor.setIdleMode(brake ? IdleMode.kBrake : IdleMode.kCoast);
  }

  @Override
  public void setRotateBrakeMode(boolean brake) {
    // rotateMotor.setIdleMode(brake ? IdleMode.kBrake : IdleMode.kCoast);
  }

  public CANSparkMax getExtendMotor() {

    return extendMotor;
  }

  public CANSparkMax getRotateMotor() {

    return rotateMotor;
  }
}
