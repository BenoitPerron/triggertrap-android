#include <SoftwareSerial.h>
#include <string.h>

#define BLUETOOTH_SPEED 9600 //This is the default baudrate that HC-10 uses

#define PIN_RX 3
#define PIN_TX 2
#define PIN_IR 6

#define CMD_END_CHAR '!'

SoftwareSerial bleSerial(PIN_RX, PIN_TX); // RX, TX§

const byte numChars = 64;
char commndBuffer[numChars];  // an array to store the received data

boolean ir_enabled = false;   // use IR in general


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

  Serial.println("<Arduino is ready>");

}
void loop() {
  // if (bleSerial.available()) {
  //     Serial.write(bleSerial.read());
  //     // Serial.write("in receive");
  //  }
  //Serial.write("in loop");
  //  if (Serial.available()) {
  //    bleSerial.write(Serial.read());
  //  }

  recvCommand();

}


void recvCommand() {

  static byte ndx = 0;

  char rc;

  // maybe shorter -> bleSerial.readBytesUntil(CMD_END_CHAR, commndBuffer, sizeof(commndBuffer) / sizeof(char) );

  while (bleSerial.available() > 0 ) {
    rc = bleSerial.read();

    if (rc != CMD_END_CHAR) {
      commndBuffer[ndx] = rc;
      ndx++;
      if (ndx >= numChars) {
        ndx = numChars - 1;
      }
    }
    else
    {
      commndBuffer[ndx] = '\0'; // terminate the string
      ndx = 0;

      String cmd(commndBuffer);

      processCommand(commndBuffer);
    }
  }
}

void processCommand(char* cmdSequence) {

  Serial.println(cmdSequence);

  // first we get the single groups
  char* command = strtok(cmdSequence, ";");

  while (command)
  {
     char* params = strtok(command, ",");

    // int token=0;
    // while (params != null)
    // {
    if ( *command == 'B')
    {
      closeTrigger();
      break;
    }
    else
    {
      if ( *command == 'C')
      {
        openTrigger();
        break;
      }
      else
      {
        if ( *command == 'I')
        {
          return;
        }
        else
        {
          if ( *command == 'J')
          {
            params++;
            int enableBLE = atoi(*params);

            toggleBLE(enableBLE);
          }
          else
          {
            if ( *command == 'H')
            {
              params++;
              int ir_id = atoi(*params);
              chooseIR(ir_id);
              break;
            }
            else
            {
              if ( *command == 'A')
              {
                params++;
                int delay = atoi(*params);
                params++;
                int expose = atoi(*params);
                params++;
                int loop = 1;
                if (params)
                {
                  loop = atoi(*params);
                }
                takeSinglePicture (delay, expose, loop);
                break;
              }
              else
              {
                Serial.print("unknown command:");
                Serial.println(command);
              }
            }
          }
        }
      }
    }
    // TODO: looping over sequence is NYI !!!!
  }

  // token++;
  // }

}



//----------- business function


// B
void closeTrigger()
{
  // close wire switch
  Serial.println("close Shutter");

  // close IR switch
  if (ir_enabled)
  {
    Serial.println("close IR Shutter");
  }

}

// C
void openTrigger()
{
  // open wire switch
  Serial.println("open Shutter");
  // open IR switch
  if (ir_enabled)
  {
    Serial.println("open IR Shutter");
  }
}

// A
void takeSinglePicture ( int delay_ms, int expose_ms, int loop )
{
  for (int i = 0; i < loop; i++)
  {

    delayMicroseconds(delay_ms << 10);
    closeTrigger();

    delayMicroseconds(expose_ms << 10);
    openTrigger();
  }
}

// H
void chooseIR( int cameraType)
{
  Serial.print("IR group selected:");
  Serial.println(cameraType);

  ir_enabled = (cameraType < 0);

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

