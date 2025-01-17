package frc.robot.commands.auto.complex;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.commands.auto.Initialize;
import frc.robot.commands.auto.StraightDriveToPose;
// import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.drive.Drive;
import frc.robot.subsystems.intakerelease.IntakeRelease;

public class OnlyBackupBumpSide extends SequentialCommandGroup {

  public OnlyBackupBumpSide(
      int aprilTag, String relativePosition, Drive drive, IntakeRelease intakeRelease) {
    addCommands(
        new Initialize(aprilTag, relativePosition, drive),
        new StraightDriveToPose(Units.inchesToMeters(175.0), 0.0, 0.0, drive));
  }
}
