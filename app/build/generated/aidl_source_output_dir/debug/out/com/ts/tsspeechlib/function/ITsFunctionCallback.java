/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.ts.tsspeechlib.function;
public interface ITsFunctionCallback extends android.os.IInterface
{
  /** Default implementation for ITsFunctionCallback. */
  public static class Default implements com.ts.tsspeechlib.function.ITsFunctionCallback
  {
    @Override public void isSupportFastCharging(boolean isSupport) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.ts.tsspeechlib.function.ITsFunctionCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.ts.tsspeechlib.function.ITsFunctionCallback interface,
     * generating a proxy if needed.
     */
    public static com.ts.tsspeechlib.function.ITsFunctionCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.ts.tsspeechlib.function.ITsFunctionCallback))) {
        return ((com.ts.tsspeechlib.function.ITsFunctionCallback)iin);
      }
      return new com.ts.tsspeechlib.function.ITsFunctionCallback.Stub.Proxy(obj);
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
        case TRANSACTION_isSupportFastCharging:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.isSupportFastCharging(_arg0);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.ts.tsspeechlib.function.ITsFunctionCallback
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
      @Override public void isSupportFastCharging(boolean isSupport) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((isSupport)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_isSupportFastCharging, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_isSupportFastCharging = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
  }
  public static final java.lang.String DESCRIPTOR = "com.ts.tsspeechlib.function.ITsFunctionCallback";
  public void isSupportFastCharging(boolean isSupport) throws android.os.RemoteException;
}
