package frc.robot.commands.intakerelease;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.intakerelease.IntakeRelease;
import frc.robot.subsystems.intakerelease.IntakeRelease.Wants;
import frc.robot.subsystems.led.Led2023;
import frc.robot.subsystems.led.Led2023.COLORS_467;
import frc.robot.subsystems.led.Led2023.ColorScheme;

public class IntakeCMD extends CommandBase {
  private final IntakeRelease intakerelease;
  private final Led2023 ledStrip;
  private final Arm arm;

  public IntakeCMD(IntakeRelease intakerelease, Led2023 ledStrip, Arm arm) {
    this.intakerelease = intakerelease;
    this.ledStrip = ledStrip;
    this.arm = arm;

    addRequirements(intakerelease, ledStrip, arm);
  }

  @Override
  public void initialize() {
    ledStrip.set(COLORS_467.Black);
  }

  @Override
  public void execute() {
    intakerelease.intake();
    if (intakerelease.getWants() == Wants.CUBE) {
      ledStrip.setCmdColorScheme(ColorScheme.INTAKE_CUBE);
    } else if (intakerelease.getWants() == Wants.CONE) {
      ledStrip.setCmdColorScheme(ColorScheme.INTAKE_CONE);
    } else {
      ledStrip.setCmdColorScheme(ColorScheme.INTAKE_UNKNOWN);
    }
  }

  @Override
  public void end(boolean interrupted) {
    if (isFinished()) {
      arm.raise();
    }
    ledStrip.defaultLights();
  }

  @Override
  public boolean isFinished() {
    return (intakerelease.getWants() == Wants.CUBE && intakerelease.haveCube())
        || intakerelease.haveCone();
  }
}
