package com.udacity.catpoint.security.data;

import java.util.Set;

import com.udacity.catpoint.security.application.StatusListener;

/**
 * Interface showing the methods our security repository will need to support
 */
public interface SecurityRepository {
    void addSensor(Sensor sensor);

    void removeSensor(Sensor sensor);

    void updateSensor(Sensor sensor);

    void setAlarmStatus(AlarmStatus alarmStatus);

    void setArmingStatus(ArmingStatus armingStatus);



    Set<Sensor> getSensors();

    AlarmStatus getAlarmStatus();

    ArmingStatus getArmingStatus();



}
