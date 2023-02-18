package frc.robot.commands.intakerelease;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.intakerelease.IntakeRelease;
import frc.robot.subsystems.intakerelease.IntakeRelease.Wants;
import frc.robot.subsystems.led.Led2023;
import frc.robot.subsystems.led.Led2023.COLORS_467;

public class IntakeCMD extends CommandBase {
  private IntakeRelease intakerelease;
  private Led2023 ledStrip;

  public IntakeCMD(IntakeRelease intakerelease, Led2023 ledStrip) {
    this.intakerelease = intakerelease;
    this.ledStrip = ledStrip;
    addRequirements(ledStrip);

    addRequirements(intakerelease);
  }

  @Override
  public void initialize() {}

  @Override
  public void execute() {
    intakerelease.intake();
    if (intakerelease.getWants() == Wants.CUBE) {
      ledStrip.setColorMovingUp(
          COLORS_467.White.getColor(), COLORS_467.Blue.getColor()); // Blue, black
    } else if (intakerelease.getWants() == Wants.CONE) {
      ledStrip.setColorMovingUp(
          COLORS_467.White.getColor(), COLORS_467.Green.getColor()); // Green, black
    } else {
      ledStrip.setTop(COLORS_467.Blue);
      ledStrip.setTop(COLORS_467.Gold);
    }
  }

  @Override
  public void end(boolean interrupted) {}

  @Override
  public boolean isFinished() {
    // return intakerelease.haveCube() || intakerelease.haveCone();
    return false;
  }
}
