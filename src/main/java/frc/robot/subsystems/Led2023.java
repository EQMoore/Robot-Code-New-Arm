package frc.robot.subsystems;

import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.util.Color8Bit;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.lib.leds.LEDManager;
import frc.lib.leds.LEDStrip;
import frc.robot.RobotConstants;

public class Led2023 extends SubsystemBase {
  public LEDStrip ledStrip;

  public Led2023() {
    super();

    ledStrip = LEDManager.getInstance().createDoubleStrip(RobotConstants.get().led2023LedCount());
    for (int i = 0; i < ledStrip.getSize(); i++) {
      ledStrip.setRGB(i, 0, 0, 0);
    }
  }

  public void setLED(int index, Color color) {
    ledStrip.setLED(index, color);
  }

  public void setLED(int index, Color8Bit color) {
    ledStrip.setLED(index, color);
  }

  public void setRGB(int index, int r, int g, int b) {
    ledStrip.setRGB(index, r, g, b);
  }

  public void setHSV(int index, int h, int s, int v) {
    ledStrip.setHSV(index, h, s, v);
  }

  public void setHSB(int index, float h, float s, float b) {
    ledStrip.setHSB(index, h, s, b);
  }

  public void setHSB(int index, int h, int s, int b) {
    setHSB(index, h / 360f, s / 255f, b / 255f);
  }

  public void sendData() {
    ledStrip.update();
  }

  @Override
  public void initSendable(SendableBuilder builder) {
    super.initSendable(builder);
  }
}
