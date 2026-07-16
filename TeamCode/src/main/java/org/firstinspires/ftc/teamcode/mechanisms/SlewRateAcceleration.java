package org.firstinspires.ftc.teamcode.mechanisms;

public class SlewRateAcceleration {
    private double currentPower = 0.0;
    private final double maxStep;
    public  SlewRateAcceleration(double maxStep){
        this.maxStep = maxStep;
    }

    public double update(double targetPower){
        // se il joystick viene rilasciato, il robot si fermerà subito
        if (targetPower==0){
            currentPower=0;
            return currentPower;
        }

        // così facendo il robot accelererà gradualemtne (di maxStep alla volta)
        double error = targetPower - currentPower;
        if (error > maxStep){
            error += maxStep;
        } else if (error < -maxStep){
            error -= maxStep;
        } else{
            currentPower = targetPower;
        }
        return currentPower;
    }
}
