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
  public void execute() {
    arm.manualRotate(0.1);
  }

  @Override
  public void end(boolean interrupted) {}

  @Override
  public boolean isFinished() {
    return false;
  }
}
