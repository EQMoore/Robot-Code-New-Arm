package frc.robot.subsystems.arm;

import com.revrobotics.CANSparkMax;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class RotateArmSubsystem extends SubsystemBase {
  private CANSparkMax rotateMotor;

  public RotateArmSubsystem(CANSparkMax rotateMotor) {
    this.rotateMotor = rotateMotor;
  }
   public void SetArmExtendVoltage(double volts) {
    rotateMotor.setVoltage(volts);
  }
  
}
