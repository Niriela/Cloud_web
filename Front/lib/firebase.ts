// Configuration et initialisation Firebase pour FCM
import { initializeApp } from "firebase/app";
import { getMessaging, onMessage } from "firebase/messaging";

const firebaseConfig = {
  // TODO: Remplace par ta config Firebase
  apiKey: "",
  authDomain: "",
  projectId: "",
  storageBucket: "",
  messagingSenderId: "",
  appId: "",
};

const app = initializeApp(firebaseConfig);
export const messaging = getMessaging(app);

// Fonction pour écouter les notifications FCM
export function listenForNotifications(callback: (payload: any) => void) {
  onMessage(messaging, (payload) => {
    callback(payload);
  });
}
