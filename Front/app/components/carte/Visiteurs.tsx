import { MapContainer, TileLayer, LayersControl, Marker, Popup, Tooltip } from "react-leaflet";
import * as L from "leaflet";
import type { LatLngExpression } from "leaflet";
import "leaflet/dist/leaflet.css";
import "../../carte.css";
import {
  getSignalements,
  getSignalementsStats,
  getTypeSignalements,
  getSignalementPhotos,
  type SignalementMapDto,
  type SignalementsStats,
  type TypeSignalement,
  type PhotoSignalementDto,
} from "~/lib/api";
import { useEffect, useMemo, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogTrigger,
} from "~/components/ui/modal";
import { Button } from "~/components/ui/button";
import { EmptyState } from "~/components/ui/empty-state";

const { BaseLayer } = LayersControl;

type PointType = 'travaux' | 'accident' | 'nid-de-poule' | 'repare' | 'abime' | 'alerte' | 'zone-rouge' | 'eau' | 'eft';

const colorForType = (type: PointType): string => {
    switch (type) {
        case 'travaux': return '#f39c12';
        case 'accident': return '#e74c3c';
        case 'nid-de-poule': return '#3498db';
        case 'repare': return '#2ecc71';
        case 'abime': return '#d35400';
        case 'alerte': return '#c0392b';
        case 'zone-rouge': return '#8e44ad';
        case 'eau': return '#2980b9';
        case 'eft': return '#7f8c8d';
        default: return '#2ecc71';
    }
};

const emojiForType: Record<PointType, string> = {
  travaux: '🚧',
  accident: '❗',
  'nid-de-poule': '🕳️',
  repare: '✅',
  abime: '⚠️',
  alerte: '🔥',
  'zone-rouge': '🔴',
  eau: '💧',
  eft: '❓'
};

