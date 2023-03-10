package frc.robot.commands.arm;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.led.Led2023;
import frc.robot.subsystems.led.Led2023.COLORS_467;

public class ArmManualUpCMD extends CommandBase {
  private final Arm arm;
  private Led2023 ledStrip;

  public ArmManualUpCMD(Arm arm, Led2023 ledStrip) {
    this.arm = arm;
    this.ledStrip = ledStrip;
    addRequirements(arm, ledStrip);
  }

  @Override
  public void initialize() {
    ledStrip.set(COLORS_467.Black);
  }

  @Override
  public void execute() {
    arm.manualRotate(7);
    System.out.println("Arm UP!!@!");
  }

  @Override
  public void end(boolean interrupted) {
    arm.hold();
    ledStrip.defaultLights();
  }
}
