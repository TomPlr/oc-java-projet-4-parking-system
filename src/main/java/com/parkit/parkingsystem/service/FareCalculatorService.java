package com.parkit.parkingsystem.service;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, Boolean hasDiscount){
        DecimalFormat df = new DecimalFormat("0.00");

        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        LocalDateTime inTime = LocalDateTime.ofInstant(ticket.getInTime().toInstant(), ZoneId.systemDefault());
        LocalDateTime outTime = LocalDateTime.ofInstant(ticket.getOutTime().toInstant(), ZoneId.systemDefault());

        double duration = (double) Duration.between(inTime, outTime).toMinutes() / 60;

        if(duration <= 0.5){
            ticket.setPrice(0);
        } else {
            double discount = hasDiscount ? 0.95 : 1 ;
            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR * discount);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR * discount);
                    break;
                }
                default: throw new IllegalArgumentException("Unknown Parking Type");
            }
        }
    }

    public void calculateFare(Ticket ticket){
        calculateFare(ticket, false);
    }
}