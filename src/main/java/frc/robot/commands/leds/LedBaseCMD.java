package frc.robot.commands.leds;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.led.Led2023;

public abstract class LedBaseCMD extends CommandBase {

  protected Led2023 ledStrip;

  protected LedBaseCMD(Led2023 ledStrip) {
    this.ledStrip = ledStrip;
    addRequirements(ledStrip);
  }

  @Override
  public void initialize() {
    ledStrip.resetTimers();
  }
}
