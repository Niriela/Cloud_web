// Créer un nouveau composant : delais-table.tsx
import React, { useState, useEffect } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../ui/table';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { ChevronUp, ChevronDown, Download, Filter } from 'lucide-react';
import { fetchDelaisTraitement, type DelaiTraitement } from '~/lib/api';

interface SortConfig {
  key: keyof DelaiTraitement;
  direction: 'asc' | 'desc';
}

export default function DelaisTable() {
  const [data, setData] = useState<DelaiTraitement[]>([]);
  const [filteredData, setFilteredData] = useState<DelaiTraitement[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortConfig, setSortConfig] = useState<SortConfig>({ key: 'dateCreation', direction: 'desc' });
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage] = useState(10);
  const [filters, setFilters] = useState({
    dateDebut: '',
    dateFin: '',
    statut: '',
    entreprise: '',
    typeSignalement: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [data, filters]);

  const loadData = async () => {
    try {
      const result = await fetchDelaisTraitement();
      setData(result);
      setFilteredData(result);
    } catch (error) {
      console.error('Erreur:', error);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let filtered = [...data];

    // Filtre par date
    if (filters.dateDebut) {
      filtered = filtered.filter(item => 
        new Date(item.dateCreation) >= new Date(filters.dateDebut)
      );
    }
    if (filters.dateFin) {
      filtered = filtered.filter(item => 
        new Date(item.dateCreation) <= new Date(filters.dateFin)
      );
    }

    // Filtre par statut
    if (filters.statut) {
      filtered = filtered.filter(item => 
        item.statutActuel === filters.statut
      );
    }

    // Filtre par entreprise
    if (filters.entreprise) {
      filtered = filtered.filter(item => 
        item.entrepriseAssociee === filters.entreprise
      );
    }

    // Filtre par type
    if (filters.typeSignalement) {
      filtered = filtered.filter(item => 
        item.typeSignalement === filters.typeSignalement
      );
    }

    // Appliquer le tri
    filtered.sort((a, b) => {
      const aValue = a[sortConfig.key];
      const bValue = b[sortConfig.key];
      
      if (aValue < bValue) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });

    setFilteredData(filtered);
    setCurrentPage(1);
  };

  const handleSort = (key: keyof DelaiTraitement) => {
    setSortConfig({
      key,
      direction: sortConfig.key === key && sortConfig.direction === 'asc' ? 'desc' : 'asc'
    });
  };

  const handleFilterChange = (key: string, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const resetFilters = () => {
    setFilters({
      dateDebut: '',
      dateFin: '',
      statut: '',
      entreprise: '',
      typeSignalement: '',
    });
  };

  const exportToCSV = () => {
    const headers = ['ID', 'Description', 'Type', 'Statut', 'Délai (jours)', 'Entreprise', 'Date création', 'Date dernière modification'];
    const csvContent = [
      headers.join(','),
      ...filteredData.map(item => [
        item.signalementId,
        `"${item.description.replace(/"/g, '""')}"`,
        item.typeSignalement,
        item.statutActuel,
        item.delaiJours,
        item.entrepriseAssociee,
        item.dateCreation,
        item.dateDerniereModification
      ].join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `delais-traitement-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
  };

  // Calcul de la pagination
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = filteredData.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(filteredData.length / itemsPerPage);
  const statusOptions = Array.from(new Set(data.map((item) => item.statutActuel))).filter(Boolean);
  const entrepriseOptions = Array.from(
    new Set(
      data
        .map((item) => item.entrepriseAssociee)
        .filter((value) => Boolean(value && value.trim())),
    ),
  );

  if (loading) {
    return <div>Chargement...</div>;
  }

  return (
    <div className="space-y-4">
      {/* Filtres */}
      <div className="bg-gray-50 p-4 rounded-lg space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">Filtres</h3>
          <div className="flex gap-2">
            <Button onClick={applyFilters} size="sm">
              <Filter className="h-4 w-4 mr-2" />
              Appliquer
            </Button>
            <Button onClick={resetFilters} variant="outline" size="sm">
              Réinitialiser
            </Button>
          </div>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="text-sm font-medium mb-1 block">Période</label>
            <div className="flex gap-2">
              <Input
                type="date"
                placeholder="Début"
                value={filters.dateDebut}
                onChange={(e) => handleFilterChange('dateDebut', e.target.value)}
                className="flex-1"
              />
              <Input
                type="date"
                placeholder="Fin"
                value={filters.dateFin}
                onChange={(e) => handleFilterChange('dateFin', e.target.value)}
                className="flex-1"
              />
            </div>
          </div>
          
          <div>
            <label className="text-sm font-medium mb-1 block">Statut</label>
            <select
              className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={filters.statut}
              onChange={(event) => handleFilterChange('statut', event.target.value)}
            >
              <option value="">Tous les statuts</option>
              {statusOptions.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
          
          <div>
            <label className="text-sm font-medium mb-1 block">Entreprise</label>
            <select
              className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={filters.entreprise}
              onChange={(event) => handleFilterChange('entreprise', event.target.value)}
            >
              <option value="">Toutes les entreprises</option>
              {entrepriseOptions.map((entreprise) => (
                <option key={entreprise} value={entreprise}>
                  {entreprise}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* En-tête avec export */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-lg font-semibold">
            Tableau des délais de traitement
          </h2>
          <p className="text-sm text-gray-600">
            {filteredData.length} signalements trouvés
          </p>
        </div>
        <Button onClick={exportToCSV} variant="outline" size="sm">
          <Download className="h-4 w-4 mr-2" />
          Exporter CSV
        </Button>
      </div>

      {/* Tableau */}
      <div className="border rounded-lg overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="cursor-pointer" onClick={() => handleSort('signalementId')}>
                <div className="flex items-center">
                  ID
                  {sortConfig.key === 'signalementId' && (
                    sortConfig.direction === 'asc' ? <ChevronUp className="h-4 w-4 ml-1" /> : <ChevronDown className="h-4 w-4 ml-1" />
                  )}
                </div>
              </TableHead>
              <TableHead className="cursor-pointer" onClick={() => handleSort('typeSignalement')}>
                <div className="flex items-center">
                  Type
                  {sortConfig.key === 'typeSignalement' && (
                    sortConfig.direction === 'asc' ? <ChevronUp className="h-4 w-4 ml-1" /> : <ChevronDown className="h-4 w-4 ml-1" />
                  )}
                </div>
              </TableHead>
              <TableHead>Description</TableHead>
              <TableHead className="cursor-pointer" onClick={() => handleSort('statutActuel')}>
                <div className="flex items-center">
                  Statut
                  {sortConfig.key === 'statutActuel' && (
                    sortConfig.direction === 'asc' ? <ChevronUp className="h-4 w-4 ml-1" /> : <ChevronDown className="h-4 w-4 ml-1" />
                  )}
                </div>
              </TableHead>
              <TableHead className="cursor-pointer" onClick={() => handleSort('delaiJours')}>
                <div className="flex items-center">
                  Délai (jours)
                  {sortConfig.key === 'delaiJours' && (
                    sortConfig.direction === 'asc' ? <ChevronUp className="h-4 w-4 ml-1" /> : <ChevronDown className="h-4 w-4 ml-1" />
                  )}
                </div>
              </TableHead>
              <TableHead>Entreprise</TableHead>
              <TableHead>Date création</TableHead>
              <TableHead>Dernière modification</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {currentItems.map((item) => (
              <TableRow key={item.signalementId}>
                <TableCell className="font-medium">#{item.signalementId}</TableCell>
                <TableCell>{item.typeSignalement}</TableCell>
                <TableCell className="max-w-xs truncate" title={item.description}>
                  {item.description}
                </TableCell>
                <TableCell>
                  <span className={`px-2 py-1 rounded-full text-xs ${
                    item.statutActuel === 'Terminé' ? 'bg-green-100 text-green-800' :
                    item.statutActuel === 'En cours' ? 'bg-blue-100 text-blue-800' :
                    item.statutActuel === 'Nouveau' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {item.statutActuel}
                  </span>
                </TableCell>
                <TableCell>
                  <span className={`font-semibold ${
                    item.delaiJours <= 3 ? 'text-green-600' :
                    item.delaiJours <= 7 ? 'text-blue-600' :
                    item.delaiJours <= 14 ? 'text-yellow-600' :
                    'text-red-600'
                  }`}>
                    {item.delaiJours} jours
                  </span>
                </TableCell>
                <TableCell>{item.entrepriseAssociee || '-'}</TableCell>
                <TableCell>{item.dateCreation}</TableCell>
                <TableCell>{item.dateDerniereModification}</TableCell>
              </TableRow>
            ))}
            {currentItems.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center py-8 text-gray-500">
                  Aucun signalement trouvé avec les filtres actuels
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-600">
            Page {currentPage} sur {totalPages} • {filteredData.length} résultats
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
              disabled={currentPage === 1}
            >
              Précédent
            </Button>
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              let pageNum;
              if (totalPages <= 5) {
                pageNum = i + 1;
              } else if (currentPage <= 3) {
                pageNum = i + 1;
              } else if (currentPage >= totalPages - 2) {
                pageNum = totalPages - 4 + i;
              } else {
                pageNum = currentPage - 2 + i;
              }
              
              return (
                <Button
                  key={pageNum}
                  variant={currentPage === pageNum ? "default" : "outline"}
                  size="sm"
                  onClick={() => setCurrentPage(pageNum)}
                >
                  {pageNum}
                </Button>
              );
            })}
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
              disabled={currentPage === totalPages}
            >
              Suivant
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
