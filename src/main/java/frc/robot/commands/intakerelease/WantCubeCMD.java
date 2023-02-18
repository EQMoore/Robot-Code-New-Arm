package frc.robot.commands.intakerelease;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.Led2023;
import frc.robot.subsystems.Led2023.COLORS_467;
import frc.robot.subsystems.intakerelease.IntakeRelease;
import frc.robot.subsystems.intakerelease.IntakeRelease.Wants;

public class WantCubeCMD extends CommandBase {
  private IntakeRelease intakerelease;
  private Led2023 ledStrip;

  public WantCubeCMD(IntakeRelease intakerelease, Led2023 ledStrip) {
    this.intakerelease = intakerelease;

    addRequirements(intakerelease);
    this.ledStrip = ledStrip;
    addRequirements(ledStrip);
  }

  @Override
  public void initialize() {}

  @Override
  public void execute() {
    intakerelease.setWants(Wants.CUBE);
    ledStrip.set(COLORS_467.Blue);
    ledStrip.sendData();
  }

  @Override
  public void end(boolean interrupted) {}

  @Override
  public boolean isFinished() {
    return false;
  }
}
