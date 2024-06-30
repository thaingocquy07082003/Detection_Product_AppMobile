#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>

const char* ssid = "<3";
const char* password = "cocaidaubui09";

const char* esp32CamIP = "192.168.12.127";
int serverPort = 80;

const int buttonPin = D3; // GPIO pin connected to the button
const int buzzerPin = D2;
const int trigPin = D6;  // GPIO pin connected to TRIG pin of ultrasonic sensor
const int echoPin = D5;  // GPIO pin connected to ECHO pin of ultrasonic sensor

bool lastButtonState = HIGH; // Lưu trạng thái cuối cùng của nút
bool buttonPressed = false; // Cờ để đánh dấu nút đã được nhấn

void setup() {
  Serial.begin(115200);
  pinMode(buttonPin, INPUT_PULLUP);
  pinMode(buzzerPin, OUTPUT);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");
}

void loop() {
  // Đo khoảng cách từ cảm biến siêu âm
    long duration, distance;
    digitalWrite(trigPin, LOW); // Đặt mức thấp ở chân TRIG
    delayMicroseconds(2);
    digitalWrite(trigPin, HIGH); // Gửi xung cao trong 10 micro giây
    delayMicroseconds(10);
    digitalWrite(trigPin, LOW);

    duration = pulseIn(echoPin, HIGH); // Đọc thời gian từ chân ECHO
    distance = duration * 0.034 / 2; // Tính khoảng cách, với âm thanh đi 34cm mỗi mili giây

  // Đọc trạng thái hiện tại của nút
  bool currentButtonState = digitalRead(buttonPin);

  // Kiểm tra nếu trạng thái nút đã thay đổi từ không nhấn sang nhấn
  if (currentButtonState == LOW && lastButtonState == HIGH) {
    buttonPressed = true; // Đặt cờ nút đã được nhấn
  }

  // Cập nhật trạng thái cuối cùng của nút
  lastButtonState = currentButtonState;

  if (buttonPressed && distance <= 30 ) {
    buttonPressed = false; // Reset cờ

    digitalWrite(buzzerPin, 1000); // Bật còi
    delay(1000); // Kêu trong 0.5 giây
    digitalWrite(buzzerPin, LOW);  // Tắt còi

    // Gửi yêu cầu HTTP
    if (WiFi.status() == WL_CONNECTED) {
      HTTPClient http;
      WiFiClient client;
      String url = "http://" + String(esp32CamIP) + ":" + String(serverPort) + "/capture";
      http.begin(client, url); // Use the new API with WiFiClient and URL
      http.addHeader("Content-Type", "application/x-www-form-urlencoded");

      int httpResponseCode = http.POST("message=Capture");

      if (httpResponseCode > 0) {
        String response = http.getString();
        Serial.println(httpResponseCode);
        Serial.println(response);
      } else {
        Serial.println("Error on sending POST: " + String(httpResponseCode));
      }

      http.end();
    } else {
      Serial.println("WiFi not connected");
    }
  }

  delay(50); // Thêm một khoảng delay ngắn để debounce nút
}
