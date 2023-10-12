package frc.robot.subsystems.arm;


import com.revrobotics.CANSparkMax;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class ExtendArmSubsystem extends SubsystemBase {
  private CANSparkMax extendMotor;

  public ExtendArmSubsystem(CANSparkMax extendMotor) {
    super();
    this.extendMotor = extendMotor;
    }

  public void SetArmExtendVoltage(double volts){
    extendMotor.setVoltage(volts);
  }

}
