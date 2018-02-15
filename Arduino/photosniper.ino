#include <multiCameraIrControl.h>#include <SoftwareSerial.h>
#define BLUETOOTH_SPEED 9600 //This is the default baudrate that HC-10 uses
#define PIN_RX 3#define PIN_TX 2  #define PIN_IR 6  // IR LED Haxe 2#define PIN_AF 7     // Pin for Autofocus-Optocoupler#define PIN_SHUTTER  8  // Pin for Shutter-Optocoupler
//Camera Storage Delay#define Camera_delay      250     //Time in ms for which the script will be delayed after one cyle in infinite mode. In this Time the camera will store the Photo on the SD Card. Increase Value if your camera skips some cycles.#define MIROR_UP false 
// IR CameraIDs#define IR_CAM_NONE 0#define IR_CAM_CANON 1#define IR_CAM_NIKON 2#define IR_CAM_MINOLTA 3#define IR_CAM_OLYMPUS 4#define IR_CAM_PENTAX 5#define IR_CAM_SONY 6
#define CMD_BUFFER_LEN (byte)4#define CMD_SEQUENCEBUFFER_SIZE (byte)128
byte[] CMD_START_TAG = {0x0f,0x01,0x02,0x03};byte[] CMD_END_TAG = {0x0f,0x0f,0x0f,0x0f};
//------------------------------
SoftwareSerial bleSerial(PIN_RX, PIN_TX); // RX, TX§
char commndBuffer[numChars];   // an array to store the received data
boolean ir_enabled = false;    // use IR in generalboolean cam_busy = false;    // processing cmdsboolean shutterReleased = false; // shutter busy
int old_ir_id = IR_CAM_NONE;
IRCamera *camera = NULL;
void setup() {  //initialize serial port for logs  Serial.begin(BLUETOOTH_SPEED);  while (!Serial) {  }  bleSerial.begin(BLUETOOTH_SPEED);  bleSerial.write("AT+DEFAULT");  bleSerial.write("AT+RESET");  bleSerial.write("AT+NAMESniper");  bleSerial.write("AT+ROLE1");  bleSerial.write("AT+TYPE1"); //Simple pairing
  Serial.println("<SniperBox is ready>");
}void loop() {
  recvCommand();
}
void recvCommand() {
  static byte ndx = 0;  byte cmdIx = 0;  byte[CMD_BUFFER_LEN] oneCmdBuffer; 
  char rc;
  // maybe shorter -> bleSerial.readBytesUntil(CMD_END_CHAR, commndBuffer, sizeof(commndBuffer) / sizeof(char) );
  boolean inCmdMode = false;
  while (bleSerial.available() > 0 ) {    rc = bleSerial.read();
    // all commands are CMD_BUFFER_LEN byte sequences !! BEGIN & END TAGS are stripped
    if (!cam_busy)    {  oneCmdBuffer[cmdIx++] = rc;    if (rc == CMD_BUFFER_LEN) // preprocess single cmd  {   cmdIx = 0;      // check if we got a CMD_TAG      if (inCmdMode)   {    // is not END_TAG    if ((oneCmdBuffer[0] != CMD_END_TAG[0]) && (oneCmdBuffer[1] != CMD_END_TAG[1]) && (oneCmdBuffer[2] != CMD_END_TAG[2]) && (oneCmdBuffer[3] != CMD_END_TAG[3]))    {     // add cmd to buffer     for (int i = 0; i < CMD_BUFFER_LEN; i++)     {      commndBuffer[ndx++] = oneCmdBuffer[i];              if (ndx >= CMD_SEQUENCEBUFFER_SIZE) {       ndx = CMD_SEQUENCEBUFFER_SIZE - 1;        }           }    }    else    {     // cmd sequence is ready - we goto processing     cam_busy = true;              processCommandChain(commndBuffer, ndx);
     cam_busy = false;     commndBuffer[ndx] = '\0'; // terminate the string     ndx = 0;         }   }   else   {    inCmdMode = ((oneCmdBuffer[0] == CMD_START_TAG[0]) && (oneCmdBuffer[1] == CMD_START_TAG[1]) && (oneCmdBuffer[2] == CMD_START_TAG[2]) && (oneCmdBuffer[3] == CMD_START_TAG[3]))   }  }    }  }}
void processCommandChain(char* cmdSequence, int bytesUsed) {
  Serial.println(cmdSequence);
  int x = 0;  while ( x < bytesUsed )  {    // get a cmd with params      byte* cmd = cmdSequence[x++];    word* param1 = cmdSequence[x];    x += 2;    byte* param2 = cmdSequence[x++];
 // check last cmd to be a loop ;-)     if (processCommand(cmd, param1, param2)) {  // we found a STOP cmd  break; }
  }}
boolean processCommand(byte cmd, word param1, byte param2){ boolean breakout = false;  switch (cmd)  {    case 0x01 : // 'B'      closeShutter();      break;    case 0x02 : // 'C'      openShutter();      break;    case 0x03 : // 'D' for (int i = 0; i < param2;i++)  // little delay {  delay(param1); }      break;    case 0x05 : // 'H'       chooseIR(param1);      break; case 0x06 : // 'I'  breakout = true;  break;    case 0x07 : // 'J'       toggleBLE(param1);      break;      default:      Serial.print("unknown command:");      Serial.println(cmd);
  }}

//----------- business function
// Bvoid closeShutter(){ if (shutterReleased)  return;   // close wire switch  Serial.println("close Shutter");
    if(MIROR_UP == true){    //Use if Mirror Up Setting is Activated    //Trigger Mirror-Up is set    digitalWrite(PIN_AF, HIGH);  //Activate Focus    delay(40);  //Give the Camera some time to react    digitalWrite(PIN_SHUTTER, HIGH); //Activate Shutter    delay(100);  //Wait for the camera to move mirror up, then Deactivate Shutter/AF    digitalWrite(PIN_SHUTTER, LOW); //Deactivate Shutter    digitalWrite(PIN_AF, LOW);  //Deactivate Focus    delay(250);   //Pause to reduce vibrations of the camera    //Mirror is now UP, Start Trigger routine  }   //Set AF and Shutter Pin to high    digitalWrite(PIN_AF, HIGH);  //Activate Focus    delay(40);  //Give the Camera some time to react    digitalWrite(PIN_SHUTTER, HIGH); //Activate Shutter  
 shutterReleased = true;     // close IR switch  if (ir_enabled)  {    Serial.println("close IR Shutter");    camera->shotNow();  }
}
// Cvoid openShutter(){  // open wire switch  Serial.println("open Shutter");   //Reset AF and Shutter Pin to LOW    digitalWrite(PIN_SHUTTER, LOW); //Deactivate Shutter    digitalWrite(PIN_AF, LOW);  //Deactivate Focus    //delay(200);  //Give the Camera a short break    // open IR switch  if (ir_enabled)  {    Serial.println("open IR Shutter");  }  shutterReleased = false;}
// Avoid takeSinglePicture ( int delay_ms, int expose_ms, int loop ){  for (int i = 0; i < loop; i++)  {
    delayMicroseconds(delay_ms << 10);    closeShutter();
    delayMicroseconds(expose_ms << 10);    openShutter();  }}
// Hvoid chooseIR( int cameraType){  Serial.print("IR group selected:");  Serial.println(cameraType);
  ir_enabled = (cameraType > IR_CAM_NONE);
  if ( ir_enabled )  {    if ( cameraType != old_ir_id)    {      // delete old      if ( camera )      {        delete camera;        camera = NULL;      }    }    if ( !camera )    {      old_ir_id = cameraType;      switch (cameraType)      {        case IR_CAM_CANON :          camera = new Canon(PIN_IR);          break;        case IR_CAM_NIKON :          camera = new Nikon(PIN_IR);          break;        case IR_CAM_MINOLTA :          camera = new Minolta(PIN_IR);          break;        case IR_CAM_OLYMPUS :          camera = new Olympus(PIN_IR);          break;        case IR_CAM_PENTAX :          camera = new Pentax(PIN_IR);          break;        case IR_CAM_SONY :          camera = new Sony(PIN_IR);          break;      }    }  }
  // ....}
// Jvoid toggleBLE( int enableBLE){  Serial.println("toggle BLE");
  if (!enableBLE)  {    bleSerial.write("AT+SLEEP");  }  else  {    bleSerial.write("12345678901234567890123456789012345678901234567890123456789012345678901234567890");  }
  // enable/disable BLE}
