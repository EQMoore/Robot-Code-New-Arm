package frc.robot.subsystems.drive;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.lib.io.gyro3d.IMUIO;
import frc.lib.io.gyro3d.IMUIOInputsAutoLogged;
import frc.lib.io.newvision.VisionIO;
import frc.lib.io.newvision.VisionIO.VisionIOInputs;
import frc.robot.RobotConstants;
import java.util.ArrayList;
import java.util.List;
import org.littletonrobotics.junction.Logger;
import org.photonvision.EstimatedRobotPose;

public class Drive extends SubsystemBase {
  private final IMUIO gyroIO;
  private final IMUIOInputsAutoLogged gyroInputs = new IMUIOInputsAutoLogged();
  private final List<VisionIO> aprilTagCameraIO = new ArrayList<>();
  private final List<VisionIOInputs> aprilTagCameraInputs = new ArrayList<>();
  private final Module[] modules = new Module[4];
  private final SwerveDriveKinematics kinematics = RobotConstants.get().kinematics();

  private boolean isCharacterizing = false;
  private DriveMode driveMode = DriveMode.NORMAL;
  private double characterizationVolts = 0.0;
  private ChassisSpeeds setpoint = new ChassisSpeeds();
  private SwerveModuleState[] lastSetpointStates =
      new SwerveModuleState[] {
        new SwerveModuleState(),
        new SwerveModuleState(),
        new SwerveModuleState(),
        new SwerveModuleState()
      };

  private final SwerveModulePosition[] modulePositions;
  private SwerveDrivePoseEstimator odometry;
  private double simGyro = 0.0;

  public Drive(
      IMUIO gyroIO,
      ModuleIO flModuleIO,
      ModuleIO frModuleIO,
      ModuleIO blModuleIO,
      ModuleIO brModuleIO,
      List<VisionIO> aprilTagCameraIO) {
    this.gyroIO = gyroIO;
    modules[0] = new Module(flModuleIO, 0);
    modules[1] = new Module(frModuleIO, 1);
    modules[2] = new Module(blModuleIO, 2);
    modules[3] = new Module(brModuleIO, 3);
    for (var module : modules) {
      module.setBrakeMode(true);
      module.periodic();
    }
    for (var aprilTagCamera : aprilTagCameraIO) {
      this.aprilTagCameraIO.add(aprilTagCamera);
      this.aprilTagCameraInputs.add(new VisionIOInputs());
    }
    this.gyroIO.updateInputs(gyroInputs);

    modulePositions = new SwerveModulePosition[4];
    for (int i = 0; i < 4; i++) {
      modulePositions[i] = modules[i].getPosition();
    }

    if (gyroInputs.connected) {
      odometry =
          new SwerveDrivePoseEstimator(
              kinematics,
              Rotation2d.fromDegrees(gyroInputs.yaw),
              modulePositions,
              new Pose2d(0, 0, Rotation2d.fromDegrees(180)));
    } else {
      odometry =
          new SwerveDrivePoseEstimator(
              kinematics,
              new Rotation2d(simGyro),
              modulePositions,
              new Pose2d(0, 0, Rotation2d.fromDegrees(180)));
    }
  }

