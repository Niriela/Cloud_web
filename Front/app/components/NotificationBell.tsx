
import { useState, useEffect } from "react";
import { Bell } from "lucide-react";
import { getFirestoreSignalements } from "~/lib/firestore-data";

export function NotificationBell() {
  const [hasNotif, setHasNotif] = useState(false);
  const [changedSignalement, setChangedSignalement] = useState<any | null>(null);
  const [showPopup, setShowPopup] = useState(false);
  const [prevSignalements, setPrevSignalements] = useState<any[]>([]);

  useEffect(() => {
    let mounted = true;
    async function checkStatus() {
      const signalements = await getFirestoreSignalements();
      if (prevSignalements.length === 0) {
        setPrevSignalements(signalements);
        return;
      }
      // Compare chaque signalement par id et statut
      for (let i = 0; i < signalements.length; i++) {
        const prev = prevSignalements.find(s => s.id === signalements[i].id);
        if (prev && prev.statut !== signalements[i].statut) {
          setHasNotif(true);
          setChangedSignalement(signalements[i]);
          break;
        }
      }
      setPrevSignalements(signalements);
    }
    checkStatus();
    const interval = setInterval(checkStatus, 10000); // vérifie toutes les 10s
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, [prevSignalements]);

  const handleClick = () => {
    if (hasNotif) setShowPopup((v) => !v);
  };

  return (
    <div className="fixed top-4 right-6 z-50">
      <div className="relative">
        <Bell className="size-8 text-muted-foreground cursor-pointer" onClick={handleClick} />
        {hasNotif && (
          <span className="absolute top-0 right-0 block h-3 w-3 rounded-full bg-red-500 border-2 border-white" />
        )}
        {showPopup && changedSignalement && (
          <div className="absolute right-0 mt-2 w-72 bg-white border rounded-lg shadow-lg p-4 z-50">
            <div className="font-bold mb-2">Signalement modifié</div>
            <div className="text-sm mb-1">ID : {changedSignalement.id}</div>
            <div className="text-sm mb-1">Type : <b>{changedSignalement.typeSignalement}</b></div>
            <div className="text-sm mb-1">Statut : <b>{changedSignalement.statut}</b></div>
            {changedSignalement.description && (
              <div className="text-xs text-gray-600 mb-1">{changedSignalement.description}</div>
            )}
            <button className="mt-3 px-3 py-1 bg-blue-500 text-white rounded" onClick={() => setShowPopup(false)}>Fermer</button>
          </div>
        )}
      </div>
    </div>
  );
}
