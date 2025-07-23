#include <jni.h>
#include <errno.h>

#include <string.h>
#include <unistd.h>
#include <stdint.h>
#include <inttypes.h>
#include <iostream>
#include <fstream>
#include <stdio.h>
#include <sstream>
#include <vector>
#include <map>
#include <iomanip>
#include <thread>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/resource.h>
#include <sys/uio.h>

#include <fcntl.h>
#include <android/log.h>
#include <pthread.h>
#include <dirent.h>
#include <list>
#include <libgen.h>

#include <sys/mman.h>
#include <sys/wait.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/un.h>

#include <codecvt>
#include <chrono>
#include <queue>

#include "ImGui/imgui_internal.h"
#include "ImGui/imgui.h"
#include "ImGui/imgui_impl_android.h"
#include "ImGui/imgui_impl_opengl3.h"


#include <EGL/egl.h>
#include <GLES3/gl3.h>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <sys/system_properties.h>

//====================================
#include "ImGui/FONTS/DEFAULT.h"
//=====================================

int screenWidth = 0;
int screenHeight = 0;
bool g_Initialized = false;
ImGuiWindow* g_window = NULL;

struct sConfig {
    struct sESPMenu {
        bool Bones;
        bool Line;
        bool Box;
        bool Health;
        bool Name;
        bool Distance;
        bool TeamID;
        bool Vehicle;
		bool Radar;
        bool zj;
        bool AIMBOT;
        bool RECOIL;
    };
    sESPMenu ESPMenu{0};
	struct sRadar {
        float x;
		float y;
    };
    sRadar Radar{0};
};
sConfig Config{0};

ImColor boxColor = ImColor(225, 0, 0, 255);
ImColor lineColor = ImColor(225, 0, 0, 255);
ImColor ggColor = ImColor(225, 0, 0, 255);                       




extern "C" {
    JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_init(JNIEnv* env, jclass cls);
    JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_resize(JNIEnv* env, jobject obj, jint width, jint height);
    JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_step(JNIEnv* env, jobject obj);
	JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_imgui_Shutdown(JNIEnv* env, jobject obj);
	JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_MotionEventClick(JNIEnv* env, jobject obj,jboolean down,jfloat PosX,jfloat PosY);
	JNIEXPORT jstring JNICALL Java_com_mycompany_application_GLES3JNIView_getWindowRect(JNIEnv *env, jobject thiz);
	JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_real(JNIEnv* env, jobject obj, jint width, jint height);
};

JNIEXPORT void JNICALL
Java_com_mycompany_application_GLES3JNIView_init(JNIEnv* env, jclass cls) {

    //SetUpImGuiContext
    if(g_Initialized) return ;
	IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
	
	//Set ImGui Style
    ImGui::StyleColorsDark();
 
    // Setup Platform/Renderer backends
    ImGui_ImplAndroid_Init();
    ImGui_ImplOpenGL3_Init("#version 300 es");	
	ImGui::GetStyle().ScaleAllSizes(3.0f);
   
    g_Initialized=true;
	
	//===============================================================//
    static const ImWchar icons_ranges[] = { 0xf000, 0xf3ff, 0x0900, 0x097F, 0,};
    ImFontConfig font_config;
    ImFontConfig icons_config;
    ImFontConfig CustomFont;
    CustomFont.FontDataOwnedByAtlas = false;
    icons_config.MergeMode = true;
    icons_config.PixelSnapH = true;
    icons_config.OversampleH = 2.5;
    icons_config.OversampleV = 2.5;
    //=====================================| 𝗙𝗢𝗡𝗧𝗦
    io.Fonts->AddFontFromMemoryTTF(const_cast<std::uint8_t*>(Custom3), sizeof(Custom3), 30.f, &CustomFont);
	memset(&Config, 0, sizeof(sConfig));
	
	
	
}

JNIEXPORT void JNICALL
Java_com_mycompany_application_GLES3JNIView_resize(JNIEnv* env, jobject obj, jint width, jint height) {
	screenWidth = (int) width;
    screenHeight = (int) height;
	glViewport(0, 0, width, height);
	ImGuiIO &io = ImGui::GetIO();
    io.ConfigWindowsMoveFromTitleBarOnly = true;
    io.IniFilename = NULL;
	ImGui::GetIO().DisplaySize = ImVec2((float)width, (float)height);
}

