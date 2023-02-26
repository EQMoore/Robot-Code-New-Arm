package frc.robot.commands.auto;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.lib.holonomictrajectory.Waypoint;
import frc.robot.FieldConstants;
import frc.robot.commands.arm.ArmScoreHighNodeCMD;
import frc.robot.commands.drive.GoToTrajectory;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.drive.Drive;
import java.util.List;

public class ScoreConeHigh extends SequentialCommandGroup {
  public ScoreConeHigh(Drive drive, Arm arm) {
    addCommands(
        new GoToTrajectory(
            drive,
            List.of(
                new Waypoint(FieldConstants.aprilTags.get(7).getTranslation().toTranslation2d()),
                new Waypoint(
                    new Translation2d(
                        FieldConstants.aprilTags.get(7).getTranslation().toTranslation2d().getX()+0.3,
                        FieldConstants.aprilTags.get(7).getTranslation().toTranslation2d().getY()
                            + FieldConstants.Grids.nodeSeparationY)))),
        new ArmScoreHighNodeCMD(arm));
  }
}
