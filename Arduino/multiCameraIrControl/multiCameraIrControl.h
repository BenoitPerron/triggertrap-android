/*******************************************
 *
 * Name.......:  cameraIrControl Library
 * Description:  A powerful Library to control easy various cameras via IR. Please check the project page and leave a comment.
 * Author.....:  Sebastian Setz
 * Version....:  1.2
 * Date.......:  2010-12-16
 * Project....:  http://sebastian.setz.name/arduino/my-libraries/multi-camera-ir-control
 * Contact....:  http://Sebastian.Setz.name
 * License....:  This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 *               To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ or send a letter to
 *               Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 * Keywords...:  arduino, library, camera, ir, control, canon, nikon, olympus, minolta, sony, pentax, interval, timelapse
 *
 ********************************************/

#ifndef multiCameraIrControl_h
#define multiCameraIrControl_h

#include "Arduino.h"

class IRCamera{
	
public:
  IRCamera(int pin);

  void shotNow();
  void shotDelayed();
  
protected:  
  
  int _pin;
  int _freq;
};

class Nikon: public IRCamera{
	
public:
  Nikon(int pin);
  void shotNow();
};

class Canon: public IRCamera{
public:
  Canon(int pin);
  void shotNow();
  void shotDelayed();
};

class Pentax: public IRCamera{
public:
  Pentax(int pin);
  void shotNow();
};

class Olympus: public IRCamera{
public:
  Olympus(int pin);
  void shotNow();
};

class Minolta: public IRCamera{
public:
  Minolta(int pin);
  void shotNow();
  void shotDelayed();
};

class Sony: public IRCamera{
public:
  Sony(int pin);
  void shotNow();
  void shotDelayed();
};

#endif
