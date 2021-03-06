package bisq.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class BisqNotificationServer {

    private Logger logger = LoggerFactory.getLogger(getClass().getName());
    private ApnsClient apnsClientProduction;
    private ApnsClient apnsClientDevelopment;
    private ApnsPayloadBuilder payloadBuilder;
    public static final String IOS_BUNDLE_IDENTIFIER = "bisqremote.joachimneumann.com";
    public static final String IOS_CERTIFICATE_FILE = "push_certificate.production.p12";
    public static final String IOS_PASSWORD_FILE = "iOS_certificate_password";
    public static final String ANDROID_CERTIFICATE_FILE = "serviceAccountKey.json";
    public static final String ANDROID_DATABASE_URL = "https://bisqnotifications.firebaseio.com";

    BisqNotificationServer() {
        try {

            ClassLoader classLoader = getClass().getClassLoader();

            // ***
            // *** Android
            // ***

            InputStream resource_Android = classLoader.getResourceAsStream(ANDROID_CERTIFICATE_FILE);
            if (resource_Android == null) {
                throw new IOException(ANDROID_CERTIFICATE_FILE+" does not exist");
            } else {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(resource_Android))
                        .setDatabaseUrl(ANDROID_DATABASE_URL)
                        .build();

                FirebaseApp.initializeApp(options);
            }

            // ***
            // *** iOS
            // ***

            String password = "";
            URL password_iOS = classLoader.getResource(IOS_PASSWORD_FILE);
            if (password_iOS == null) {
                throw new IOException(IOS_PASSWORD_FILE+" does not exist");
            } else {
                File passwordFile = new File(password_iOS.getFile());
                BufferedReader br = new BufferedReader(new FileReader(passwordFile));
                password = br.readLine();
            }
            URL resource_iOS = classLoader.getResource(IOS_CERTIFICATE_FILE);
            if (resource_iOS == null) {
                throw new IOException(IOS_CERTIFICATE_FILE+" does not exist");
            } else {
                File p12File = new File(resource_iOS.getFile());
                logger.info("Using iOS certification file {}.", p12File.getAbsolutePath());
                apnsClientProduction = new ApnsClientBuilder()
                        .setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
                        .setClientCredentials(p12File, password)
                        .build();
                apnsClientDevelopment = new ApnsClientBuilder()
                        .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                        .setClientCredentials(p12File, password)
                        .build();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void overTor_____sendiOSMessage(String apsTokenHex, String encryptedMessage, Boolean sound, Boolean production, Boolean contentAvailable) {
        SimpleApnsPushNotification pushNotification;
        payloadBuilder = new ApnsPayloadBuilder();
        if (sound) payloadBuilder.setSoundFileName("default");
        payloadBuilder.setAlertBody("Bisq notification");
        if (contentAvailable) {payloadBuilder.setContentAvailable(true);}
        payloadBuilder.addCustomProperty("encrypted", encryptedMessage);
        final String payload = payloadBuilder.buildWithDefaultMaximumLength();
        logger.error("payload "+payload);
        SimpleApnsPushNotification simpleApnsPushNotification = new SimpleApnsPushNotification(apsTokenHex, IOS_BUNDLE_IDENTIFIER, payload);

        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
                sendNotificationFuture;
        if (production) {
            sendNotificationFuture = apnsClientProduction.sendNotification(simpleApnsPushNotification);
        } else {
            sendNotificationFuture = apnsClientDevelopment.sendNotification(simpleApnsPushNotification);
        }

        try {
            PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = sendNotificationFuture.get();
            if (pushNotificationResponse.isAccepted()) {
                logger.info("Push notification accepted by APNs gateway.");
            } else {
                logger.info("Notification rejected by the APNs gateway: " +
                        pushNotificationResponse.getRejectionReason());
                if (pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
                    logger.info("\t…and the token is invalid as of " +
                            pushNotificationResponse.getTokenInvalidationTimestamp());
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    public void overTor_____sendAndroidMessage(String apsTokenHex, String encryptedMessage, Boolean sound) {
        Message.Builder messageBuilder = Message.builder();

        Notification notification = new Notification("Bisq", "notification");
        messageBuilder.setNotification(notification);
        messageBuilder.putData("title", "Title");
        messageBuilder.putData("encrypted", encryptedMessage);
        messageBuilder.setToken(apsTokenHex);
        if (sound) {
            messageBuilder.putData("sound", "default");
        }

        Message message = messageBuilder.build();

        String response = null;
        try {
            FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
            response = firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
        // Response is a message ID string.
            System.out.println("Sent message: " + response);

    }
}
