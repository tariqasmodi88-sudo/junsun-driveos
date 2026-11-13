/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.ts.tsspeechlib.function;
// Funzioni della radio Junsun (servizio com.ts.tsspeechlib.function.TsFunctionService di com.ts.MainUI).
// L'ORDINE dei metodi deve restare identico all'originale.
public interface ITsSpeechFunction extends android.os.IInterface
{
  /** Default implementation for ITsSpeechFunction. */
  public static class Default implements com.ts.tsspeechlib.function.ITsSpeechFunction
  {
    @Override public boolean isScreenOpen() throws android.os.RemoteException
    {
      return false;
    }
    @Override public void onOpenScreen() throws android.os.RemoteException
    {
    }
    @Override public void onCloseScreen() throws android.os.RemoteException
    {
    }
    @Override public int getCurrentBrightness() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getMinBrightnes() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getMaxBrightnes() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void onBrightenScreen() throws android.os.RemoteException
    {
    }
    @Override public void onDimmingScreen() throws android.os.RemoteException
    {
    }
    @Override public void onScreenBrightest() throws android.os.RemoteException
    {
    }
    @Override public void onScreenDarkest() throws android.os.RemoteException
    {
    }
    @Override public int getCurrentVolume() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getMinVolume() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int getMaxVolume() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void onVolumeUp() throws android.os.RemoteException
    {
    }
    @Override public void onVolumeDown() throws android.os.RemoteException
    {
    }
    @Override public void onVolumeMax() throws android.os.RemoteException
    {
    }
    @Override public void onVolumeMin() throws android.os.RemoteException
    {
    }
    @Override public boolean isVolumeMute() throws android.os.RemoteException
    {
      return false;
    }
    @Override public void onVolumeMute() throws android.os.RemoteException
    {
    }
    @Override public void onVolumeUnmute() throws android.os.RemoteException
    {
    }
    @Override public void showLauncher() throws android.os.RemoteException
    {
    }
    // 0 nessuna, 1 radio, 4 musica, 5 musica bluetooth
    @Override public int getWorkMode() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void openSetting() throws android.os.RemoteException
    {
    }
    @Override public void openAvin() throws android.os.RemoteException
    {
    }
    @Override public void openCarInfo() throws android.os.RemoteException
    {
    }
    @Override public void speechStartTTS() throws android.os.RemoteException
    {
    }
    @Override public void speechStopTTS() throws android.os.RemoteException
    {
    }
    @Override public void speechStartRecognition() throws android.os.RemoteException
    {
    }
    @Override public void speechStopRecognition() throws android.os.RemoteException
    {
    }
    @Override public void setVolume(int number) throws android.os.RemoteException
    {
    }
    @Override public void setScreenBrightness(int number) throws android.os.RemoteException
    {
    }
    @Override public void supportFastCharging(int state) throws android.os.RemoteException
    {
    }
    @Override public void setFunctionCallback(com.ts.tsspeechlib.function.ITsFunctionCallback callback) throws android.os.RemoteException
    {
    }
    // 0 spento, 1 acceso, -1 errore
    @Override public int isIllOn() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int isBackCar() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int withBrakes() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public java.lang.String getDeviceType() throws android.os.RemoteException
    {
      return null;
    }
    @Override public java.lang.String getHMI() throws android.os.RemoteException
    {
      return null;
    }
    @Override public java.lang.String getBranch() throws android.os.RemoteException
    {
      return null;
    }
    @Override public int GetRadioIC() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int GetCanType() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int GetCameraType() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void SendKey(int KeyCode) throws android.os.RemoteException
    {
    }
    @Override public void KillProcess(java.lang.String pname) throws android.os.RemoteException
    {
    }
    @Override public void OpenMainUIApp(int nWin, int parat) throws android.os.RemoteException
    {
    }
    @Override public void CloseMainUIApp(int nWin) throws android.os.RemoteException
    {
    }
    @Override public java.lang.String GetDeviceID() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void TurnOnWifi(boolean bturn) throws android.os.RemoteException
    {
    }
    @Override public java.lang.String GetIccid() throws android.os.RemoteException
    {
      return null;
    }
    @Override public java.lang.String GetImei() throws android.os.RemoteException
    {
      return null;
    }
    @Override public void SetVideoChannel(int nCh) throws android.os.RemoteException
    {
    }
    @Override public void DeleteAllKey() throws android.os.RemoteException
    {
    }
    @Override public void StartKeyStudy(int nKey) throws android.os.RemoteException
    {
    }
    @Override public boolean KeyStudyOk(int nKey) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean bSignalOK() throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.ts.tsspeechlib.function.ITsSpeechFunction
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.ts.tsspeechlib.function.ITsSpeechFunction interface,
     * generating a proxy if needed.
     */
    public static com.ts.tsspeechlib.function.ITsSpeechFunction asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.ts.tsspeechlib.function.ITsSpeechFunction))) {
        return ((com.ts.tsspeechlib.function.ITsSpeechFunction)iin);
      }
      return new com.ts.tsspeechlib.function.ITsSpeechFunction.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_isScreenOpen:
        {
          boolean _result = this.isScreenOpen();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_onOpenScreen:
        {
          this.onOpenScreen();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onCloseScreen:
        {
          this.onCloseScreen();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getCurrentBrightness:
        {
          int _result = this.getCurrentBrightness();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getMinBrightnes:
        {
          int _result = this.getMinBrightnes();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getMaxBrightnes:
        {
          int _result = this.getMaxBrightnes();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_onBrightenScreen:
        {
          this.onBrightenScreen();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onDimmingScreen:
        {
          this.onDimmingScreen();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onScreenBrightest:
        {
          this.onScreenBrightest();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onScreenDarkest:
        {
          this.onScreenDarkest();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getCurrentVolume:
        {
          int _result = this.getCurrentVolume();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getMinVolume:
        {
          int _result = this.getMinVolume();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getMaxVolume:
        {
          int _result = this.getMaxVolume();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_onVolumeUp:
        {
          this.onVolumeUp();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onVolumeDown:
        {
          this.onVolumeDown();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onVolumeMax:
        {
          this.onVolumeMax();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onVolumeMin:
        {
          this.onVolumeMin();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isVolumeMute:
        {
          boolean _result = this.isVolumeMute();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_onVolumeMute:
        {
          this.onVolumeMute();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onVolumeUnmute:
        {
          this.onVolumeUnmute();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_showLauncher:
        {
          this.showLauncher();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getWorkMode:
        {
          int _result = this.getWorkMode();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_openSetting:
        {
          this.openSetting();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_openAvin:
        {
          this.openAvin();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_openCarInfo:
        {
          this.openCarInfo();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_speechStartTTS:
        {
          this.speechStartTTS();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_speechStopTTS:
        {
          this.speechStopTTS();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_speechStartRecognition:
        {
          this.speechStartRecognition();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_speechStopRecognition:
        {
          this.speechStopRecognition();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setVolume:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.setVolume(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setScreenBrightness:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.setScreenBrightness(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_supportFastCharging:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.supportFastCharging(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setFunctionCallback:
        {
          com.ts.tsspeechlib.function.ITsFunctionCallback _arg0;
          _arg0 = com.ts.tsspeechlib.function.ITsFunctionCallback.Stub.asInterface(data.readStrongBinder());
          this.setFunctionCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isIllOn:
        {
          int _result = this.isIllOn();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_isBackCar:
        {
          int _result = this.isBackCar();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_withBrakes:
        {
          int _result = this.withBrakes();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_getDeviceType:
        {
          java.lang.String _result = this.getDeviceType();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_getHMI:
        {
          java.lang.String _result = this.getHMI();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_getBranch:
        {
          java.lang.String _result = this.getBranch();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_GetRadioIC:
        {
          int _result = this.GetRadioIC();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_GetCanType:
        {
          int _result = this.GetCanType();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_GetCameraType:
        {
          int _result = this.GetCameraType();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_SendKey:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.SendKey(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_KillProcess:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.KillProcess(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_OpenMainUIApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          this.OpenMainUIApp(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_CloseMainUIApp:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.CloseMainUIApp(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_GetDeviceID:
        {
          java.lang.String _result = this.GetDeviceID();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_TurnOnWifi:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.TurnOnWifi(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_GetIccid:
        {
          java.lang.String _result = this.GetIccid();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_GetImei:
        {
          java.lang.String _result = this.GetImei();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_SetVideoChannel:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.SetVideoChannel(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_DeleteAllKey:
        {
          this.DeleteAllKey();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_StartKeyStudy:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.StartKeyStudy(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_KeyStudyOk:
        {
          int _arg0;
          _arg0 = data.readInt();
          boolean _result = this.KeyStudyOk(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_bSignalOK:
        {
          boolean _result = this.bSignalOK();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.ts.tsspeechlib.function.ITsSpeechFunction
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public boolean isScreenOpen() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isScreenOpen, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void onOpenScreen() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onOpenScreen, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onCloseScreen() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onCloseScreen, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getCurrentBrightness() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCurrentBrightness, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getMinBrightnes() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMinBrightnes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getMaxBrightnes() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMaxBrightnes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void onBrightenScreen() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onBrightenScreen, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onDimmingScreen() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDimmingScreen, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onScreenBrightest() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onScreenBrightest, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onScreenDarkest() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onScreenDarkest, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getCurrentVolume() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getCurrentVolume, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getMinVolume() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMinVolume, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int getMaxVolume() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getMaxVolume, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void onVolumeUp() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVolumeUp, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onVolumeDown() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVolumeDown, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onVolumeMax() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVolumeMax, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onVolumeMin() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVolumeMin, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isVolumeMute() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isVolumeMute, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void onVolumeMute() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVolumeMute, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onVolumeUnmute() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onVolumeUnmute, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void showLauncher() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_showLauncher, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      // 0 nessuna, 1 radio, 4 musica, 5 musica bluetooth
      @Override public int getWorkMode() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getWorkMode, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void openSetting() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openSetting, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void openAvin() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openAvin, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void openCarInfo() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openCarInfo, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void speechStartTTS() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_speechStartTTS, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void speechStopTTS() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_speechStopTTS, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void speechStartRecognition() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_speechStartRecognition, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void speechStopRecognition() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_speechStopRecognition, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setVolume(int number) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(number);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setVolume, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setScreenBrightness(int number) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(number);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setScreenBrightness, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void supportFastCharging(int state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_supportFastCharging, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setFunctionCallback(com.ts.tsspeechlib.function.ITsFunctionCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setFunctionCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      // 0 spento, 1 acceso, -1 errore
      @Override public int isIllOn() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isIllOn, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int isBackCar() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isBackCar, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int withBrakes() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_withBrakes, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String getDeviceType() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getDeviceType, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String getHMI() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHMI, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String getBranch() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getBranch, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int GetRadioIC() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_GetRadioIC, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int GetCanType() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_GetCanType, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int GetCameraType() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_GetCameraType, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void SendKey(int KeyCode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(KeyCode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_SendKey, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void KillProcess(java.lang.String pname) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(pname);
          boolean _status = mRemote.transact(Stub.TRANSACTION_KillProcess, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void OpenMainUIApp(int nWin, int parat) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(nWin);
          _data.writeInt(parat);
          boolean _status = mRemote.transact(Stub.TRANSACTION_OpenMainUIApp, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void CloseMainUIApp(int nWin) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(nWin);
          boolean _status = mRemote.transact(Stub.TRANSACTION_CloseMainUIApp, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public java.lang.String GetDeviceID() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_GetDeviceID, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void TurnOnWifi(boolean bturn) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((bturn)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_TurnOnWifi, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public java.lang.String GetIccid() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_GetIccid, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String GetImei() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_GetImei, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void SetVideoChannel(int nCh) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(nCh);
          boolean _status = mRemote.transact(Stub.TRANSACTION_SetVideoChannel, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void DeleteAllKey() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_DeleteAllKey, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void StartKeyStudy(int nKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(nKey);
          boolean _status = mRemote.transact(Stub.TRANSACTION_StartKeyStudy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean KeyStudyOk(int nKey) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(nKey);
          boolean _status = mRemote.transact(Stub.TRANSACTION_KeyStudyOk, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean bSignalOK() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_bSignalOK, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_isScreenOpen = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onOpenScreen = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onCloseScreen = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_getCurrentBrightness = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_getMinBrightnes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_getMaxBrightnes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onBrightenScreen = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onDimmingScreen = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_onScreenBrightest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onScreenDarkest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getCurrentVolume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_getMinVolume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_getMaxVolume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_onVolumeUp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_onVolumeDown = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_onVolumeMax = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_onVolumeMin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_isVolumeMute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    static final int TRANSACTION_onVolumeMute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
    static final int TRANSACTION_onVolumeUnmute = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
    static final int TRANSACTION_showLauncher = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
    static final int TRANSACTION_getWorkMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
    static final int TRANSACTION_openSetting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
    static final int TRANSACTION_openAvin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
    static final int TRANSACTION_openCarInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
    static final int TRANSACTION_speechStartTTS = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
    static final int TRANSACTION_speechStopTTS = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
    static final int TRANSACTION_speechStartRecognition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
    static final int TRANSACTION_speechStopRecognition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
    static final int TRANSACTION_setVolume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
    static final int TRANSACTION_setScreenBrightness = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
    static final int TRANSACTION_supportFastCharging = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
    static final int TRANSACTION_setFunctionCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
    static final int TRANSACTION_isIllOn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
    static final int TRANSACTION_isBackCar = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
    static final int TRANSACTION_withBrakes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
    static final int TRANSACTION_getDeviceType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
    static final int TRANSACTION_getHMI = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
    static final int TRANSACTION_getBranch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
    static final int TRANSACTION_GetRadioIC = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
    static final int TRANSACTION_GetCanType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
    static final int TRANSACTION_GetCameraType = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
    static final int TRANSACTION_SendKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 42);
    static final int TRANSACTION_KillProcess = (android.os.IBinder.FIRST_CALL_TRANSACTION + 43);
    static final int TRANSACTION_OpenMainUIApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 44);
    static final int TRANSACTION_CloseMainUIApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 45);
    static final int TRANSACTION_GetDeviceID = (android.os.IBinder.FIRST_CALL_TRANSACTION + 46);
    static final int TRANSACTION_TurnOnWifi = (android.os.IBinder.FIRST_CALL_TRANSACTION + 47);
    static final int TRANSACTION_GetIccid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 48);
    static final int TRANSACTION_GetImei = (android.os.IBinder.FIRST_CALL_TRANSACTION + 49);
    static final int TRANSACTION_SetVideoChannel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 50);
    static final int TRANSACTION_DeleteAllKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 51);
    static final int TRANSACTION_StartKeyStudy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 52);
    static final int TRANSACTION_KeyStudyOk = (android.os.IBinder.FIRST_CALL_TRANSACTION + 53);
    static final int TRANSACTION_bSignalOK = (android.os.IBinder.FIRST_CALL_TRANSACTION + 54);
  }
  public static final java.lang.String DESCRIPTOR = "com.ts.tsspeechlib.function.ITsSpeechFunction";
  public boolean isScreenOpen() throws android.os.RemoteException;
  public void onOpenScreen() throws android.os.RemoteException;
  public void onCloseScreen() throws android.os.RemoteException;
  public int getCurrentBrightness() throws android.os.RemoteException;
  public int getMinBrightnes() throws android.os.RemoteException;
  public int getMaxBrightnes() throws android.os.RemoteException;
  public void onBrightenScreen() throws android.os.RemoteException;
  public void onDimmingScreen() throws android.os.RemoteException;
  public void onScreenBrightest() throws android.os.RemoteException;
  public void onScreenDarkest() throws android.os.RemoteException;
  public int getCurrentVolume() throws android.os.RemoteException;
  public int getMinVolume() throws android.os.RemoteException;
  public int getMaxVolume() throws android.os.RemoteException;
  public void onVolumeUp() throws android.os.RemoteException;
  public void onVolumeDown() throws android.os.RemoteException;
  public void onVolumeMax() throws android.os.RemoteException;
  public void onVolumeMin() throws android.os.RemoteException;
  public boolean isVolumeMute() throws android.os.RemoteException;
  public void onVolumeMute() throws android.os.RemoteException;
  public void onVolumeUnmute() throws android.os.RemoteException;
  public void showLauncher() throws android.os.RemoteException;
  // 0 nessuna, 1 radio, 4 musica, 5 musica bluetooth
  public int getWorkMode() throws android.os.RemoteException;
  public void openSetting() throws android.os.RemoteException;
  public void openAvin() throws android.os.RemoteException;
  public void openCarInfo() throws android.os.RemoteException;
  public void speechStartTTS() throws android.os.RemoteException;
  public void speechStopTTS() throws android.os.RemoteException;
  public void speechStartRecognition() throws android.os.RemoteException;
  public void speechStopRecognition() throws android.os.RemoteException;
  public void setVolume(int number) throws android.os.RemoteException;
  public void setScreenBrightness(int number) throws android.os.RemoteException;
  public void supportFastCharging(int state) throws android.os.RemoteException;
  public void setFunctionCallback(com.ts.tsspeechlib.function.ITsFunctionCallback callback) throws android.os.RemoteException;
  // 0 spento, 1 acceso, -1 errore
  public int isIllOn() throws android.os.RemoteException;
  public int isBackCar() throws android.os.RemoteException;
  public int withBrakes() throws android.os.RemoteException;
  public java.lang.String getDeviceType() throws android.os.RemoteException;
  public java.lang.String getHMI() throws android.os.RemoteException;
  public java.lang.String getBranch() throws android.os.RemoteException;
  public int GetRadioIC() throws android.os.RemoteException;
  public int GetCanType() throws android.os.RemoteException;
  public int GetCameraType() throws android.os.RemoteException;
  public void SendKey(int KeyCode) throws android.os.RemoteException;
  public void KillProcess(java.lang.String pname) throws android.os.RemoteException;
  public void OpenMainUIApp(int nWin, int parat) throws android.os.RemoteException;
  public void CloseMainUIApp(int nWin) throws android.os.RemoteException;
  public java.lang.String GetDeviceID() throws android.os.RemoteException;
  public void TurnOnWifi(boolean bturn) throws android.os.RemoteException;
  public java.lang.String GetIccid() throws android.os.RemoteException;
  public java.lang.String GetImei() throws android.os.RemoteException;
  public void SetVideoChannel(int nCh) throws android.os.RemoteException;
  public void DeleteAllKey() throws android.os.RemoteException;
  public void StartKeyStudy(int nKey) throws android.os.RemoteException;
  public boolean KeyStudyOk(int nKey) throws android.os.RemoteException;
  public boolean bSignalOK() throws android.os.RemoteException;
}
