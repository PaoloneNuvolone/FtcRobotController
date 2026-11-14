package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

public class Flywheel {

    // PROVA
    public DcMotorEx motor;
    private double kp = 4.0;
    private double ki = 0.1;
    private double kd = 0.0;
    private double kf = 13.5;
    public Flywheel(HardwareMap hw){
        motor = hw.get(DcMotorEx.class, "flywheel");
        motor.setDirection(DcMotorEx.Direction.REVERSE);
        motor.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setVelocityPIDFCoefficients(kp,ki,kd,kf);


        //oppure si può fare anche che creo un oggetto con dentro i coefficienti e poi li do a motor
        PIDFCoefficients coefficients = new PIDFCoefficients(kp, ki, kd, kf);
        motor.setPIDFCoefficients(DcMotorEx.RunMode.RUN_USING_ENCODER, coefficients);

    }

}
