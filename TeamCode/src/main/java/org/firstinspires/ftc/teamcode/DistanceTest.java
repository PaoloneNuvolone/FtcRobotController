package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DistanceSensor;

import org.firstinspires.ftc.teamcode.mechanisms.TestBenchDistanceSensor;

@TeleOp
public class DistanceTest extends OpMode {

    TestBenchDistanceSensor bench = new TestBenchDistanceSensor();

    @Override
    public void init() {
        bench.init(hardwareMap);
    }

    @Override
    public void loop() {
        telemetry.addData("Distance CM", bench.getDistance());
    }
}
