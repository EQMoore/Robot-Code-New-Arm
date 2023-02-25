package frc.robot.commands.arm;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.arm.Arm;

public class ArmManualUpCMD extends CommandBase {
  private final Arm arm;

  public ArmManualUpCMD(Arm arm) {
    this.arm = arm;

    addRequirements(arm);
  }

  @Override
  public void initialize() {
    arm.manualRotate(2);
  }

  @Override
  public void end(boolean interrupted) {
    arm.hold();
  }
}
