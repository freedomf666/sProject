package com.udacity.catpoint;

import com.udacity.catpoint.imageService.ImageService;
import com.udacity.catpoint.securityService.data.*;
import com.udacity.catpoint.securityService.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private final UUID random = UUID.randomUUID();
    private Sensor sensor;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private Set<Sensor> getSensors(int n, boolean status){
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < n; i++) {
            sensors.add(new Sensor(random.toString(),SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository,imageService);
        sensor = new Sensor(random.toString(), SensorType.DOOR);
    }

    //Case 1 If alarm is armed and a sensor becomes activated, put the system into pending alarm status
    @Test
    void alarmArmed_andSensorActivated_putSystemIntoPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


     /*Case 2 If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm
     status to alarm on. [This is the case where all sensors are deactivated and then one gets activated]*/

     @Test
     void alarmArmed_andSensorActivated_andSystemAlreadyPending_setAlarmStatusToAlarm() {
         when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
         when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
         securityService.changeSensorActivationStatus(sensor,true);
         verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);
     }

     //case 3 If pending alarm and all sensors are inactive, return to no alarm state.
     @Test
     void pendingAlarm_andAllSensorsInactive_returnNoAlarmState() {
         when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
         sensor.setActive(false);
         securityService.changeSensorActivationStatus(sensor, false);
         verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
     }

     //case 4 If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans ={true,false})
    void alarmActive_changeInSensorState_notAffectAlarmState(boolean status){
         when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
         securityService.changeSensorActivationStatus(sensor,status);
         verify(securityRepository,never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /* case 5 If a sensor is activated while already active and the system is in pending state, change it to alarm
    state. [This is the case where one sensor is already active and then another gets activated]
     */
    void sensorActivatedWhileAlreadyActive_andSystemPendingState_changeToAlarmState(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository,atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

//case 6 If a sensor is deactivated while already inactive, make no changes to the alarm state.
@ParameterizedTest
@EnumSource(value = AlarmStatus.class, names ={"NO_ALARM","PENDING_ALARM","ALARM"})
void sensorDeactivatedWhileAlreadyInactive_makeNoChangesToAlarmState(AlarmStatus status){
    sensor.setActive(false);
    when(securityRepository.getAlarmStatus()).thenReturn(status);
    securityService.changeSensorActivationStatus(sensor,false);
    verify(securityRepository,never()).setAlarmStatus(any(AlarmStatus.class));
}

//case 7 If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    @Test
void imageContainCatWhileSystemArmedHome_putSystemIntoAlarmStatus() {
    when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }


//case 8 If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void imageNotContainCat_changeStatusToNoAlarm_AsLongAsSensorsNotActive() {
    when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);
    securityService.processImage(mock(BufferedImage.class));
    securityService.changeSensorActivationStatus(sensor,false);
    verify(securityRepository,atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
 }

 //case 9 If the system is disarmed, set the status to no alarm.
    @Test
    void systemDisarmed_setStatusToNoAlarm(){
    securityService.setArmingStatus(ArmingStatus.DISARMED);
    verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//case 10 If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,names = {"ARMED_AWAY", "ARMED_HOME"})
    void systemArmed_resetAllSensorsInactive(ArmingStatus state) {
    Set<Sensor> sensors = getSensors(5,true);
    when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    when(securityService.getSensors()).thenReturn(sensors);
    securityService.setArmingStatus(state);
    securityService.getSensors().forEach(sensor ->{assertFalse(sensor.getActive());});
    }

//case 11 If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    void systemArmedHomeWhileShowsCat_setAlarmStatusToAlarm() {
    when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    securityService.processImage(mock(BufferedImage.class));
    securityService.setAlarmStatus(AlarmStatus.ALARM);
    verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}












