package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class TestBenchDistanceSensor {
    private TestBenchDistanceSensor distance;

    public void init (HardwareMap hwMap){
        distance = hwMap.get(TestBenchDistanceSensor.class, "sensor_distance");
    }


}
