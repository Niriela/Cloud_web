import { useParams, useLoaderData, Link } from "react-router-dom";
import { 
  getSignalements, 
  getSignalementPhotos, 
  type SignalementMapDto, 
  type PhotoSignalementDto 
} from "~/lib/api";
import { AppSidebar } from "~/components/app-sidebar";
import {
  SidebarProvider,
  SidebarInset,
  SidebarTrigger,
} from "~/components/ui/sidebar";
import { Separator } from "~/components/ui/separator";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "~/components/ui/breadcrumb";
import { Card, CardContent, CardHeader, CardTitle } from "~/components/ui/card";
import { Button } from "~/components/ui/button";
import { ArrowLeft, Camera, MapPin, Calendar, DollarSign, Ruler } from "lucide-react";
import { EmptyState } from "~/components/ui/empty-state";

// Solution temporaire si vous ne voulez pas créer le composant Badge
// Créez un Badge simple en attendant
const SimpleBadge = ({ children, className = "" }: { children: React.ReactNode, className?: string }) => (
  <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${className}`}>
    {children}
  </span>
);

// Loader
export async function loader({ params }: { params: { id: string } }) {
  try {
    const allSignalements = await getSignalements();
    const signalement = allSignalements.find(s => s.id === parseInt(params.id));
    
    if (!signalement) {
      throw new Response("Signalement non trouvé", { status: 404 });
    }
    
    // Récupérer les vraies photos via API
    let photos: PhotoSignalementDto[] = [];
    try {
      photos = await getSignalementPhotos(signalement.id);
    } catch (error) {
      console.error("Erreur lors du chargement des photos:", error);
      // Les photos restent un tableau vide en cas d'erreur
    }
    
    return { 
      signalement,
      photos
    };
  } catch (error) {
    throw new Response("Erreur de chargement", { status: 500 });
  }
}

export default function SignalementDetailPage() {
  const { id } = useParams();
  const { signalement, photos } = useLoaderData() as { 
    signalement: SignalementMapDto; 
    photos: PhotoSignalementDto[] 
  };

  const getStatusColor = (status: string | null) => {
    if (!status) return 'bg-gray-100 text-gray-800 border-gray-200';
    
    switch (status.toLowerCase()) {
      case 'terminé':
      case 'réparé':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'en cours':
      case 'en traitement':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'en attente':
      case 'à traiter':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'annulé':
      case 'rejeté':
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getTypeIcon = (type: string | null) => {
    if (!type) return '📍';
    
    const typeLower = type.toLowerCase();
    if (typeLower.includes('travaux') || typeLower.includes('construction')) return '🚧';
    if (typeLower.includes('accident')) return '❗';
    if (typeLower.includes('nid')) return '🕳️';
    if (typeLower.includes('réparé') || typeLower.includes('repare')) return '✅';
    if (typeLower.includes('eau') || typeLower.includes('fuite')) return '💧';
    if (typeLower.includes('zone')) return '🔴';
    return '📍';
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return 'Non spécifiée';
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  };

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        {/* Header avec breadcrumb */}
        <header className="flex h-16 shrink-0 items-center justify-between gap-2 px-4 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
          <div className="flex items-center gap-2">
            <SidebarTrigger className="-ml-1" />
            <Separator
              orientation="vertical"
              className="mr-2 data-[orientation=vertical]:h-4"
            />
            <Breadcrumb>
              <BreadcrumbList>
                <BreadcrumbItem>
                  <BreadcrumbLink asChild>
                    <Link to="/dashboard">Dashboard</Link>
                  </BreadcrumbLink>
                </BreadcrumbItem>
                <BreadcrumbSeparator />
                <BreadcrumbItem>
                  <BreadcrumbLink asChild>
                    <Link to="/Visiteurs">Visiteurs</Link>
                  </BreadcrumbLink>
                </BreadcrumbItem>
                <BreadcrumbSeparator />
                <BreadcrumbItem>
                  <BreadcrumbPage>
                    Signalement #{signalement.id}
                  </BreadcrumbPage>
                </BreadcrumbItem>
              </BreadcrumbList>
            </Breadcrumb>
          </div>
          
          <Button asChild variant="outline" size="sm" className="gap-2">
            <Link to="/Visiteurs">
              <ArrowLeft className="h-4 w-4" />
              Retour à la carte
            </Link>
          </Button>
        </header>

        {/* Contenu principal */}
        <div className="flex-1 p-4 md:p-6">
          <div className="max-w-7xl mx-auto">
            {/* En-tête du signalement */}
            <div className="mb-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4">
                <div>
                  <h1 className="text-2xl md:text-3xl font-bold text-gray-900">
                    <span className="mr-2">{getTypeIcon(signalement.typeSignalement)}</span>
                    {signalement.typeSignalement || "Signalement"} #{signalement.id}
                  </h1>
                  <p className="text-gray-600 mt-1">
                    Créé le {formatDate(signalement.date)}
                  </p>
                </div>
                <SimpleBadge className={`px-4 py-2 text-sm font-medium border ${getStatusColor(signalement.statut)}`}>
                  {signalement.statut || "Statut inconnu"}
                </SimpleBadge>
              </div>
              
              {signalement.description && (
                <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                  <p className="text-gray-700">{signalement.description}</p>
                </div>
              )}
            </div>

            {/* Grille principale */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Colonne de gauche - Galerie photos (2/3) */}
              <div className="lg:col-span-2">
                <Card>
                  <CardHeader className="border-b">
                    <div className="flex items-center justify-between">
                      <CardTitle className="flex items-center gap-2">
                        <Camera className="h-5 w-5" />
                        Galerie photos
                      </CardTitle>
                      {photos.length > 0 && (
                        <SimpleBadge className="bg-blue-100 text-blue-800 border-blue-200">
                          {photos.length} photo{photos.length > 1 ? 's' : ''}
                        </SimpleBadge>
                      )}
                    </div>
                  </CardHeader>
                  <CardContent className="p-6">
                    {photos.length > 0 ? (
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                        {photos.map((photo) => (
                          <div 
                            key={photo.id} 
                            className="group relative overflow-hidden rounded-lg border border-gray-200 bg-white hover:shadow-lg transition-shadow"
                          >
                            <div className="aspect-[4/3] overflow-hidden">
                              <img
                                src={photo.url}
                                alt={`Photo du signalement #${signalement.id}`}
                                className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                                loading="lazy"
                                onError={(e) => {
                                  const target = e.target as HTMLImageElement;
                                  target.src = "https://via.placeholder.com/400x300?text=Image+Indisponible";
                                }}
                              />
                            </div>
                            <div className="p-3">
                              <p className="text-sm font-medium text-gray-900">
                                Photo #{photo.id}
                              </p>
                              <p className="text-xs text-gray-500 mt-1">
                                {photo.updatedAt 
                                  ? new Date(photo.updatedAt).toLocaleDateString('fr-FR')
                                  : 'Date inconnue'}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <EmptyState
                        title="Aucune photo disponible"
                        description="Ce signalement n'a pas encore de photos associées. Les photos seront ajoutées par l'équipe de gestion."
                        icon={
                          <div className="mx-auto w-16 h-16 bg-white rounded-full flex items-center justify-center mb-4 border-2 border-gray-300">
                            <Camera className="h-8 w-8 text-gray-400" />
                          </div>
                        }
                        action={
                          <div className="flex flex-col sm:flex-row gap-3 justify-center">
                            <Button asChild variant="ghost" className="gap-2">
                              <Link to="/Visiteurs">
                                <ArrowLeft className="h-4 w-4" />
                                Voir d'autres signalements
                              </Link>
                            </Button>
                          </div>
                        }
                      />
                    )}
                  </CardContent>
                </Card>
              </div>

              {/* Colonne de droite - Informations (1/3) */}
              <div className="space-y-6">
                {/* Carte des détails */}
                <Card>
                  <CardHeader className="border-b">
                    <CardTitle className="flex items-center gap-2">
                      <MapPin className="h-5 w-5" />
                      Localisation
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-6">
                    <div className="space-y-4">
                      <div className="flex items-center gap-3 p-3 bg-blue-50 rounded-lg">
                        <MapPin className="h-5 w-5 text-blue-600" />
                        <div>
                          <p className="text-sm font-medium text-gray-700">Coordonnées GPS</p>
                          <p className="text-sm text-gray-600">
                            {signalement.latitude?.toFixed(6) || 'N/A'}, {signalement.longitude?.toFixed(6) || 'N/A'}
                          </p>
                        </div>
                      </div>
                      
                      <a
                        href={`https://www.openstreetmap.org/?mlat=${signalement.latitude}&mlon=${signalement.longitude}&zoom=18`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="block w-full"
                      >
                        <Button variant="outline" className="w-full gap-2">
                          <MapPin className="h-4 w-4" />
                          Voir sur OpenStreetMap
                        </Button>
                      </a>
                    </div>
                  </CardContent>
                </Card>

                {/* Carte des métriques */}
                <Card>
                  <CardHeader className="border-b">
                    <CardTitle>📊 Métriques</CardTitle>
                  </CardHeader>
                  <CardContent className="p-6 space-y-4">
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <Ruler className="h-5 w-5 text-gray-600" />
                        <div>
                          <p className="text-sm font-medium text-gray-700">Surface</p>
                        </div>
                      </div>
                      <span className="font-semibold">
                        {signalement.surface ? `${signalement.surface} m²` : 'Non spécifié'}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <DollarSign className="h-5 w-5 text-gray-600" />
                        <div>
                          <p className="text-sm font-medium text-gray-700">Budget</p>
                        </div>
                      </div>
                      <span className="font-semibold">
                        {signalement.budget ? `${signalement.budget} €` : 'Non spécifié'}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <Calendar className="h-5 w-5 text-gray-600" />
                        <div>
                          <p className="text-sm font-medium text-gray-700">Date</p>
                        </div>
                      </div>
                      <span className="font-semibold">
                        {formatDate(signalement.date)}
                      </span>
                    </div>
                  </CardContent>
                </Card>

                {/* Carte entreprise */}
                {signalement.entreprise && (
                  <Card>
                    <CardHeader className="border-b">
                      <CardTitle>🏢 Entreprise</CardTitle>
                    </CardHeader>
                    <CardContent className="p-6">
                      <div className="flex items-center gap-3 p-3 bg-green-50 rounded-lg">
                        <div className="flex-1">
                          <p className="font-medium text-gray-900">{signalement.entreprise}</p>
                          <p className="text-sm text-gray-600">
                            Responsable du traitement
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )}
              </div>
            </div>
          </div>
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}