const getIconForType = (type: PointType) => {
  const emoji = emojiForType[type] ?? '📍';
  return L.divIcon({
    className: 'custom-div-icon',
    html: `<div style="
      display:flex;align-items:center;justify-content:center;
      width:36px;height:36px;border-radius:50%;
      background:#fff;border:3px solid ${colorForType(type)};
      font-size:18px;line-height:1;">${emoji}</div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
  });
};

const typeMapping: Record<string, PointType> = {
  "en construction": "travaux",
  "accident": "accident",
  "nid de poule": "nid-de-poule",
  "réparé": "repare",
  "repare": "repare",
  "abîmé": "abime",
  "abime": "abime",
  "alerte": "alerte",
  "zone rouge": "zone-rouge",
  "fuite / eau": "eau",
  "eft": "eft",
};

const normalizeLabel = (label?: string | null) =>
  (label ?? "").trim().toLowerCase();

const resolveType = (label?: string | null): PointType => {
  const key = normalizeLabel(label);
  return typeMapping[key] ?? "travaux";
};

// Composant pour la galerie de photos
function PhotoGallery({ 
  signalementId, 
  signalementType 
}: { 
  signalementId: number; 
  signalementType: string;
}) {
  const [photos, setPhotos] = useState<PhotoSignalementDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [photoCount, setPhotoCount] = useState<number>(0);

  useEffect(() => {
    if (signalementId) {
      setIsLoading(true);
      setError(null);
      getSignalementPhotos(signalementId)
        .then((fetchedPhotos) => {
          setPhotos(fetchedPhotos);
          setPhotoCount(fetchedPhotos.length);
        })
        .catch((err) => {
          console.error("Erreur lors du chargement des photos:", err);
          setError("Impossible de charger les photos.");
          setPhotoCount(0);
        })
        .finally(() => setIsLoading(false));
    }
  }, [signalementId]);

  return (
    <div className="photo-gallery">
      <div className="mb-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">
            Photos du signalement: {signalementType}
          </h3>
          {photoCount > 0 && (
            <span className="inline-flex items-center rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-800">
              {photoCount} photo{photoCount > 1 ? 's' : ''}
            </span>
          )}
        </div>
        {error && (
          <p className="text-sm text-red-600 bg-red-50 p-2 rounded mt-2">{error}</p>
        )}
      </div>
      
      {isLoading ? (
        <div className="flex flex-col items-center justify-center h-40 space-y-3">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <p className="text-gray-500">Chargement des photos...</p>
        </div>
      ) : photoCount === 0 ? (
        <EmptyState
          title="Aucune photo disponible"
          description="Ce signalement n'a pas encore de photos associées. Les photos seront ajoutées par l'équipe de gestion."
          icon={
            <div className="mx-auto w-16 h-16 bg-white rounded-full flex items-center justify-center mb-4 border-2 border-gray-300">
              <svg 
                className="h-8 w-8 text-gray-400" 
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
              >
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  strokeWidth={1.5} 
                  d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" 
                />
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  strokeWidth={1.5} 
                  d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" 
                />
              </svg>
            </div>
          }
          action={
            <div className="flex flex-col sm:flex-row gap-3 justify-center">
              <Button variant="outline" className="gap-2">
                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Signaler l'absence de photos
              </Button>
            </div>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-h-[60vh] overflow-y-auto p-1">
          {photos.map((photo) => (
            <div key={photo.id} className="photo-item">
              <div className="border rounded-lg overflow-hidden bg-gray-50">
                <img
                  src={photo.url}
                  alt={`Photo du signalement ${signalementId}`}
                  className="w-full h-48 object-cover hover:opacity-90 transition-opacity"
                  onError={(e) => {
                    const target = e.target as HTMLImageElement;
                    target.src = "https://via.placeholder.com/300x200?text=Image+Non+Disponible";
                  }}
                />
                <div className="p-3 bg-white">
                  <p className="text-xs text-gray-500">
                    {photo.updatedAt 
                      ? new Date(photo.updatedAt).toLocaleDateString('fr-FR')
                      : 'Date inconnue'}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function Visiteurs() {
  const [signalements, setSignalements] = useState<SignalementMapDto[]>([]);
  const [typeSignalements, setTypeSignalements] = useState<TypeSignalement[]>([]);
  const [stats, setStats] = useState<SignalementsStats | null>(null);
  const [selectedStatus, setSelectedStatus] = useState("");
  const [selectedType, setSelectedType] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // États pour la galerie de photos
  const [selectedSignalementId, setSelectedSignalementId] = useState<number | null>(null);
  const [selectedSignalementType, setSelectedSignalementType] = useState<string>("");
  const [showPhotoModal, setShowPhotoModal] = useState(false);

  const openPhotoGallery = (signalementId: number, signalementType: string) => {
    setSelectedSignalementId(signalementId);
    setSelectedSignalementType(signalementType);
    setShowPhotoModal(true);
  };

  const closePhotoGallery = () => {
    setShowPhotoModal(false);
    setSelectedSignalementId(null);
    setSelectedSignalementType("");
  };

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    const filters = {
      status: selectedStatus || undefined,
      type: selectedType || undefined,
    };
    Promise.all([
      getSignalements(filters),
      getTypeSignalements(),
      getSignalementsStats(filters),
    ])
      .then(([signalementsData, typesData, statsData]) => {
        if (!active) return;
        setSignalements(signalementsData);
        setTypeSignalements(typesData);
        setStats(statsData);
      })
      .catch(() => {
        if (!active) return;
        setError("Impossible de charger les signalements.");
      })
      .finally(() => {
        if (!active) return;
        setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [selectedStatus, selectedType]);

  const points = useMemo(
    () =>
      signalements.filter(
        (item) =>
          typeof item.latitude === "number" &&
          typeof item.longitude === "number",
      ),
    [signalements],
  );

  const pointTypes: Array<{ type: PointType; label: string }> =
    typeSignalements.map((item) => ({
      type: resolveType(item.libelle),
      label: item.libelle,
    }));

  const statusOptions = useMemo(() => {
    const values = new Set<string>();
    signalements.forEach((item) => {
      if (item.statut) {
        values.add(item.statut);
      }
    });
    return Array.from(values).sort((a, b) => a.localeCompare(b));
  }, [signalements]);

  return (
    <div className="flex h-screen w-full overflow-hidden gap-4 p-4">
      {/* Modal pour les photos */}
      <Dialog open={showPhotoModal} onOpenChange={setShowPhotoModal}>
        <DialogContent className="sm:max-w-4xl max-h-[80vh] overflow-hidden">
          <DialogHeader>
            <DialogTitle>Photos du signalement</DialogTitle>
            <DialogDescription>
              Visualisez les photos associées à ce signalement
            </DialogDescription>
          </DialogHeader>
          {selectedSignalementId && (
            <PhotoGallery 
              signalementId={selectedSignalementId} 
              signalementType={selectedSignalementType}
            />
          )}
          <div className="flex justify-end pt-4 border-t">
            <Button variant="outline" onClick={closePhotoGallery}>
              Fermer
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Carte - 75% */}
      <div className="flex-1 rounded-lg overflow-hidden bg-gray-100">
        <MapContainer
          center={[-18.879, 47.507] as LatLngExpression}
          zoom={13}
          style={{ height: '100%', width: '100%' }}
        >
          <LayersControl position="topright">
            <BaseLayer checked name="OpenStreetMap">
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            </BaseLayer>
            <BaseLayer name="Satellite">
              <TileLayer url="https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png" />
            </BaseLayer>
          </LayersControl>

          {points.map((p) => (
            <Marker
              key={p.id}
              position={[p.latitude as number, p.longitude as number]}
              icon={getIconForType(resolveType(p.typeSignalement))}
            >
              <Tooltip direction="top" offset={[0, -10]} opacity={0.9} sticky>
                <div className="text-xs min-w-[200px]">
                  <div className="font-semibold">{p.typeSignalement ?? "Signalement"}</div>
                  <div className="mt-1 space-y-1">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Status:</span>
                      <span>{p.statut ?? "-"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Surface:</span>
                      <span>{p.surface ?? "-"} m²</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Budget:</span>
                      <span>{p.budget ?? "-"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Entreprise:</span>
                      <span>{p.entreprise ?? "-"}</span>
                    </div>
                  </div>
                  <div className="mt-2 pt-2 border-t">
                    <button
                      onClick={() => openPhotoGallery(p.id, p.typeSignalement || "Signalement")}
                      className="w-full text-left text-blue-600 hover:text-blue-800 hover:underline text-xs font-medium flex items-center justify-between gap-1 group"
                    >
                      <div className="flex items-center gap-1">
                        <span className="text-sm group-hover:scale-110 transition-transform">📸</span>
                        <span>Voir les photos</span>
                      </div>
                      <span className="bg-blue-100 text-blue-800 text-xs font-medium px-2 py-0.5 rounded-full">
                        ?
                      </span>
                    </button>
                  </div>
                </div>
              </Tooltip>
              <Popup>
                <div className="popup-content">
                  <strong className="text-base">{p.typeSignalement ?? "Signalement"}</strong>
                  <div className="mt-2 space-y-1 text-sm">
                    <div><span className="font-medium">Date:</span> {p.date ?? "-"}</div>
                    <div><span className="font-medium">Statut:</span> {p.statut ?? "-"}</div>
                    <div><span className="font-medium">Surface:</span> {p.surface ?? "-"} m²</div>
                    <div><span className="font-medium">Budget:</span> {p.budget ?? "-"}</div>
                    <div><span className="font-medium">Entreprise:</span> {p.entreprise ?? "-"}</div>
                  </div>
                  <div className="mt-3 pt-3 border-t">
                    <button
                      onClick={() => openPhotoGallery(p.id, p.typeSignalement || "Signalement")}
                      className="w-full py-2 px-4 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded-md text-sm font-medium flex items-center justify-between transition-colors group"
                    >
                      <div className="flex items-center gap-2">
                        <span className="text-base group-hover:scale-110 transition-transform">📸</span>
                        <span>Voir les photos</span>
                      </div>
                      <span className="bg-blue-100 text-blue-800 text-xs font-medium px-2 py-1 rounded-full">
                        ?
                      </span>
                    </button>
                  </div>
                </div>
              </Popup>
            </Marker>
          ))} 
        </MapContainer>
      </div>

      {/* Légende - 25% */}
      <div className="w-1/4 bg-white rounded-lg shadow-lg p-4 overflow-y-auto">
        <h3 className="font-semibold text-sm mb-4 text-gray-800">Légende</h3>
        {isLoading ? (
          <p className="text-xs text-gray-500">Chargement...</p>
        ) : null}
        {error ? (
          <p className="text-xs text-red-600">{error}</p>
        ) : null}
        <div className="mb-4 space-y-2">
          <div>
            <label className="text-xs font-medium text-gray-700">Statut</label>
            <select
              className="mt-1 w-full rounded-md border border-gray-200 bg-white p-2 text-xs"
              value={selectedStatus}
              onChange={(event) => setSelectedStatus(event.target.value)}
            >
              <option value="">Tous</option>
              {statusOptions.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Type</label>
            <select
              className="mt-1 w-full rounded-md border border-gray-200 bg-white p-2 text-xs"
              value={selectedType}
              onChange={(event) => setSelectedType(event.target.value)}
            >
              <option value="">Tous</option>
              {typeSignalements.map((type) => (
                <option key={type.id} value={type.libelle}>
                  {type.libelle}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="mb-6 rounded-md border border-gray-200 bg-gray-50 p-3">
          <p className="text-xs font-semibold text-gray-700 mb-2">Récapitulatif</p>
          <div className="space-y-1 text-xs text-gray-700">
            <div>Nombre de points: {stats?.totalPoints ?? 0}</div>
            <div>Surface totale: {stats?.totalSurface ?? 0} m²</div>
            <div>Budget total: {stats?.totalBudget ?? 0}</div>
            <div>Avancement: {stats ? stats.advancementPercent.toFixed(1) : 0}%</div>
          </div>
        </div>
        <div className="space-y-3">
          {pointTypes.map((item) => (
            <div key={item.type} className="flex items-center gap-2">
              <div
                className="flex-shrink-0 flex items-center justify-center w-7 h-7 rounded-full text-base"
                style={{
                  backgroundColor: '#fff',
                  border: `3px solid ${colorForType(item.type)}`,
                }}
              >
                {emojiForType[item.type]}
              </div>
              <span className="text-xs text-gray-700">{item.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}