package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp (name = "New Arcade Drive - FGC",group = "TeleOp Competition")
public class TeleOpMovements extends LinearOpMode {
    private DcMotor leftMotor, rightMotor, upIntakeMotor;
    private DcMotorEx flywheel_left, flywheel_right;
    private Servo servitore1, servitore2;
    private double currentPower = 0.0;
    private final double maxStep = 0.04;
    double intakeVelocity;
    private static final double TARGET_VELOCITY = 1600.0;
    private static final double SERVO_CLOSE = 0.2;
    private static final double SERVO_OPEN = 1.0;

    @Override
    public void runOpMode() throws InterruptedException{

        /// INITIALIZING MOTORS
        leftMotor = hardwareMap.get(DcMotor.class, "left_motor");
        rightMotor = hardwareMap.get(DcMotor.class, "right_motor");
        upIntakeMotor = hardwareMap.get(DcMotor.class, "intake_motor");

        servitore1 = hardwareMap.get(Servo.class, "servitore_1");
        servitore2 = hardwareMap.get(Servo.class, "servitore_2");

        flywheel_right = hardwareMap.get(DcMotorEx.class, "flywheel_right");
        flywheel_left = hardwareMap.get(DcMotorEx.class, "flywheel_left");

        /// SET MOVE DIRECTION OF MOTORS
        leftMotor.setDirection(DcMotor.Direction.REVERSE);
        rightMotor.setDirection(DcMotor.Direction.FORWARD);
        upIntakeMotor.setDirection(DcMotor.Direction.FORWARD);

        flywheel_right.setDirection(DcMotorEx.Direction.FORWARD);
        flywheel_left.setDirection(DcMotorEx.Direction.FORWARD);

        /// SET USING THE ENCODER FOR THE SPEED
        leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        upIntakeMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        flywheel_right.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        flywheel_left.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);

        /// RESETTING THE ENCODER
        leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        flywheel_right.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        flywheel_left.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);

        /// SET THE MOTOR BEHAVIOR WHEN STOPPED
        leftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        upIntakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        flywheel_right.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
        flywheel_left.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);

        /// WHEN THE ROBOT IS READY, PRESS PLAY
        telemetry.addLine("[Initialized] Press Play to start");
        telemetry.update();

        waitForStart();
        while (opModeIsActive()){

            // ARCADE MODE WITH CONTROLLER
            double throttle = -gamepad1.left_stick_y;
            double spin = gamepad1.left_stick_x;

            double leftPower = throttle + spin;
            double rightPower = throttle - spin;

            double max = Math.max(Math.abs(leftPower),Math.abs(rightPower));

            if (max>1.0){
                leftPower /= max;
                rightPower /= max;
            }

            if (gamepad1.right_trigger_pressed){
                leftPower*=0.6;
                rightPower*=0.6;
            }

            // Prima di dare la potenza ai motori, per preservare la batteria, è bene dargli una rampa
            // di accelerazione !NO TESTATO!

            // double leftFilteredPower = update(leftPower);
            // double rightFilteredPower = update(rightPower);

            leftMotor.setPower(leftPower);
            rightMotor.setPower(rightPower);

            // INTAKE WITH R1 e R2
            intakeVelocity = (gamepad1.right_bumper) ? 1 : (gamepad1.left_bumper) ? -1 : 0;
            upIntakeMotor.setPower(intakeVelocity);

            // FLYWHEEL WITH TRIANGOLO e QUADRATO

            /// DA TESTARE!

            if (gamepad1.yWasPressed()){
                // flywheel è un DcMotorEx quindi si può usare setVelocity() al posto di setPower()
                flywheel_right.setVelocity(TARGET_VELOCITY);
                flywheel_left.setVelocity(TARGET_VELOCITY);

                if (flywheel_right.getVelocity()>=(TARGET_VELOCITY-150) && flywheel_left.getVelocity()>=(TARGET_VELOCITY-150)){
                    servitore1.setPosition(SERVO_OPEN);
                    servitore2.setPosition(-SERVO_OPEN);
                    wait(200);
                    upIntakeMotor.setPower(1);
                }
            } else if (gamepad1.xWasPressed()){
                flywheel_right.setVelocity(0);
                flywheel_left.setVelocity(0);

                servitore1.setPosition(SERVO_CLOSE);
                servitore2.setPosition(-SERVO_CLOSE);
                upIntakeMotor.setPower(0);
            }

            // A SUMMARY FOR THE DRIVER
            telemetry.addData("Status", "Running");
            telemetry.addData("Left POWER", leftPower);
            telemetry.addData("Right POWER", rightPower);
            telemetry.addData("Left ENCODER", leftMotor.getCurrentPosition());
            telemetry.addData("Right ENCODER", rightMotor.getCurrentPosition());
            telemetry.addData("Intake Power", upIntakeMotor.getPower());
            telemetry.addData("Right Flywheel velocity",flywheel_right.getVelocity());
            telemetry.addData("Left Flywheel velocity", flywheel_left.getVelocity());
            telemetry.update();
        }

    }
    public double update(double targetPower){
        // se il joystick viene rilasciato, il robot si fermerà subito
        if (targetPower==0){
            currentPower=0;
            return currentPower;
        }

        // così facendo il robot accelererà gradualemtne (di maxStep alla volta)
        double error = targetPower - currentPower;
        if (error < maxStep){
            error += maxStep;
        } else if (error > -maxStep){
            error -= maxStep;
        } else{
            currentPower = targetPower;
        }
        return currentPower;
    }
}
