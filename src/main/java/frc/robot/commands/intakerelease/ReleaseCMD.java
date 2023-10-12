package frc.robot.commands.intakerelease;

// import frc.robot.commands.arm.ArmDropCMD;
// import frc.robot.commands.arm.ArmHomeCMD;
// import frc.robot.subsystems.arm.Arm;

// public class ReleaseCMD extends SequentialCommandGroup {
//   public ReleaseCMD(IntakeRelease intakerelease, Arm arm) {
//     addCommands(
//         new ConditionalCommand(
//             Commands.sequence(
//                 new ArmDropCMD(intakerelease::haveCone, intakerelease::wantsCone, arm),
//                 // .withTimeout(0.4),
//                 // new WaitCommand(0.5),
//                 Commands.parallel(
//                         Commands.run(intakerelease::release, intakerelease).withTimeout(0.5),
//                         new ArmHomeCMD(arm, intakerelease::wantsCone))
//                     .withTimeout(5.0)),
//             Commands.sequence(
//                 new ArmDropCMD(intakerelease::haveCone, intakerelease::wantsCone, arm)
//                     .withTimeout(0.4),
//                 Commands.run(intakerelease::release, intakerelease).withTimeout(0.5),
//                 new ArmHomeCMD(arm, intakerelease::wantsCone).withTimeout(5.0)),
//             intakerelease::wantsCone));
//   }
// }