  @Override
  public void periodic() {
    // Update inputs for IOs
    gyroIO.updateInputs(gyroInputs);
    Logger.getInstance().processInputs("Drive/Gyro", gyroInputs);

    for (int i = 0; i < aprilTagCameraIO.size(); i++) {
      aprilTagCameraIO.get(i).updateInputs(aprilTagCameraInputs.get(i));
      Logger.getInstance().processInputs("Drive/Vision" + i, aprilTagCameraInputs.get(i));
    }
    for (var module : modules) {
      module.periodic();
    }

    // Run modules
    if (DriverStation.isDisabled()) {
      // Disable output while disabled
      for (var module : modules) {
        module.stop();
      }

      // Clear setpoint logs
      Logger.getInstance().recordOutput("SwerveStates/Setpoints", new double[] {});
      Logger.getInstance().recordOutput("SwerveStates/SetpointsOptimized", new double[] {});
    } else {
      switch (driveMode) {
        case CHARACTERIZATION -> {
          // Run in characterization mode
          for (var module : modules) {
            module.runCharacterization(characterizationVolts);
          }

          // Clear setpoint logs
          Logger.getInstance().recordOutput("SwerveStates/Setpoints", new double[] {});
          Logger.getInstance().recordOutput("SwerveStates/SetpointsOptimized", new double[] {});
        }
        case NORMAL -> {
          Twist2d setpointTwist =
              new Pose2d()
                  .log(
                      new Pose2d(
                          setpoint.vxMetersPerSecond * 0.020,
                          setpoint.vyMetersPerSecond * 0.020,
                          new Rotation2d(setpoint.omegaRadiansPerSecond * 0.020)));
          ChassisSpeeds adjustedSpeeds =
              new ChassisSpeeds(
                  setpointTwist.dx / 0.020, setpointTwist.dy / 0.020, setpointTwist.dtheta / 0.020);
          // In normal mode, run the controllers for turning and driving based on the current
          // setpoint
          SwerveModuleState[] setpointStates =
              RobotConstants.get().kinematics().toSwerveModuleStates(adjustedSpeeds);
          SwerveDriveKinematics.desaturateWheelSpeeds(
              setpointStates, RobotConstants.get().maxLinearSpeed());

          // Set to last angles if zero
          if (adjustedSpeeds.vxMetersPerSecond == 0.0
              && adjustedSpeeds.vyMetersPerSecond == 0.0
              && adjustedSpeeds.omegaRadiansPerSecond == 0) {
            for (int i = 0; i < 4; i++) {
              setpointStates[i] = new SwerveModuleState(0.0, lastSetpointStates[i].angle);
            }
          }
          lastSetpointStates = setpointStates;

          // Set setpoints to modules
          // boolean isStationary =
          //    Math.abs(setpoint.vxMetersPerSecond) < 1e-3
          //        && Math.abs(setpoint.vyMetersPerSecond) < 1e-3
          //        && Math.abs(setpoint.omegaRadiansPerSecond) < 1e-3;
          boolean isStationary = false; // TODO: can this be removed?

          SwerveModuleState[] optimizedStates = new SwerveModuleState[4];
          for (int i = 0; i < 4; i++) {
            optimizedStates[i] = modules[i].runSetpoint(setpointStates[i], isStationary);
          }

          // Log setpoint states
          Logger.getInstance().recordOutput("SwerveStates/Setpoints", setpointStates);
          Logger.getInstance().recordOutput("SwerveStates/SetpointsOptimized", optimizedStates);
        }
      }
    }

    // Log measured states
    SwerveModuleState[] measuredStates = new SwerveModuleState[4];
    for (int i = 0; i < 4; i++) {
      measuredStates[i] = modules[i].getState();
    }
    Logger.getInstance().recordOutput("SwerveStates/Measured", measuredStates);

    // Update odometry
    SwerveModulePosition[] measuredPositions = new SwerveModulePosition[4];
    for (int i = 0; i < 4; i++) {
      measuredPositions[i] = modules[i].getPosition();
    }
    for (int i = 0; i < aprilTagCameraIO.size(); i++) {
      aprilTagCameraIO.get(i).updateInputs(aprilTagCameraInputs.get(i));
    }

    if (gyroInputs.connected) {
      odometry.update(Rotation2d.fromDegrees(gyroInputs.yaw), measuredPositions);
      simGyro = Units.degreesToRadians(gyroInputs.yaw);
    } else {
      simGyro += kinematics.toChassisSpeeds(measuredStates).omegaRadiansPerSecond * 0.02;
      odometry.update(new Rotation2d(simGyro), measuredPositions);
    }

    for (VisionIOInputs aprilTagCameraInput : aprilTagCameraInputs) {
      //      aprilTagCameraInput.estimatedPose.ifPresent(
      //          estimatedRobotPose -> {
      //            Logger.getInstance()
      //                .recordOutput(
      //                    "Odometry/VisionPose/" +
      // aprilTagCameraInputs.indexOf(aprilTagCameraInput),
      //                    estimatedRobotPose.estimatedPose.toPose2d());
      //
      //            Vector<N3> std =
      //                switch (aprilTagCameraInputs.indexOf(aprilTagCameraInput)) {
      //                  case 0 -> VecBuilder.fill(0.1, 0.1, 0.1); // TODO: Tune these
      //                  case 1 -> VecBuilder.fill(0.1, 0.1, 0.1); // TODO: Tune these
      //                  default -> VecBuilder.fill(1.0, 1.0, 1.0);
      //                };
      //
      //            odometry.addVisionMeasurement(
      //                estimatedRobotPose.estimatedPose.toPose2d(),
      //                estimatedRobotPose.timestampSeconds,
      //                std);
      //          });
      if (aprilTagCameraInput.estimatedPose.isPresent()) {
        EstimatedRobotPose estimatedRobotPose = aprilTagCameraInput.estimatedPose.get();
        Logger.getInstance()
            .recordOutput(
                "Odometry/VisionPose/" + aprilTagCameraInputs.indexOf(aprilTagCameraInput),
                estimatedRobotPose.estimatedPose.toPose2d());

        Vector<N3> std =
            switch (aprilTagCameraInputs.indexOf(aprilTagCameraInput)) {
              case 0 -> VecBuilder.fill(0.001, 0.001, 0.001); // TODO: Tune these
              case 1 -> VecBuilder.fill(0.001, 0.001, 0.001); // TODO: Tune these
              default -> VecBuilder.fill(1.0, 1.0, 1.0);
            };

        odometry.addVisionMeasurement(
            estimatedRobotPose.estimatedPose.toPose2d(), estimatedRobotPose.timestampSeconds, std);
      }
    }

    Logger.getInstance().recordOutput("Odometry", getPose());
  }

