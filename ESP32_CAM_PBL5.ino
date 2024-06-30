#include "esp_camera.h"
#include "Arduino.h"
#include "FS.h"                // SD Card ESP32
#include "SD_MMC.h"            // SD Card ESP32
#include "soc/soc.h"           // Disable brownout problems
#include "soc/rtc_cntl_reg.h"  // Disable brownout problems
#include "driver/rtc_io.h"
#include <EEPROM.h>            // read and write from flash memory
#include <WiFi.h>
#include <HTTPClient.h>
#include <WebServer.h>
int buttonPressed = 0;  
const char* serverAddress = "192.168.12.47";
int serverPort = 8080; // or any other port your server is listening on

// WiFi access 
const char* ssid = "<3";
const char* password = "cocaidaubui09";

#define DEFAULT_SD_FS SD_MMC //For ESP32 SDMMC
#define CARD_TYPE_SD 1

// Photo File Name to save in LittleFS
#define FILE_PHOTO_PATH "/photo.jpg"
#define BUCKET_PHOTO "/data/photo.jpg"

// Define the number of bytes you want to access
#define EEPROM_SIZE 1

// Pin definition for CAMERA_MODEL_AI_THINKER
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27

#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

int pictureNumber = 1;

WebServer server(80); // Create a web server object that listens for HTTP request on port 80

void initWiFi() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
}
void Capture()
{
  camera_fb_t * fb = esp_camera_fb_get();  
  if(!fb) {
    Serial.println("Camera capture failed");
    return; 
  }


  String path = "/photo" + String(pictureNumber) + ".jpg";

  fs::FS &fs = SD_MMC; 
  Serial.printf("Picture file name: %s\n", path.c_str());
  
  File file = fs.open(path.c_str(), FILE_WRITE);
  if(!file){
    Serial.println("Failed to open file in writing mode");
  } else {
    file.write(fb->buf, fb->len); // payload (image), payload length
    file.flush(); // Ensure data is written to the SD card
    Serial.printf("Saved file to path: %s\n", path.c_str());

    EEPROM.write(0, pictureNumber);
    EEPROM.commit();
  }
  file.close();
  esp_camera_fb_return(fb);
}
void takeAndSavePhoto() {
  Capture();
  Capture();
  camera_fb_t * fb = esp_camera_fb_get();  
  if(!fb) {
    Serial.println("Camera capture failed");
    return; 
  }


  String path = "/photo" + String(pictureNumber) + ".jpg";

  fs::FS &fs = SD_MMC; 
  Serial.printf("Picture file name: %s\n", path.c_str());
  
  File file = fs.open(path.c_str(), FILE_WRITE);
  if(!file){
    Serial.println("Failed to open file in writing mode");
  } else {
    file.write(fb->buf, fb->len); // payload (image), payload length
    file.flush(); // Ensure data is written to the SD card
    Serial.printf("Saved file to path: %s\n", path.c_str());

    EEPROM.write(0, pictureNumber);
    EEPROM.commit();
    // Create the URL for the Flask server
    String url = "http://" + String(serverAddress) + ":" + String(serverPort) + "/upload";
    // Create an HTTPClient object
    HTTPClient http;
    // Begin HTTP connection
    http.begin(url);
    // Set the Content-Type header to image/jpeg
    http.addHeader("Content-Type", "image/jpeg");
    // Send the image data
    int httpResponseCode = http.POST(fb->buf, fb->len);
    // Check the server response
    if (httpResponseCode > 0) {
      Serial.print("Photo sent successfully, HTTP response code: ");
      Serial.println(httpResponseCode);
    } else {
      Serial.print("Failed to send photo, HTTP response code: ");
      Serial.println(httpResponseCode);
    }
    http.end();
  }

  file.close();
  esp_camera_fb_return(fb);
}

void handleCaptureRequest() {
  if (server.hasArg("message") && server.arg("message") == "Capture") {
    // takeAndSavePhoto();
    buttonPressed = 1 ;
    server.send(200, "text/plain", "Photo captured and saved");
  } else {
    server.send(400, "text/plain", "Invalid request");
  }
}

void setup() {
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); // Disable brownout detector
 
  Serial.begin(115200);

  initWiFi();
  
  // Configure the camera
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG; 

  if(psramFound()){
    config.frame_size = FRAMESIZE_UXGA; // FRAMESIZE_ + QVGA|CIF|VGA|SVGA|XGA|SXGA|UXGA
    config.jpeg_quality = 10;
    config.fb_count = 2;
  } else {
    config.frame_size = FRAMESIZE_SVGA;
    config.jpeg_quality = 12;
    config.fb_count = 1;
  }

  // Init Camera
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x", err);
    return;
  }
  
  sensor_t * s = esp_camera_sensor_get();

  s->set_brightness(s, 0);
  s->set_contrast(s, -2);
  s->set_saturation(s, -1);
  s->set_special_effect(s, 0);
  s->set_whitebal(s, 0);
  s->set_awb_gain(s, 0);
  s->set_wb_mode(s, 0);
  s->set_exposure_ctrl(s, 0);
  s->set_aec2(s, 0);
  s->set_ae_level(s, 0);
  s->set_aec_value(s, 450);
  s->set_gain_ctrl(s, 0);
  s->set_agc_gain(s, 10);
  s->set_gainceiling(s, (gainceiling_t)6);
  s->set_bpc(s, 0);
  s->set_wpc(s, 1);
  s->set_raw_gma(s, 1);
  s->set_lenc(s, 0);
  s->set_hmirror(s, 0);
  s->set_vflip(s, 1);
  s->set_dcw(s, 0);
  s->set_colorbar(s, 0);

  if(!SD_MMC.begin()){
    Serial.println("SD Card Mount Failed");
    return;
  }

  uint8_t cardType = SD_MMC.cardType();
  if(cardType == CARD_NONE){
    Serial.println("No SD Card attached");
    return;
  }

  server.on("/capture", HTTP_POST, handleCaptureRequest);

  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  if(buttonPressed == 1)
  {
    buttonPressed = 0;
    takeAndSavePhoto();
    delay(500);
  }
  server.handleClient();
}