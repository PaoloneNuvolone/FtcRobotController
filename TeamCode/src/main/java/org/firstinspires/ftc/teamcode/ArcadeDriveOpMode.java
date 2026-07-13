package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.mechanisms.ArcadeDrive;

@TeleOp(name = "Arcade_Drive_Test")
public class ArcadeDriveOpMode extends OpMode {

    ArcadeDrive drive = new ArcadeDrive();
    double throttle;
    double spin;

    @Override
    public void init() {
        drive.init(hardwareMap);
    }

    @Override
    public void loop() {
        throttle = -gamepad1.left_stick_y;
        spin = gamepad1.left_stick_x;

        drive.drive(throttle,spin);
    }
}
