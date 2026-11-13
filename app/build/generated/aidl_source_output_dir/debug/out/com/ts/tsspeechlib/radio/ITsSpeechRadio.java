/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.ts.tsspeechlib.radio;
// Interfaccia della radio Junsun (servizio com.ts.tsspeechlib.radio.TsRadioService dell'app com.ts.MainUI).
// L'ORDINE dei metodi deve restare identico all'originale: determina i codici delle chiamate.
public interface ITsSpeechRadio extends android.os.IInterface
{
  /** Default implementation for ITsSpeechRadio. */
  public static class Default implements com.ts.tsspeechlib.radio.ITsSpeechRadio
  {
    @Override public void onPrevFreq() throws android.os.RemoteException
    {
    }
    @Override public void onNextFreq() throws android.os.RemoteException
    {
    }
    // sceglie il preferito numero N
    @Override public void onSelectedFreq(int number) throws android.os.RemoteException
    {
    }
    @Override public void openRadio() throws android.os.RemoteException
    {
    }
    @Override public void closeRadio() throws android.os.RemoteException
    {
    }
    @Override public void onRadioFM() throws android.os.RemoteException
    {
    }
    @Override public void onRadioAM() throws android.os.RemoteException
    {
    }
    @Override public int getRadioState() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void setMixVolumeSize(int size) throws android.os.RemoteException
    {
    }
    @Override public void setSoundCoexistence(int state) throws android.os.RemoteException
    {
    }
    // AM: >=4, FM: <3
    @Override public int getRadioBand() throws android.os.RemoteException
    {
      return 0;
    }
    @Override public void SeekUp() throws android.os.RemoteException
    {
    }
    @Override public void SeekDn() throws android.os.RemoteException
    {
    }
    @Override public void OpenRadioCh() throws android.os.RemoteException
    {
    }
    @Override public void CloseRadioCh() throws android.os.RemoteException
    {
    }
    // nBand: 0 FM, 4 AM
    @Override public void TurnBandAndFq(int nBand, int fq) throws android.os.RemoteException
    {
    }
    @Override public boolean bRdsOn() throws android.os.RemoteException
    {
      return false;
    }
    @Override public int GetRaidoFreq() throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.ts.tsspeechlib.radio.ITsSpeechRadio
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.ts.tsspeechlib.radio.ITsSpeechRadio interface,
     * generating a proxy if needed.
     */
    public static com.ts.tsspeechlib.radio.ITsSpeechRadio asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.ts.tsspeechlib.radio.ITsSpeechRadio))) {
        return ((com.ts.tsspeechlib.radio.ITsSpeechRadio)iin);
      }
      return new com.ts.tsspeechlib.radio.ITsSpeechRadio.Stub.Proxy(obj);
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
        case TRANSACTION_onPrevFreq:
        {
          this.onPrevFreq();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onNextFreq:
        {
          this.onNextFreq();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onSelectedFreq:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.onSelectedFreq(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_openRadio:
        {
          this.openRadio();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_closeRadio:
        {
          this.closeRadio();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onRadioFM:
        {
          this.onRadioFM();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onRadioAM:
        {
          this.onRadioAM();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getRadioState:
        {
          int _result = this.getRadioState();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setMixVolumeSize:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.setMixVolumeSize(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_setSoundCoexistence:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.setSoundCoexistence(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getRadioBand:
        {
          int _result = this.getRadioBand();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_SeekUp:
        {
          this.SeekUp();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_SeekDn:
        {
          this.SeekDn();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_OpenRadioCh:
        {
          this.OpenRadioCh();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_CloseRadioCh:
        {
          this.CloseRadioCh();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_TurnBandAndFq:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          this.TurnBandAndFq(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_bRdsOn:
        {
          boolean _result = this.bRdsOn();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_GetRaidoFreq:
        {
          int _result = this.GetRaidoFreq();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.ts.tsspeechlib.radio.ITsSpeechRadio
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
      @Override public void onPrevFreq() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPrevFreq, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onNextFreq() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onNextFreq, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      // sceglie il preferito numero N
      @Override public void onSelectedFreq(int number) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(number);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onSelectedFreq, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void openRadio() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openRadio, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void closeRadio() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_closeRadio, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onRadioFM() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onRadioFM, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onRadioAM() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onRadioAM, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public int getRadioState() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRadioState, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setMixVolumeSize(int size) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(size);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setMixVolumeSize, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void setSoundCoexistence(int state) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(state);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setSoundCoexistence, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      // AM: >=4, FM: <3
      @Override public int getRadioBand() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getRadioBand, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void SeekUp() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_SeekUp, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void SeekDn() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_SeekDn, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void OpenRadioCh() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_OpenRadioCh, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void CloseRadioCh() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_CloseRadioCh, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      // nBand: 0 FM, 4 AM
      @Override public void TurnBandAndFq(int nBand, int fq) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(nBand);
          _data.writeInt(fq);
          boolean _status = mRemote.transact(Stub.TRANSACTION_TurnBandAndFq, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean bRdsOn() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_bRdsOn, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int GetRaidoFreq() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_GetRaidoFreq, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_onPrevFreq = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onNextFreq = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onSelectedFreq = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_openRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_closeRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onRadioFM = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onRadioAM = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getRadioState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_setMixVolumeSize = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_setSoundCoexistence = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getRadioBand = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_SeekUp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_SeekDn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_OpenRadioCh = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
    static final int TRANSACTION_CloseRadioCh = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
    static final int TRANSACTION_TurnBandAndFq = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
    static final int TRANSACTION_bRdsOn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    static final int TRANSACTION_GetRaidoFreq = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
  }
  public static final java.lang.String DESCRIPTOR = "com.ts.tsspeechlib.radio.ITsSpeechRadio";
  public void onPrevFreq() throws android.os.RemoteException;
  public void onNextFreq() throws android.os.RemoteException;
  // sceglie il preferito numero N
  public void onSelectedFreq(int number) throws android.os.RemoteException;
  public void openRadio() throws android.os.RemoteException;
  public void closeRadio() throws android.os.RemoteException;
  public void onRadioFM() throws android.os.RemoteException;
  public void onRadioAM() throws android.os.RemoteException;
  public int getRadioState() throws android.os.RemoteException;
  public void setMixVolumeSize(int size) throws android.os.RemoteException;
  public void setSoundCoexistence(int state) throws android.os.RemoteException;
  // AM: >=4, FM: <3
  public int getRadioBand() throws android.os.RemoteException;
  public void SeekUp() throws android.os.RemoteException;
  public void SeekDn() throws android.os.RemoteException;
  public void OpenRadioCh() throws android.os.RemoteException;
  public void CloseRadioCh() throws android.os.RemoteException;
  // nBand: 0 FM, 4 AM
  public void TurnBandAndFq(int nBand, int fq) throws android.os.RemoteException;
  public boolean bRdsOn() throws android.os.RemoteException;
  public int GetRaidoFreq() throws android.os.RemoteException;
}
