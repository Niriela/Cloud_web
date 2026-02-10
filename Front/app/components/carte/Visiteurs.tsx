import { MapContainer, TileLayer, LayersControl, Marker, Popup, Tooltip } from "react-leaflet";
import * as L from "leaflet";
import type { LatLngExpression } from "leaflet";
import "leaflet/dist/leaflet.css";
import "../../carte.css";
import {
  getSignalements,
  getSignalementsStats,
  getTypeSignalements,
  type SignalementMapDto,
  type SignalementsStats,
  type TypeSignalement,
} from "~/lib/api";
import { useEffect, useMemo, useState } from "react";

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

const normalizePercent = (value?: number | null) => {
  if (typeof value !== "number" || Number.isNaN(value)) {
    return 0;
  }
  return Math.max(0, Math.min(100, value));
};

const resolveType = (label?: string | null): PointType => {
  const key = normalizeLabel(label);
  return typeMapping[key] ?? "travaux";
};

export default function Visiteurs() {
  const [signalements, setSignalements] = useState<SignalementMapDto[]>([]);
  const [typeSignalements, setTypeSignalements] = useState<TypeSignalement[]>([]);
  const [stats, setStats] = useState<SignalementsStats | null>(null);
  const [selectedStatus, setSelectedStatus] = useState("");
  const [selectedType, setSelectedType] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  const advancementPercent = normalizePercent(stats?.advancementPercent ?? 0);

  return (
    <div className="flex h-screen w-full overflow-hidden gap-4 p-4">
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
                <div className="text-xs">
                  <div className="font-semibold">{p.typeSignalement ?? "Signalement"}</div>
                  <div>Status: {p.statut ?? "-"}</div>
                  <div>Surface: {p.surface ?? "-"} m²</div>
                  <div>Budget: {p.budget ?? "-"}</div>
                  <div>Entreprise: {p.entreprise ?? "-"}</div>
                  {/* Ajouter cette ligne pour le lien Voir les photos */}
                  <div className="mt-1">
                    <a 
                      href={`/signalements/${p.id}/photos`} 
                      className="text-blue-600 hover:underline text-xs"
                      onClick={(e) => e.stopPropagation()} // Empêche la fermeture du tooltip
                    >
                      📸 Voir les photos
                    </a>
                  </div>
                </div>
              </Tooltip>
              <Popup>
                <strong>{p.typeSignalement ?? "Signalement"}</strong>
                <br />
                Date: {p.date ?? "-"}
                <br />
                Statut: {p.statut ?? "-"}
                <br />
                Surface: {p.surface ?? "-"} m²
                <br />
                Budget: {p.budget ?? "-"}
                <br />
                Entreprise: {p.entreprise ?? "-"}
                {/* Ajouter cette ligne pour le lien Voir les photos */}
                <br />
                <a 
                  href={`/signalements/${p.id}/photos`} 
                  className="text-blue-600 hover:underline"
                >
                  📸 Voir les photos
                </a>
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
          </div>
          <div className="mt-3">
            <div className="mb-1 flex items-center justify-between text-xs text-gray-700">
              <span>Avancement</span>
              <span className="font-semibold">{advancementPercent.toFixed(1)}%</span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
              <div
                className="h-full rounded-full bg-emerald-500 transition-all duration-300"
                style={{ width: `${advancementPercent}%` }}
              />
            </div>
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
