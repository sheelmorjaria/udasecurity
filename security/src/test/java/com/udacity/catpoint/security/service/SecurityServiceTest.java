package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.image.service.ImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static com.udacity.catpoint.security.data.AlarmStatus.*;
import static com.udacity.catpoint.security.data.ArmingStatus.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private SecurityService securityService;
    @Mock
    private Sensor sensorOne,sensorTwo;
    private Set<Sensor> allSensors(boolean status){
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensorOne);
        sensors.add(sensorTwo);
        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;


    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
    }


    //1)If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @ValueSource(strings = {"ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndASensorIsActivated_changeSystemStatusToPending(String armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.valueOf(armingStatus));
        when(securityRepository.getAlarmStatus()).thenReturn(NO_ALARM);
        sensorOne.setActive(false);
        securityService.changeSensorActivationStatus(sensorOne,true);
        verify(securityRepository, atMost(1)).setAlarmStatus(PENDING_ALARM);
    }

    //2)If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest
    @ValueSource(strings = {"ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndASensorIsActivated_changeSystemStatusToAlarm(String armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.valueOf(armingStatus));
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensorOne.setActive(false);
        securityService.changeSensorActivationStatus(sensorOne,true);
        verify(securityRepository, atMost(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //3)If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void ifAlarmPendingAndAllSensorsAreInactive_changeSystemStatusToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        Set<Sensor> sensors = allSensors(false);
        securityService.changeSensorActivationStatus(sensors,false);
        verify(securityRepository, atMost(1)).setAlarmStatus(NO_ALARM);
    }
    //4)As described on udacity finding bugs
    @Test
    void ifAlarmActive_changeInSensorStateShouldNotAffectAlarmState(){
        sensorOne.setActive(false);
        sensorTwo.setActive(false);
        securityService.changeSensorActivationStatus(sensorOne, true);
        securityService.changeSensorActivationStatus(sensorTwo,true);
        securityService.changeSensorActivationStatus(sensorOne,false);
        securityService.changeSensorActivationStatus(sensorTwo,false);
        verify(securityRepository, atMost(1)).setAlarmStatus(PENDING_ALARM);
        verify(securityRepository, atMost(1)).setAlarmStatus(ALARM);
    }
    //5)If a sensor is activated while already active and the system is in pending alarm state, set the alarm status to alarm.
    //rigid test
    @Test
    void ifSensorIsActivatedWhileAlreadyActiveAndSystemIsInPendingState_changeSystemStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensorOne.setActive(true);
        securityService.changeSensorActivationStatus(sensorOne,true);
        verify(securityRepository, atMost(1)).setAlarmStatus(ALARM);
    }

    //6)If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @ValueSource(strings = {"ALARM", "PENDING_ALARM", "NO_ALARM"})
    void ifSensorIsDeactivatedWhileAlreadyInactive_makeNoChangesToAlarmState(String alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.valueOf(alarmStatus));
        sensorOne.setActive(false);
        securityService.changeSensorActivationStatus(sensorOne,false);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.valueOf(alarmStatus));
    }

    //7)If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    //rigid test
    @Test
    void ifImageServiceIdentifiesImageContainingCatWhileSystemIsArmedHome_changeSystemStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atMost(1)).setAlarmStatus(ALARM);
    }

    //8)If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void ifImageServiceIdentifiesImageThatDoesNotContainCat_changeStatusToNoAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        Set<Sensor> sensors = allSensors(false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.changeSensorActivationStatus(sensors,false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atMost(1)).setAlarmStatus(NO_ALARM);
    }
    //9)If the system is disarmed, set the status to no alarm.
    //rigid test
    @Test
    void ifSystemIsDisarmed_setStatusToNoAlarm() {
        securityService.setArmingStatus(DISARMED);
        verify(securityRepository, atMost(1)).setAlarmStatus(NO_ALARM);
    }

    //10)As described on udacity fixing bugs
    @ParameterizedTest
    @ValueSource(strings = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemIsArmed_resetAllSensorsToInactive(String armingStatus) {
        sensorOne.setActive(false);
        sensorTwo.setActive(false);
        securityService.changeSensorActivationStatus(sensorOne,true);
        securityService.changeSensorActivationStatus(sensorTwo,true);
        securityRepository.setArmingStatus(ArmingStatus.valueOf(armingStatus));
        assertFalse(sensorOne.getActive());
        assertFalse(sensorTwo.getActive());
    }

    //11)As described on udacity fixing bugs
    @ParameterizedTest
    @ValueSource(strings = {"DISARMED","ARMED_AWAY"})
    void ifSystemIsDisarmedThenCameraShowsCatAndArmingStatusSetToArmedHome_setAlarmStatusToAlarm(String armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.valueOf(armingStatus));
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ARMED_HOME);
        verify(securityRepository, atMost(1)).setAlarmStatus(ALARM);
    }
    //Extra coverage tests
    @Test
    void addRemoveFetchSensor() {
        securityService.addSensor(sensorOne);
        securityService.getSensors();
        securityService.removeSensor(sensorOne);
    }

    @Test
    void addRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    void ifSensorIsActivatedWhileAlarmIsPending_changeSystemStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensorOne.setActive(false);
        securityService.changeSensorActivationStatus(sensorOne,true);

        verify(securityRepository, atMost(1)).setAlarmStatus(ALARM);
    }


    @Test
    void ifSensorIsDeactivatedWhileAlarmIsPending_changeSystemStatusToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensorOne.setActive(true);
        securityService.handleSensorDeactivated();
        verify(securityRepository, atMost(1)).setAlarmStatus(NO_ALARM);
    }

    @Test
    void ifSensorIsActivatedWhileAlarmIsPending_changeSystemToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensorOne.setActive(true);
        securityService.changeSensorActivationStatus(sensorOne,true);
        verify(securityRepository, atMost(1)).setAlarmStatus(ALARM);
    }

    @Test
    void ifSensorIsActivatedWhileAlarmIsNoAlarm_changeSystemToPendingAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(NO_ALARM);
        sensorOne.setActive(true);
        securityService.changeSensorActivationStatus(sensorOne,true);
        verify(securityRepository, atMost(1)).setAlarmStatus(PENDING_ALARM);
    }

    @Test
    void processImage() {
        securityService.processImage(mock(BufferedImage.class));
    }

}