void BeginDraw() {
	
     ImGuiIO &io = ImGui::GetIO();
    ImVec2 center = ImGui::GetMainViewport()->GetCenter();
                ImGui::SetNextWindowPos(center, ImGuiCond_Appearing, ImVec2(0.5f, 0.5f));
                ImGui::SetNextWindowSize(ImVec2(868, 562));
     if (ImGui::Begin("IMGUI_GOD×FOREVER", 0, ImGuiWindowFlags_NoSavedSettings)) {
		g_window = ImGui::GetCurrentWindow();
	
		if (ImGui::BeginTabBar("Tab", ImGuiTabBarFlags_FittingPolicyScroll)) {		
			if (ImGui::BeginTabItem("ESP MENU")) {
                ImGui::Spacing();
				ImGui::Checkbox("Box", &Config.ESPMenu.Box);
				ImGui::SameLine();
				ImGui::Checkbox("Line", &Config.ESPMenu.Line);
				ImGui::SameLine();
				ImGui::Checkbox("Skeleton", &Config.ESPMenu.Bones);
				ImGui::SameLine();
				ImGui::Checkbox("Distance", &Config.ESPMenu.Distance);
				ImGui::Checkbox("Name", &Config.ESPMenu.Name);
				ImGui::SameLine();
				ImGui::Checkbox("Health", &Config.ESPMenu.Health);
				ImGui::SameLine();
				ImGui::Checkbox("Team ID", &Config.ESPMenu.TeamID);
				ImGui::SameLine();
				ImGui::Checkbox("Radar", &Config.ESPMenu.Radar);
				ImGui::Spacing();
                ImGui::ColorEdit4("Adjust Box Color", (float*)&boxColor);
                ImGui::ColorEdit4("Adjust Line Color", (float*)&lineColor);
                ImGui::ColorEdit4("Adjust Skeleton Color", (float*)&ggColor);              
                ImGui::Spacing();
				ImGui::SliderFloat("Radar X", &Config.Radar.x, 0.0f, screenWidth , "%.2f", 1);
				ImGui::SliderFloat("Radar Y", &Config.Radar.y, 0.0f, screenHeight , "%.2f", 1);			
				ImGui::EndTabItem();           
			}
            ImGui::Separator();
            ImGui::Text("Time-consuming To Draw %.3f ms (%.1f FPS)", 1000.0f / io.Framerate, io.Framerate);         
			ImGui::EndTabBar();

        }
    }
}

JNIEXPORT void JNICALL
Java_com_mycompany_application_GLES3JNIView_step(JNIEnv* env, jobject obj) {
    
	ImGuiIO& io = ImGui::GetIO();
	
    static bool show_MainMenu_window = true;

	//Start the Dear ImGui frame
    ImGui_ImplOpenGL3_NewFrame();
    ImGui_ImplAndroid_NewFrame(screenWidth,  screenHeight);//？Settings window
    ImGui::NewFrame();       
    
	if (show_MainMenu_window) {
		BeginDraw();
	}
	
    ImGui::Render();
	glClear(GL_COLOR_BUFFER_BIT);
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
}

JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_imgui_Shutdown(JNIEnv* env, jobject obj){
    if (!g_Initialized)
        return;
     // Cleanup
    ImGui_ImplOpenGL3_Shutdown();
    ImGui_ImplAndroid_Shutdown();
    ImGui::DestroyContext();
    g_Initialized=false;
}

JNIEXPORT void JNICALL Java_com_mycompany_application_GLES3JNIView_MotionEventClick(JNIEnv* env, jobject obj,jboolean down,jfloat PosX,jfloat PosY){
	ImGuiIO & io = ImGui::GetIO();
	io.MouseDown[0] = down;
	io.MousePos = ImVec2(PosX,PosY);
}

JNIEXPORT jstring JNICALL Java_com_mycompany_application_GLES3JNIView_getWindowRect(JNIEnv *env, jobject thiz) {
    //get drawing window
    // TODO: accomplish getWindowSizePos()
    char result[256]="0|0|0|0";
    if(g_window){
        sprintf(result,"%d|%d|%d|%d",(int)g_window->Pos.x,(int)g_window->Pos.y,(int)g_window->Size.x,(int)g_window->Size.y);
    }
    return env->NewStringUTF(result);
}
