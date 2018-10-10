package hu.kfg.naplo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class AuthenticatorService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        EkretaAuthenticator authenticator = new EkretaAuthenticator(this);
        return authenticator.getIBinder();
    }
}
