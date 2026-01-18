import { MapContainer, TileLayer, LayersControl, Marker, Popup } from 'react-leaflet';
import * as L from 'leaflet';
import type { LatLngExpression } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import '../../carte.css';

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

const samplePoints = [
  { id: 1, lat: -18.879, lng: 47.507, type: 'travaux', title: 'Travaux rue A', desc: 'Travaux de voirie' },
  { id: 2, lat: -18.883, lng: 47.512, type: 'accident', title: 'Accident', desc: 'Accident mineur' },
  { id: 3, lat: -18.875, lng: 47.499, type: 'nid-de-poule', title: 'Nid de poche', desc: 'Voirie dégradée' },
  { id: 4, lat: -18.877, lng: 47.505, type: 'repare', title: 'Réparé', desc: 'Signalement réparé' },
  { id: 5, lat: -18.881, lng: 47.510, type: 'abime', title: 'Abîmé', desc: 'Dommages importants' },
  { id: 6, lat: -18.882, lng: 47.508, type: 'alerte', title: 'Alerte', desc: 'Alerte sécurité' },
  { id: 7, lat: -18.880, lng: 47.503, type: 'zone-rouge', title: 'Zone rouge', desc: 'Accès interdit' },
  { id: 8, lat: -18.876, lng: 47.506, type: 'eau', title: 'Fuite / eau', desc: 'Possible inondation / fuite' }
];

export default function Visiteurs() {
  return (
    <div className="relative flex h-full w-full min-h-[600px] rounded-xl overflow-hidden">
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

        {samplePoints.map((p) => (
          <Marker
            key={p.id}
            position={[p.lat, p.lng]}
            icon={getIconForType(p.type as PointType)}
          >
            <Popup>
              <strong>{p.title}</strong>
              <br />
              {p.desc}
              <br />
              Type: {p.type}
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}