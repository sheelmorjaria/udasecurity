package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.udacity.catpoint.security.data.AlarmStatus.*;


/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {
    //changed to interface, design by contract
    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();

    //variable to share state between methods, with initial state
    private boolean catDetected = false;


    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }
    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    //update to alarm if cat detected and system armed-home. Deactivate all sensors if armed at all
    public  void setArmingStatus(ArmingStatus armingStatus){
        //test 9
        if(armingStatus == ArmingStatus.DISARMED){
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }//to pass test 11
        else if (armingStatus==ArmingStatus.ARMED_HOME && catDetected) {
            setAlarmStatus(ALARM);
        }
        //to pass test 10
        if(armingStatus == ArmingStatus.ARMED_HOME||armingStatus == ArmingStatus.ARMED_AWAY){
            Set<Sensor> sensors = activeSensors(getSensors());
            //use sensor set of active sensors to be deactivated
            changeSensorActivationStatus(sensors, false);
        }
        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(statusListener -> statusListener.sensorStatusChanged());
    }



    /**
     * Check if a cat is detected in the provided image. If a cat is detected and the system is armed-home,
     * change the alarm status to ALARM.
     * @param cat
     * @return
     */
    private boolean catDetected(boolean cat) {
        //update shared state
        catDetected = cat;
        //test 7
        if (catDetected && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(ALARM);
        }//test 8
        else if (!catDetected && allSensorsInactive()) {
            setAlarmStatus(NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(catDetected));
        return  catDetected;
    }
    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of
     * the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {

        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(statusListener -> statusListener.notify(status));
    }
    /**
     * Internal method for updating the alarm status when a sensor has been activated.*/
    public void handleSensorActivated(){
        if(getArmingStatus() == ArmingStatus.DISARMED){
            return;
        }
        //test 2 and 5
        else if(getAlarmStatus() == AlarmStatus.PENDING_ALARM){
            setAlarmStatus(ALARM);
        }//test 1
        else if(getAlarmStatus()==AlarmStatus.NO_ALARM){
            setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated*/
    public void handleSensorDeactivated() {

        if (getAlarmStatus()== PENDING_ALARM) {
            setAlarmStatus(NO_ALARM);
        }
    }

    public Set<Sensor> activeSensors(Set<Sensor> sensors) {
        Set<Sensor> activeSensors = sensors.stream().filter(Sensor::getActive).collect(Collectors.toSet());
        return activeSensors;
    }

    public boolean allSensorsInactive(){
        return getSensors().stream().noneMatch(Sensor::getActive);
    }

    public void changeSensorActivationStatus(Set<Sensor> sensors, Boolean active) {
        //test 3
        if (sensors.stream().noneMatch(Sensor::getActive)) {
            if (getAlarmStatus() == PENDING_ALARM) {
                setAlarmStatus(NO_ALARM);
            }
        }
        for (Sensor sensor : sensors) {
            changeSensorActivationStatus(sensor,active);
        }
    }

    public void changeSensorActivationStatus(Sensor sensor, Boolean active){
        //test 4
        if (getAlarmStatus() == PENDING_ALARM || getAlarmStatus() == NO_ALARM) {
            //handle sensor activation
            if (!sensor.getActive() && active) {
                handleSensorActivated();
                //handles sensor deactivation
            }else if (sensor.getActive() && !active) {
                handleSensorDeactivated();
            } //test 5
            else if (sensor.getActive() && active) {
                handleSensorActivated();
            }//test 6
            else if (!sensor.getActive() && !active) {
                return;
            }
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }
    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }
    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }
    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }
    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }
    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }
    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
