import { MapContainer, TileLayer, LayersControl, CircleMarker, Popup } from 'react-leaflet';
import type { LatLngExpression } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import '../../carte.css';

const { BaseLayer } = LayersControl;

const samplePoints = [
  { id: 1, lat: -18.879, lng: 47.507, type: 'travaux', title: 'Travaux rue A', desc: 'Travaux de voirie' },
  { id: 2, lat: -18.883, lng: 47.512, type: 'accident', title: 'Accident', desc: 'Accident mineur' },
  { id: 3, lat: -18.875, lng: 47.499, type: 'nid-de-poule', title: 'Nid de poche', desc: 'Voirie dégradée' }
];

type PointType = 'travaux' | 'accident' | 'nid-de-poule';

const colorForType = (type: PointType): string => {
    switch (type) {
        case 'travaux': return '#f39c12';
        case 'accident': return '#e74c3c';
        case 'nid-de-poule': return '#3498db';
        default: return '#2ecc71';
    }
};

export default function Visiteurs() {
  return (
    
    <div className="visiteurs-page">
      <aside className="visiteurs-sidebar">
        <h2>Module Visiteurs</h2>
        <ul>
          <li>Voir la carte avec les différents points</li>
          <li>Installer lib et init carte</li>
          <li>Configurer couches carto</li>
          <li>Styling et responsive</li>
          <li>Création & gestion des markers</li>
        </ul>
      </aside>

      <main className="visiteurs-map">
        <MapContainer center={[-18.879, 47.507] as LatLngExpression} zoom={13} style={{ height: '100%', width: '100%' }}>
          <LayersControl position="topright">
            <BaseLayer checked name="OpenStreetMap">
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            </BaseLayer>
            <BaseLayer name="Satellite">
              <TileLayer url="https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png" />
            </BaseLayer>
          </LayersControl>

          {samplePoints.map(p => (
            <CircleMarker
              key={p.id}
              center={[p.lat, p.lng]}
              pathOptions={{ color: colorForType(p.type as PointType), fillColor: colorForType(p.type as PointType), fillOpacity: 0.8 }}
              radius={10}
            >
              <Popup>
                <strong>{p.title}</strong><br />
                {p.desc}<br />
                Type: {p.type}
              </Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      </main>
    </div>
  );
}