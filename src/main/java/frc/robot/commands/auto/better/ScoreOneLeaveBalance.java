package frc.robot.commands.auto.better;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.lib.holonomictrajectory.Waypoint;
import frc.robot.FieldConstants;
import frc.robot.FieldConstants.Community;
import frc.robot.commands.auto.Balancing;
import frc.robot.commands.drive.GoToTrajectory;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.drive.Drive;
import frc.robot.subsystems.intakerelease.IntakeRelease;
import frc.robot.subsystems.led.Led2023;
import java.util.List;

public class ScoreOneLeaveBalance extends SequentialCommandGroup {
  public ScoreOneLeaveBalance(Drive drive, Arm arm, IntakeRelease intakeRelease, Led2023 ledStrip) {
    // generic so that we can use this in SuperAuto
    Pose2d startingPosition =
        new Pose2d(
            new Translation2d(
                Community.outerX + 1.5,
                (Community.chargingStationLeftY + Community.chargingStationRightY) / 2),
            new Rotation2d(Math.PI));
    boolean enterFront =
        startingPosition.getX()
            < (Community.chargingStationInnerX + Community.chargingStationOuterX) / 2.0;
    Pose2d position0 =
        new Pose2d(
            enterFront ? Community.chargingStationInnerX : Community.chargingStationOuterX,
            //            enterFront
            //                ? Community.chargingStationInnerX - 0.6
            //                : Community.chargingStationOuterX + 0.6,
            MathUtil.clamp(
                startingPosition.getY(),
                Community.chargingStationRightY + 0.6,
                Community.chargingStationLeftY - 0.6),
            Rotation2d.fromDegrees(startingPosition.getRotation().getCos() > 0.0 ? 0.0 : 180.0));
    Pose2d position1 =
        new Pose2d(
            (Community.chargingStationOuterX + Community.chargingStationInnerX) / 2.0,
            position0.getY(),
            position0.getRotation());

    //    addCommands(new ArmCalibrateZeroAtHomeCMD(arm));
    //    addCommands(new ScoreConeHigh(drive, arm, intakeRelease, ledStrip, 6));

    addCommands(
        new GoToTrajectory(
            drive,
            List.of(
                Waypoint.fromHolonomicPose(
                    FieldConstants.aprilTags
                        .getTagPose(6)
                        .get()
                        .toPose2d()
                        .transformBy(
                            new Transform2d(new Translation2d(), Rotation2d.fromDegrees(180)))),
                Waypoint.fromDifferentialPose(
                    new Pose2d(
                        new Translation2d(
                            Community.midX, (Community.chargingStationLeftY + Community.leftY) / 2),
                        new Rotation2d())),
                Waypoint.fromDifferentialPose(
                    new Pose2d(
                        new Translation2d(
                            Community.outerX + 0.5,
                            (Community.chargingStationLeftY + Community.leftY) / 2),
                        new Rotation2d())),
                new Waypoint(
                    new Translation2d(
                        Community.outerX + Community.chargingStationWidth,
                        (Community.chargingStationLeftY + Community.chargingStationRightY) / 2)),
                Waypoint.fromHolonomicPose(
                    position0,
                    enterFront ? Rotation2d.fromDegrees(0.0) : Rotation2d.fromDegrees(180.0)),
                Waypoint.fromHolonomicPose(position1))));
    //    addCommands(
    //        new GoToTrajectory(
    //            drive,
    //            List.of(
    //                Waypoint.fromHolonomicPose(startingPosition),
    //                Waypoint.fromHolonomicPose(
    //                    position0,
    //                    enterFront ? Rotation2d.fromDegrees(0.0) : Rotation2d.fromDegrees(180.0)),
    //                Waypoint.fromHolonomicPose(position1))));
    addCommands(Commands.waitSeconds(0.3));
    addCommands(new Balancing(drive));
  }
}
