package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class TestBench {
    private DigitalChannel touchSensor; // touchSensorOnClaw

    private DcMotor motor;
    private double ticksPerRev;

    public void init(HardwareMap hwMap){
        // touch sensor
        touchSensor = hwMap.get(DigitalChannel.class, "touch_sensor");
        touchSensor.setMode(DigitalChannel.Mode.INPUT);
        //Dc motor
        motor = hwMap.get(DcMotor.class, "motor");
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        ticksPerRev = motor.getMotorType().getTicksPerRev();
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        // motor.setDirection(DcMotorSimple.Direction.REVERSE);
    }


    //------ Touch Sensor ------
    public boolean getTouchSensorState(){
        return !touchSensor.getState();
    }

    public boolean isTouchSensorReleased(){
        return touchSensor.getState();
    }


    //------ DC Motor ------
    public void setMotorSpeed(double speed){
        // accepts values form -1.0 to 1.0
        motor.setPower(speed);
    }
    public double getMotorRevs(){
        return motor.getCurrentPosition() / ticksPerRev; // normalizing ticks to revolutions
    }

    public void changeBrakeMode(DcMotor.ZeroPowerBehavior zeroBehavior){
        motor.setZeroPowerBehavior(zeroBehavior);
    }

}
