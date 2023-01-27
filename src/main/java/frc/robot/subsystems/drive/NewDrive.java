package frc.robot.subsystems.drive;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.lib.io.gyro2d.Gyro2DIO;
import frc.lib.io.gyro2d.Gyro2DIOInputsAutoLogged;
import frc.robot.RobotConstants;
import org.littletonrobotics.junction.Logger;

public class NewDrive extends SubsystemBase {
  private final Gyro2DIO gyroIO;
  private final Gyro2DIOInputsAutoLogged gyroInputs = new Gyro2DIOInputsAutoLogged();
  private final Module[] modules = new Module[4];
  private final SwerveDriveKinematics kinematics = RobotConstants.get().kinematics();

  private boolean isCharacterizing = false;
  private double characterizationVolts = 0.0;
  private ChassisSpeeds setpoint = new ChassisSpeeds();
  private Timer lastMovementTimer = new Timer();

  private SwerveDriveOdometry odometry;

  public NewDrive(
      Gyro2DIO gyroIO,
      ModuleIO flModuleIO,
      ModuleIO frModuleIO,
      ModuleIO blModuleIO,
      ModuleIO brModuleIO) {
    this.gyroIO = gyroIO;
    modules[0] = new Module(flModuleIO, 0);
    modules[1] = new Module(frModuleIO, 1);
    modules[2] = new Module(blModuleIO, 2);
    modules[3] = new Module(brModuleIO, 3);
    lastMovementTimer.start();
    for (var module : modules) {
      module.setBrakeMode(true);
      module.periodic();
    }
    this.gyroIO.updateInputs(gyroInputs);

    SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];
    for (int i = 0; i < 4; i++) {
      modulePositions[i] = modules[i].getPosition();
    }

    odometry =
        new SwerveDriveOdometry(
            kinematics, Rotation2d.fromDegrees(gyroInputs.angle), modulePositions);
  }

  @Override
  public void periodic() {
    // Update inputs for IOs
    gyroIO.updateInputs(gyroInputs);
    Logger.getInstance().processInputs("Drive/Gyro", gyroInputs);
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
    } else if (isCharacterizing) {
      // Run in characterization mode
      for (var module : modules) {
        module.runCharacterization(characterizationVolts);
      }

      // Clear setpoint logs
      Logger.getInstance().recordOutput("SwerveStates/Setpoints", new double[] {});
      Logger.getInstance().recordOutput("SwerveStates/SetpointsOptimized", new double[] {});
    } else {
      Twist2d setpointTwist =
          new Pose2d()
              .log(
                  new Pose2d(
                      setpoint.vxMetersPerSecond * 0.020,
                      setpoint.vyMetersPerSecond * 0.020,
                      new Rotation2d(setpoint.omegaRadiansPerSecond * 0.020)));
      ChassisSpeeds adjustedSpeeds =
          new ChassisSpeeds(
              setpointTwist.dx / 0.020,
              setpointTwist.dy / 0.020,
              setpointTwist.dtheta / 0.020);
      // In normal mode, run the controllers for turning and driving based on the current
      // setpoint
      SwerveModuleState[] setpointStates =
          RobotConstants.get().kinematics().toSwerveModuleStates(adjustedSpeeds);
      SwerveDriveKinematics.desaturateWheelSpeeds(
          setpointStates, RobotConstants.get().maxLinearSpeed());

      // Set setpoints to modules
      SwerveModuleState[] optimizedStates = new SwerveModuleState[4];
      for (int i = 0; i < 4; i++) {
        optimizedStates[i] = modules[i].runSetpoint(setpointStates[i]);
      }

      // Log setpoint states
      Logger.getInstance().recordOutput("SwerveStates/Setpoints", setpointStates);
      Logger.getInstance().recordOutput("SwerveStates/SetpointsOptimized", optimizedStates);
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
    odometry.update(Rotation2d.fromDegrees(gyroInputs.angle), measuredPositions);
  }

  public void runVelocity(ChassisSpeeds speeds) {
    isCharacterizing = false;
    setpoint = speeds;
  }

  public void stop() {
    runVelocity(new ChassisSpeeds());
  }

  public Pose2d getPose() {
    return odometry.getPoseMeters();
  }

  public void setPose(Pose2d pose) {
    SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];
    for (int i = 0; i < 4; i++) {
      modulePositions[i] = modules[i].getPosition();
    }
    odometry.resetPosition(Rotation2d.fromDegrees(gyroInputs.angle), modulePositions, pose);
  }

  public void chassisDrive(double x, double y, double rot, boolean fieldRelative) {
    if (fieldRelative) {
      runVelocity(ChassisSpeeds.fromFieldRelativeSpeeds(x, y, rot, getPose().getRotation()));
    } else {
      runVelocity(new ChassisSpeeds(x, y, rot));
    }
  }

  public void runCharacterizationVolts(double volts) {
    isCharacterizing = true;
    characterizationVolts = volts;
  }

  public double getCharacterizationVelocity() {
    double driveVelocityAverage = 0.0;
    for (var module : modules) {
      driveVelocityAverage += module.getCharacterizationVelocity();
    }
    return driveVelocityAverage / 4.0;
  }
}
