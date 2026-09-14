#include <jni.h>
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <android/log.h>
#include <time.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NativeEngine", __VA_ARGS__)
static int is_running = 0;
static pthread_t ping_thread;
static int delay_seconds = 15;
static char target_host[256];
// The background C Thread function (Zero Java Garbage Collection!)
void* ping_loop(void* arg) {
struct addrinfo hints, *res;
memset(&hints, 0, sizeof(hints));
hints.ai_family = AF_INET;       // IPv4
hints.ai_socktype = SOCK_STREAM; // TCP Socket
while (is_running) {
// DNS Resolution & Socket Creation (Directly in Linux Kernel layer)
if (getaddrinfo(target_host, "443", &hints, &res) == 0) {
int sock = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
if (sock >= 0) {
// Connect timeout set to 5 seconds
struct timeval timeout;
timeout.tv_sec = 5;
timeout.tv_usec = 0;
setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));
if (connect(sock, res->ai_addr, res->ai_addrlen) == 0) {
// Send a dummy HTTP HEAD request.
// To keep it absolute lightest (no massive 3MB OpenSSL library),
// sending bytes over TCP Port 443 is enough to reset Tower Idle Timers.
const char* req = "HEAD / HTTP/1.1\r\nHost: hutch.lk\r\nConnection: close\r\n\r\n";
send(sock, req, strlen(req), 0);
LOGI("Packet sent to %s via Pure C Socket", target_host);
}
close(sock); // Memory Leak impossible here
}
freeaddrinfo(res);
}
// Sleep the thread efficiently using Linux NanoSleep (0% CPU Burn)
struct timespec ts;
ts.tv_sec = delay_seconds;
ts.tv_nsec = 0;
nanosleep(&ts, NULL);
}
return NULL;
}
// JNI Bridge: Start Engine
JNIEXPORT void JNICALL
Java_com_ping_keepalive_PingService_startNativePing(JNIEnv *env, jobject thiz, jstring host, jint delay) {
if (is_running) return;
// Convert Java String to C char array
const char *host_cstr = (*env)->GetStringUTFChars(env, host, NULL);
strncpy(target_host, host_cstr, sizeof(target_host) - 1);
(*env)->ReleaseStringUTFChars(env, host, host_cstr);
delay_seconds = delay;
is_running = 1;
// Create Background POSIX Thread
pthread_create(&ping_thread, NULL, ping_loop, NULL);
LOGI("Native ping engine started for %s with delay %d", target_host, delay);
}
// JNI Bridge: Stop Engine
JNIEXPORT void JNICALL
Java_com_ping_keepalive_PingService_stopNativePing(JNIEnv *env, jobject thiz) {
is_running = 0;
LOGI("Native ping engine stopped");
}
