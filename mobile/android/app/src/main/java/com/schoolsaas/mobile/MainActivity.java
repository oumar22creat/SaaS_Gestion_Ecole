package com.schoolsaas.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    /**
     * Crée le canal de notification au démarrage.
     *
     * <p>Depuis Android 8, toute notification appartient à un canal, et un canal inexistant
     * la fait disparaître sans erreur. Le manifeste désigne bien un canal par défaut à
     * Firebase Messaging, mais désigner n'est pas créer : c'est à l'application de le
     * déclarer au système, et personne d'autre ne le fera.
     *
     * <p>Ici plutôt que depuis le code TypeScript, parce que le canal doit exister avant la
     * première notification reçue — y compris lorsque l'application est fermée et que c'est
     * le système qui l'affiche, sans jamais démarrer la couche web.
     *
     * <p>Créer un canal déjà créé ne coûte rien : Android ignore l'appel. En revanche il
     * ignore aussi les modifications — nom, description, importance — dès la seconde fois.
     * Ces valeurs appartiennent au parent une fois la première notification passée, et
     * c'est voulu : lui seul décide ensuite ce qui a le droit de le réveiller.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    getString(R.string.notification_channel_vie_scolaire),
                    getString(R.string.notification_channel_vie_scolaire_nom),
                    // IMPORTANCE_HIGH, et non DEFAULT : une absence signalée doit s'afficher
                    // par-dessus ce que le parent est en train de faire. C'est l'intérêt
                    // entier de la fonctionnalité — une notification qu'il faut aller
                    // chercher dans le tiroir arrive toujours trop tard.
                    NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription(getString(R.string.notification_channel_vie_scolaire_description));
            canal.enableVibration(true);
            getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }
    }
}
