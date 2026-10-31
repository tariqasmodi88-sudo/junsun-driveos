package com.ts.tsspeechlib.radio;

// Interfaccia della radio Junsun (servizio com.ts.tsspeechlib.radio.TsRadioService dell'app com.ts.MainUI).
// L'ORDINE dei metodi deve restare identico all'originale: determina i codici delle chiamate.
interface ITsSpeechRadio {
    void onPrevFreq();
    void onNextFreq();
    // sceglie il preferito numero N
    void onSelectedFreq(int number);
    void openRadio();
    void closeRadio();
    void onRadioFM();
    void onRadioAM();
    int getRadioState();
    void setMixVolumeSize(int size);
    void setSoundCoexistence(int state);
    // AM: >=4, FM: <3
    int getRadioBand();
    void SeekUp();
    void SeekDn();
    void OpenRadioCh();
    void CloseRadioCh();
    // nBand: 0 FM, 4 AM
    void TurnBandAndFq(int nBand, int fq);
    boolean bRdsOn();
    int GetRaidoFreq();
}
