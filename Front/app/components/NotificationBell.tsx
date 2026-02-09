import { useState, useEffect, useRef } from "react";
import { Bell } from "lucide-react";
import { getSignalements, type SignalementMapDto } from "~/lib/api";

export function NotificationBell() {
  const [hasNotif, setHasNotif] = useState(false);
  const [changedSignalements, setChangedSignalements] = useState<SignalementMapDto[]>([]);
  const [showPopup, setShowPopup] = useState(false);
  const previousRef = useRef<SignalementMapDto[]>([]);

  useEffect(() => {
    async function checkStatus() {
      const signalements = await getSignalements();
      const previous = previousRef.current;
      if (previous.length === 0) {
        previousRef.current = signalements;
        return;
      }
      // Trouver tous les signalements dont le statut a changé
      const modifs = signalements.filter(sig => {
        const prev = previous.find(s => s.id === sig.id);
        return prev && prev.statut !== sig.statut;
      });
      if (modifs.length > 0) {
        setHasNotif(true);
        setChangedSignalements(prevList => {
          // Ajoute les nouveaux signalements modifiés qui ne sont pas déjà dans la liste
          const newList = [...prevList];
          modifs.forEach(sig => {
            if (!newList.find(s => s.id === sig.id && s.statut === sig.statut)) {
              newList.push(sig);
            }
          });
          return newList;
        });
      }
      previousRef.current = signalements;
    }
    checkStatus();
    const interval = setInterval(checkStatus, 10000); // vérifie toutes les 10s
    return () => {
      clearInterval(interval);
    };
  }, []);

  const handleClick = () => {
    if (hasNotif) setShowPopup((v) => !v);
  };

  const handleClose = () => {
    setShowPopup(false);
    setHasNotif(false);
    setChangedSignalements([]);
  };

  return (
    <div className="fixed top-4 right-6 z-50">
      <div className="relative">
        <Bell className="size-8 text-muted-foreground cursor-pointer" onClick={handleClick} />
        {hasNotif && (
          <span className="absolute top-0 right-0 block h-3 w-3 rounded-full bg-red-500 border-2 border-white" />
        )}
        {showPopup && changedSignalements.length > 0 && (
          <div className="absolute right-0 mt-2 w-80 bg-white border rounded-lg shadow-lg p-4 z-50 max-h-96 overflow-y-auto">
            <div className="font-bold mb-2">Signalements modifiés ({changedSignalements.length})</div>
            {changedSignalements.map(sig => (
              <div key={sig.id} className="mb-3 border-b pb-2 last:border-b-0 last:pb-0">
                <div className="text-sm mb-1">ID : {sig.id}</div>
                <div className="text-sm mb-1">Type : <b>{sig.typeSignalement}</b></div>
                <div className="text-sm mb-1">Statut : <b>{sig.statut}</b></div>
                {sig.description && (
                  <div className="text-xs text-gray-600 mb-1">{sig.description}</div>
                )}
              </div>
            ))}
            <button className="mt-3 px-3 py-1 bg-blue-500 text-white rounded" onClick={handleClose}>Fermer</button>
          </div>
        )}
      </div>
    </div>
  );
}
