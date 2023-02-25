package frc.robot.commands.arm;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.intakerelease.IntakeRelease;
import frc.robot.subsystems.intakerelease.IntakeRelease.Wants;
import frc.robot.subsystems.led.Led2023;
import frc.robot.subsystems.led.Led2023.COLORS_467;

public class ArmManualRetractCMD extends CommandBase {
  private Arm arm;
  private Led2023 ledStrip;
  private IntakeRelease intakerelease;

  public ArmManualRetractCMD(Arm arm, IntakeRelease intakerelease, Led2023 ledStrip) {
    this.arm = arm;
    this.ledStrip = ledStrip;
    this.intakerelease = intakerelease;
    addRequirements(arm, intakerelease, ledStrip);
  }

  @Override
  public void initialize() {
    arm.manualExtend(-1);
  }

  @Override
  public void execute() {
    if (intakerelease.getWants() == Wants.CUBE || intakerelease.haveCube()) {
      ledStrip.setAlternateColorsDown(
          COLORS_467.Purple, COLORS_467.White, COLORS_467.Black.getColor());
    } else {
      ledStrip.setAlternateColorsDown(
          COLORS_467.Gold, COLORS_467.White, COLORS_467.Black.getColor());
    }
  }

  @Override
  public void end(boolean interrupted) {
    arm.hold();
  }

  @Override
  public boolean isFinished() {
    return arm.isHolding();
  }
}
