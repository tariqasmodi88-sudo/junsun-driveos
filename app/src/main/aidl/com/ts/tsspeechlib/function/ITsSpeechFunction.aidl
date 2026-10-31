package com.ts.tsspeechlib.function;

import com.ts.tsspeechlib.function.ITsFunctionCallback;

// Funzioni della radio Junsun (servizio com.ts.tsspeechlib.function.TsFunctionService di com.ts.MainUI).
// L'ORDINE dei metodi deve restare identico all'originale.
interface ITsSpeechFunction {
    boolean isScreenOpen();
    void onOpenScreen();
    void onCloseScreen();
    int getCurrentBrightness();
    int getMinBrightnes();
    int getMaxBrightnes();
    void onBrightenScreen();
    void onDimmingScreen();
    void onScreenBrightest();
    void onScreenDarkest();
    int getCurrentVolume();
    int getMinVolume();
    int getMaxVolume();
    void onVolumeUp();
    void onVolumeDown();
    void onVolumeMax();
    void onVolumeMin();
    boolean isVolumeMute();
    void onVolumeMute();
    void onVolumeUnmute();
    void showLauncher();
    // 0 nessuna, 1 radio, 4 musica, 5 musica bluetooth
    int getWorkMode();
    void openSetting();
    void openAvin();
    void openCarInfo();
    void speechStartTTS();
    void speechStopTTS();
    void speechStartRecognition();
    void speechStopRecognition();
    void setVolume(int number);
    void setScreenBrightness(int number);
    void supportFastCharging(int state);
    void setFunctionCallback(ITsFunctionCallback callback);
    // 0 spento, 1 acceso, -1 errore
    int isIllOn();
    int isBackCar();
    int withBrakes();
    String getDeviceType();
    String getHMI();
    String getBranch();
    int GetRadioIC();
    int GetCanType();
    int GetCameraType();
    void SendKey(int KeyCode);
    void KillProcess(String pname);
    void OpenMainUIApp(int nWin, int parat);
    void CloseMainUIApp(int nWin);
    String GetDeviceID();
    void TurnOnWifi(boolean bturn);
    String GetIccid();
    String GetImei();
    void SetVideoChannel(int nCh);
    void DeleteAllKey();
    void StartKeyStudy(int nKey);
    boolean KeyStudyOk(int nKey);
    boolean bSignalOK();
}