  // Checks if the robot is upright within a certain threshold (checks if it will be considered
  // balanced by the charge station)
  public boolean isUpright() {
    // return (Math.abs(
    //         getPose().getRotation().getCos() * getPitch().getDegrees()
    //             + getPose().getRotation().getSin() * getRoll().getDegrees())
    //     < 2.3);

    double pitch = getPitch().getDegrees();
    double roll = getRoll().getDegrees();

    return Math.abs(roll) < 2.3 && Math.abs(pitch) < 2.3;
  }

  public void runVelocity(ChassisSpeeds speeds) {
    driveMode = DriveMode.NORMAL;
    isCharacterizing = false;
    setpoint = speeds;
  }

  public void stop() {
    driveMode = DriveMode.NORMAL;
    runVelocity(new ChassisSpeeds());
  }

  public void stopWithX() {
    stop();
    for (int i = 0; i < 4; i++) {
      lastSetpointStates[i] =
          new SwerveModuleState(
              lastSetpointStates[i].speedMetersPerSecond,
              RobotConstants.get().moduleTranslations()[i].getAngle());
    }
  }

  public double[] getGravVec() {
    return gyroInputs.gravVector;
  }

  public Pose2d getPose() {
    return odometry.getEstimatedPosition();
  }

  public void setPose(Pose2d pose) {
    SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];
    for (int i = 0; i < 4; i++) {
      modulePositions[i] = modules[i].getPosition();
    }
    odometry.resetPosition(Rotation2d.fromDegrees(gyroInputs.yaw), modulePositions, pose);
  }

  public void chassisDrive(double x, double y, double rot, boolean fieldRelative) {
    if (fieldRelative) {
      runVelocity(ChassisSpeeds.fromFieldRelativeSpeeds(x, y, rot, getPose().getRotation()));
    } else {
      runVelocity(new ChassisSpeeds(x, y, rot));
    }
  }

  public Rotation2d getRoll() {
    return Rotation2d.fromDegrees(gyroInputs.roll);
  }

  public Rotation2d getPitch() {
    return Rotation2d.fromDegrees(gyroInputs.pitch);
  }

  public double getRollVelocity() {
    return Units.degreesToRadians(gyroInputs.rollRate);
  }

  public double getPitchVelocity() {
    return Units.degreesToRadians(gyroInputs.pitchRate);
  }

  public void runCharacterizationVolts(double volts) {
    driveMode = DriveMode.CHARACTERIZATION;
    characterizationVolts = volts;
  }

  public double getCharacterizationVelocity() {
    double driveVelocityAverage = 0.0;
    for (var module : modules) {
      driveVelocityAverage += module.getCharacterizationVelocity();
    }
    return driveVelocityAverage / 4.0;
  }

  private enum DriveMode {
    NORMAL,
    CHARACTERIZATION
  }
}
