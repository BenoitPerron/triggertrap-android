#include <multiCameraIrControl.h>

#include <SoftwareSerial.h>
// #include <string.h>

#define BLUETOOTH_SPEED 9600 //This is the default baudrate that HC-10 uses

#define PIN_RX 3
#define PIN_TX 2
#define PIN_IR 6
#define PIN_AF 7    	//Pin for Autofocus-Optocoupler
#define PIN_SHUTTER  8  //Pin for Shutter-Optocoupler

//Camera Storage Delay
#define Camera_delay      250     //Time in ms for which the script will be delayed after one cyle in infinite mode. In this Time the camera will store the Photo on the SD Card. Increase Value if your camera skips some cycles.
#define MIROR_UP false 

// IR CameraIDs
#define IR_CAM_NONE 0
#define IR_CAM_CANON 1
#define IR_CAM_NIKON 2
#define IR_CAM_MINOLTA 3
#define IR_CAM_OLYMPUS 4
#define IR_CAM_PENTAX 5
#define IR_CAM_SONY 6

#define CMD_START_TAG '<'
#define CMD_END_TAG '>'

SoftwareSerial bleSerial(PIN_RX, PIN_TX); // RX, TX§

const byte numChars = 64;
char commndBuffer[numChars];  	// an array to store the received data

boolean ir_enabled = false;   	// use IR in general
boolean cam_busy = false;   	// processing cmds
boolean shutterReleased = false; // shutter busy

int old_ir_id = IR_CAM_NONE;

IRCamera *camera = NULL;

void setup() {
  //initialize serial port for logs
  Serial.begin(BLUETOOTH_SPEED);
  while (!Serial) {
  }
  bleSerial.begin(BLUETOOTH_SPEED);
  bleSerial.write("AT+DEFAULT");
  bleSerial.write("AT+RESET");
  bleSerial.write("AT+NAMESniper");
  bleSerial.write("AT+ROLE1");
  bleSerial.write("AT+TYPE1"); //Simple pairing

  Serial.println("<SniperBox is ready>");

}
void loop() {

  recvCommand();

}


void recvCommand() {

  static byte ndx = 0;

  char rc;

  // maybe shorter -> bleSerial.readBytesUntil(CMD_END_CHAR, commndBuffer, sizeof(commndBuffer) / sizeof(char) );

  boolean inCmd = false;

  while (bleSerial.available() > 0 ) {
    rc = bleSerial.read();

    // all commands are 3 byte sequences !! BEGIN & END TAGS are stripped

    if (!cam_busy)
    {
      if (rc != CMD_END_TAG) {

        if (inCmd)
        {
          commndBuffer[ndx] = rc;
          ndx++;
          if (ndx >= numChars) {
            ndx = numChars - 1;
          }
        }
        else
        {
          inCmd = (rc == CMD_START_TAG);
        }

      }
      else
      {
        inCmd = false;
        // String cmd(commndBuffer);
        cam_busy = true;

        processCommandChain(commndBuffer, ndx);

        cam_busy = false;
        commndBuffer[ndx] = '\0'; // terminate the string
        ndx = 0;

      }
    }
  }
}

void processCommandChain(char* cmdSequence, int bytesUsed) {

  Serial.println(cmdSequence);

  // first we get the single groups
  // char* command = strtok(cmdSequence, ";");

  int x = 0;
  while ( x < bytesUsed )
  {
    char* cmd = cmdSequence[x++];
    word* param1 = cmdSequence[x];
    x += 2;
    byte* param2 = cmdSequence[x++];

    processCommand(cmd, param1, param2);

  }
}

void processCommand(char cmd, word param1, byte param2)
{
  switch (cmd)
  {
    case 'B' :
      closeShutter();
      break;
    case 'C' :
      openShutter();
      break;
    case 'J' :
      toggleBLE(param1);
      break;
    case 'H' :
      chooseIR(param1);
      break;
    case 'A' :
      takeSinglePicture (param1, param2, 1);  // TODO: loop
      break;
    default:
      Serial.print("unknown command:");
      Serial.println(cmd);

  }
}




//----------- business function


// B
void closeShutter()
{
	if (shutterReleased)
		return;
	
  // close wire switch
  Serial.println("close Shutter");

    if(MIROR_UP == true){    //Use if Mirror Up Setting is Activated
    //Trigger Mirror-Up is set
    digitalWrite(PIN_AF, HIGH);  //Activate Focus
    delay(40);  //Give the Camera some time to react
    digitalWrite(PIN_SHUTTER, HIGH); //Activate Shutter
    delay(100);  //Wait for the camera to move mirror up, then Deactivate Shutter/AF
    digitalWrite(PIN_SHUTTER, LOW); //Deactivate Shutter
    digitalWrite(PIN_AF, LOW);  //Deactivate Focus
    delay(250);   //Pause to reduce vibrations of the camera
    //Mirror is now UP, Start Trigger routine
  }
  
	//Set AF and Shutter Pin to high
    digitalWrite(PIN_AF, HIGH);  //Activate Focus
    delay(40);  //Give the Camera some time to react
    digitalWrite(PIN_SHUTTER, HIGH); //Activate Shutter  

	shutterReleased = true; 
  
  // close IR switch
  if (ir_enabled)
  {
    Serial.println("close IR Shutter");
    camera->shotNow();
  }

}

// C
void openShutter()
{
  // open wire switch
  Serial.println("open Shutter");
  
	//Reset AF and Shutter Pin to LOW
    digitalWrite(PIN_SHUTTER, LOW); //Deactivate Shutter
    digitalWrite(PIN_AF, LOW);  //Deactivate Focus
    //delay(200);  //Give the Camera a short break
  
  // open IR switch
  if (ir_enabled)
  {
    Serial.println("open IR Shutter");
  }
  shutterReleased = false;
}

// A
void takeSinglePicture ( int delay_ms, int expose_ms, int loop )
{
  for (int i = 0; i < loop; i++)
  {

    delayMicroseconds(delay_ms << 10);
    closeShutter();

    delayMicroseconds(expose_ms << 10);
    openShutter();
  }
}

// H
void chooseIR( int cameraType)
{
  Serial.print("IR group selected:");
  Serial.println(cameraType);

  ir_enabled = (cameraType > IR_CAM_NONE);

  if ( ir_enabled )
  {
    if ( cameraType != old_ir_id)
    {
      // delete old
      if ( camera )
      {
        delete camera;
        camera = NULL;
      }
    }
    if ( !camera )
    {
      old_ir_id = cameraType;
      switch (cameraType)
      {
        case IR_CAM_CANON :
          camera = new Canon(PIN_IR);
          break;
        case IR_CAM_NIKON :
          camera = new Nikon(PIN_IR);
          break;
        case IR_CAM_MINOLTA :
          camera = new Minolta(PIN_IR);
          break;
        case IR_CAM_OLYMPUS :
          camera = new Olympus(PIN_IR);
          break;
        case IR_CAM_PENTAX :
          camera = new Pentax(PIN_IR);
          break;
        case IR_CAM_SONY :
          camera = new Sony(PIN_IR);
          break;
      }
    }
  }


  // ....
}

// J
void toggleBLE( int enableBLE)
{
  Serial.println("toggle BLE");

  if (!enableBLE)
  {
    bleSerial.write("AT+SLEEP");
  }
  else
  {
    bleSerial.write("12345678901234567890123456789012345678901234567890123456789012345678901234567890");
  }

  // enable/disable BLE
}

