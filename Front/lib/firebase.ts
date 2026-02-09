// Firebase Messaging helper based on the app initialized in app/lib/firebase.ts.
import {
  getMessaging,
  isSupported,
  onMessage,
  type MessagePayload,
} from "firebase/messaging";
import { firebaseApp } from "../app/lib/firebase";

let messagingPromise: Promise<ReturnType<typeof getMessaging> | null> | null = null;

async function getMessagingInstance() {
  if (!messagingPromise) {
    messagingPromise = isSupported().then((supported) =>
      supported ? getMessaging(firebaseApp) : null,
    );
  }
  return messagingPromise;
}

export async function listenForNotifications(
  callback: (payload: MessagePayload) => void,
) {
  const messaging = await getMessagingInstance();
  if (!messaging) {
    return () => undefined;
  }
  return onMessage(messaging, callback);
}
